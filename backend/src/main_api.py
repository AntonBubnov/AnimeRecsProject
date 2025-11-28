from fastapi import FastAPI, Depends, HTTPException, Query, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import datetime, timedelta
import json
import os
from typing import List, Dict, Optional, Any
from pydantic import BaseModel
from rapidfuzz import process, fuzz  # Бібліотека для нечіткого пошуку

# Імпорти з наших модулів
from .database import get_db, engine
from . import models, auth, config

# Створюємо таблиці при старті (для Render)
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Anime Recommender API")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

# --- ГЛОБАЛЬНІ ЗМІННІ ---
# Тепер METADATA зберігає повний об'єкт з metadata.json
METADATA: Dict[int, Dict[str, Any]] = {} 
EVALUATION_QUEUE: List[Dict] = [] 
GRAPH_ADJ: Dict = {} 
ALL_GENRES: List[str] = []
ALL_MEDIA_TYPES: List[str] = []

# Для пошуку: словник {id: "Title English Title Synonyms"}
SEARCH_INDEX: Dict[int, str] = {}

# --- МОДЕЛІ Pydantic ---
class UserCreate(BaseModel):
    username: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class RateRequest(BaseModel):
    anime_id: int
    rating_type: str 

# Оновлена модель відповіді (Light Model)
class AnimeResponse(BaseModel):
    id: int
    title: str
    title_en: str          # Нове поле: Англійська назва
    picture_medium: str    # Нове поле: Картинка для карток
    picture_large: str     # Нове поле: Картинка для оцінювання
    popularity: int
    score: float = 0.0     # ProfileScore
    cluster_id: Optional[int] = None

# Модель запиту фільтрації
class FilterRequest(BaseModel):
    # Сортування
    sort_by: str = "popularity" # rank, popularity, start_season, title
    sort_order: str = "desc"    # asc, desc
    
    # Фільтри
    genres_include: List[str] = []
    genres_exclude: List[str] = []
    genres_strict: bool = False # True = AND, False = OR
    
    media_types_include: List[str] = []
    media_types_exclude: List[str] = []
    
    year_from: Optional[int] = None
    year_to: Optional[int] = None
    
    score_from: Optional[float] = None
    score_to: Optional[float] = None
    
    page: int = 1
    limit: int = 50

# --- ЗАВАНТАЖЕННЯ ДАНИХ ---
@app.on_event("startup")
def load_data():
    global METADATA, EVALUATION_QUEUE, GRAPH_ADJ, SEARCH_INDEX, ALL_GENRES, ALL_MEDIA_TYPES
    
    genres_set = set()
    media_types_set = set()
    
    print("Завантаження даних у пам'ять...")
    
    # 1. Metadata (Повне завантаження)
    if os.path.exists(config.METADATA_FILE):
        with open(config.METADATA_FILE, 'r', encoding='utf-8') as f:
            raw_meta = json.load(f)
            for aid_str, data in raw_meta.items():
                aid = int(aid_str)
                METADATA[aid] = data # Зберігаємо все як є
                
                # Будуємо індекс для пошуку (об'єднуємо всі назви в один рядок)
                titles = [data.get("title", "")]
                alts = data.get("alternative_titles", {})
                if alts.get("en"): titles.append(alts["en"])
                if alts.get("ja"): titles.append(alts["ja"])
                titles.extend(alts.get("synonyms", []))
                SEARCH_INDEX[aid] = " | ".join(filter(None, titles)).lower()

                # Збір статистики для фільтрів
                for g in data.get("genres", []):
                    genres_set.add(g["name"])
                if data.get("media_type"):
                    media_types_set.add(data.get("media_type"))

    ALL_GENRES = sorted(list(genres_set))
    ALL_MEDIA_TYPES = sorted(list(media_types_set))

    # 2. Queue
    if os.path.exists(config.QUEUE_FILE):
        with open(config.QUEUE_FILE, 'r', encoding='utf-8') as f:
            EVALUATION_QUEUE = json.load(f)
            
    # 3. Graph
    if os.path.exists(config.EDGES_FILE):
        with open(config.EDGES_FILE, 'r', encoding='utf-8') as f:
            edges = json.load(f)
            for e in edges:
                src, tgt, w = e['source'], e['target'], e['weight']
                if src not in GRAPH_ADJ: GRAPH_ADJ[src] = {}
                if tgt not in GRAPH_ADJ: GRAPH_ADJ[tgt] = {}
                GRAPH_ADJ[src][tgt] = w
                GRAPH_ADJ[tgt][src] = w
                
    print(f"Дані завантажено! Аніме: {len(METADATA)}")

# --- ДОПОМІЖНІ ФУНКЦІЇ ---
def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = auth.jwt.decode(token, auth.SECRET_KEY, algorithms=[auth.ALGORITHM])
        username: str = payload.get("sub")
        if username is None: raise credentials_exception
    except auth.jwt.JWTError:
        raise credentials_exception 
    user = db.query(models.User).filter(models.User.username == username).first()
    if user is None: raise credentials_exception
    return user

def create_anime_response(aid: int, score: float = 0.0, cluster_id: int = None) -> AnimeResponse:
    """Створює об'єкт відповіді з правильними полями"""
    data = METADATA.get(aid, {})
    
    # Витягуємо назви та картинки з безпечним fallback
    title_en = data.get("alternative_titles", {}).get("en") or data.get("title", "Unknown")
    pics = data.get("main_picture", {})
    pic_med = pics.get("medium", "")
    pic_large = pics.get("large") or pic_med # Якщо large немає, беремо medium
    
    # Популярність (кількість users)
    pop = data.get("num_list_users", 0) or data.get("members", 0)

    return AnimeResponse(
        id=aid,
        title=data.get("title", "Unknown"),
        title_en=title_en,
        picture_medium=pic_med,
        picture_large=pic_large,
        popularity=pop,
        score=score,
        cluster_id=cluster_id
    )

# --- ENDPOINTS ---

@app.post("/register", response_model=Token)
def register(user: UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.username == user.username).first()
    if db_user: raise HTTPException(status_code=400, detail="Username already registered")
    hashed_pw = auth.get_password_hash(user.password)
    new_user = models.User(username=user.username, password_hash=hashed_pw)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    access_token = auth.create_access_token(data={"sub": new_user.username})
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/token", response_model=Token)
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.username == form_data.username).first()
    if not user or not auth.verify_password(form_data.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Incorrect username or password")
    access_token = auth.create_access_token(data={"sub": user.username})
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/rate")
def rate_anime(req: RateRequest, current_user: models.User = Depends(get_current_user), db: Session = Depends(get_db)):
    existing = db.query(models.Rating).filter(models.Rating.user_id == current_user.id, models.Rating.anime_id == req.anime_id).first()
    if existing:
        existing.rating_type = req.rating_type
        existing.timestamp = datetime.utcnow()
    else:
        new_rating = models.Rating(user_id=current_user.id, anime_id=req.anime_id, rating_type=req.rating_type)
        db.add(new_rating)
    db.commit()
    return {"status": "ok"}

@app.get("/evaluate", response_model=AnimeResponse)
def get_evaluation_item(current_user: models.User = Depends(get_current_user), db: Session = Depends(get_db)):
    user_ratings = db.query(models.Rating).filter(models.Rating.user_id == current_user.id).all()
    exclude_ids = set()
    now = datetime.utcnow()
    for r in user_ratings:
        if r.rating_type in ['LIKE', 'DISLIKE']: exclude_ids.add(r.anime_id)
        elif r.rating_type == 'PLAN' and (now - r.timestamp < timedelta(days=7)): exclude_ids.add(r.anime_id)
        elif r.rating_type == 'SKIP' and (now - r.timestamp < timedelta(days=30)): exclude_ids.add(r.anime_id)
    
    for item in EVALUATION_QUEUE:
        if item['id'] not in exclude_ids and item['id'] in METADATA:
            return create_anime_response(item['id'], cluster_id=item.get('cluster_id'))
            
    raise HTTPException(status_code=404, detail="No more anime to evaluate!")

@app.get("/feed", response_model=List[AnimeResponse])
def get_recommendation_feed(
    sort_by: str = "popularity", # popularity, rank, year
    page: int = 1,
    limit: int = 50,
    current_user: models.User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """
    Генерує персональну стрічку.
    Сортування: Primary = Score (DESC), Secondary = sort_by.
    """
    # 1. Отримуємо оцінки
    user_ratings = db.query(models.Rating).filter(models.Rating.user_id == current_user.id).all()
    
    rated_ids = {
        r.anime_id for r in user_ratings 
        if r.rating_type in ['LIKE', 'DISLIKE', 'PLAN']
    }
    
    # 2. Розрахунок ProfileScore
    scores = {} 
    for r in user_ratings:
        if r.anime_id not in GRAPH_ADJ: continue
        
        impact = 0.0
        if r.rating_type == 'LIKE': impact = 1.0
        elif r.rating_type == 'DISLIKE': impact = -1.0
        elif r.rating_type == 'PLAN': impact = 0.2
        
        if impact == 0: continue
            
        for neighbor_id, weight in GRAPH_ADJ[r.anime_id].items():
            scores[neighbor_id] = scores.get(neighbor_id, 0.0) + (impact * weight)

    # 3. Формуємо кандидатів
    candidates = []
    
    for aid, meta in METADATA.items():
        if aid in rated_ids: continue
            
        score = scores.get(aid, 0.0)
        
        # Визначаємо вторинний ключ сортування
        secondary_val = 0
        if sort_by == "rank":
            # Інвертуємо ранг, бо при сортуванні DESC: -1 > -100 (Тобто ранг 1 буде вище)
            rank = meta.get("rank")
            secondary_val = -rank if rank else -999999
        elif sort_by == "year":
            secondary_val = (meta.get("start_season") or {}).get("year", 0)
        else: # popularity
            secondary_val = meta.get("num_list_users") or 0

        candidates.append({
            "data": meta,
            "score": score,
            "sort_key": (score, secondary_val) # Кортеж для сортування
        })
            
    # 4. Сортування (DESC)
    # Python порівнює кортежі елемент за елементом: спочатку score, потім secondary_val
    candidates.sort(key=lambda x: x['sort_key'], reverse=True)
    
    # 5. Пагінація
    start = (page - 1) * limit
    end = start + limit
    page_items = candidates[start:end]
    
    # Формування відповіді
    result = []
    for item in page_items:
        meta = item['data']
        result.append(create_anime_response(meta["id"], score=item['score']))

    return result

@app.get("/search", response_model=List[AnimeResponse])
def search(query: str, limit: int = 10):
    """Пошук аніме за назвою (Fuzzy Search)"""
    if not query: return []
    
    # Використовуємо rapidfuzz для пошуку по нашому індексу
    # extract повертає список кортежів: (match_string, score, key)
    results = process.extract(query.lower(), SEARCH_INDEX, limit=limit, scorer=fuzz.token_set_ratio)
    
    response = []
    for _, score, aid in results:
        if score > 50: # Поріг схожості
            response.append(create_anime_response(aid, score=0.0)) # Score тут не важливий
            
    return response

@app.get("/anime/{anime_id}", response_model=Dict[str, Any])
def get_anime_details(
    anime_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Отримання повної інформації про аніме"""
    if anime_id not in METADATA:
        raise HTTPException(status_code=404, detail="Anime not found")
    
    # Беремо повні метадані
    anime_data = METADATA[anime_id].copy()
    anime_data["id"] = anime_id
    
    # Додаємо поточну оцінку користувача (якщо є)
    rating = db.query(models.Rating).filter(
        models.Rating.user_id == current_user.id,
        models.Rating.anime_id == anime_id
    ).first()
    
    anime_data["user_status"] = rating.rating_type if rating else None
    
    return anime_data

@app.get("/library", response_model=List[AnimeResponse])
def get_library(
    status: str = Query(None, description="Filter by status: LIKE, PLAN, DISLIKE, SKIP"),
    page: int = 1,
    limit: int = 50,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Повертає історію оцінок користувача (Закладки)"""
    # 1. Будуємо запит до БД
    query = db.query(models.Rating).filter(models.Rating.user_id == current_user.id)
    
    # Фільтрація за статусом (якщо передано)
    if status:
        query = query.filter(models.Rating.rating_type == status.upper())
        
    # Сортування: найновіші зверху
    query = query.order_by(models.Rating.timestamp.desc())
    
    # Пагінація на рівні SQL
    total = query.count()
    ratings = query.offset((page - 1) * limit).limit(limit).all()
    
    # 2. Формуємо відповідь, підтягуючи дані з METADATA
    response = []
    for r in ratings:
        if r.anime_id in METADATA:
            # Використовуємо нашу допоміжну функцію
            # Score тут не такий важливий, але можна передати 0 або реальний
            resp_item = create_anime_response(r.anime_id)
            response.append(resp_item)
            
    return response

@app.get("/meta/constants")
def get_constants():
    """Повертає доступні жанри та типи медіа для побудови UI фільтрів"""
    return {
        "genres": ALL_GENRES,
        "media_types": ALL_MEDIA_TYPES
    }

@app.post("/search/advanced", response_model=List[AnimeResponse])
def search_advanced(req: FilterRequest):
    """Складний пошук та фільтрація"""
    candidates = []
    
    for aid, meta in METADATA.items():
        # 1. Фільтр по Року
        if req.year_from is not None or req.year_to is not None:
            season = meta.get("start_season", {})
            year = season.get("year")
            if year is None: continue # Пропускаємо, якщо даних немає
            if req.year_from is not None and year < req.year_from: continue
            if req.year_to is not None and year > req.year_to: continue

        # 2. Фільтр по Оцінці (Mean)
        if req.score_from is not None or req.score_to is not None:
            score = meta.get("mean")
            if score is None: continue
            if req.score_from is not None and score < req.score_from: continue
            if req.score_to is not None and score > req.score_to: continue

        # 3. Фільтр по Media Type
        m_type = meta.get("media_type")
        if req.media_types_exclude and m_type in req.media_types_exclude: continue
        if req.media_types_include and m_type not in req.media_types_include: continue

        # 4. Фільтр по Жанрах
        anime_genres = {g["name"] for g in meta.get("genres", [])}
        
        # Виключення (-)
        if req.genres_exclude:
            # Якщо є хоч один заборонений жанр -> пропускаємо
            if not anime_genres.isdisjoint(req.genres_exclude): continue
            
        # Включення (+)
        if req.genres_include:
            if req.genres_strict:
                # Строго: аніме повинно мати ВСІ обрані жанри (issubset)
                if not set(req.genres_include).issubset(anime_genres): continue
            else:
                # Не строго: аніме повинно мати ХОЧА Б ОДИН (intersection)
                if not not anime_genres.intersection(req.genres_include): pass # OK
                else: continue # Немає перетинів

        candidates.append(meta)

    # 5. Сортування
    reverse = (req.sort_order == "desc")

    # Інвертуємо логіку для Rank та Title.
    # Користувач очікує, що "Descending" (або "Best/Default") для Рангу - це 1, 2, 3...
    # А для Назви - це A, B, C...
    if req.sort_by in ["rank", "title"]:
        reverse = not reverse
    
    def get_sort_key(item):
        if req.sort_by == "rank":
            return item.get("rank") or 999999 # Rank 1 краще, тому asc default. Але якщо desc, то logic reverses
        elif req.sort_by == "popularity":
            return item.get("num_list_users", 0)
        elif req.sort_by == "start_season":
            return item.get("start_season", {}).get("year", 0)
        elif req.sort_by == "title":
            return item.get("alternative_titles", {}).get("en", "") or item.get("title", "")
        return 0

    # Rank зазвичай сортують ASC (1, 2, 3), Popularity DESC.
    # Тут ми просто слідуємо req.sort_order.
    # Нюанс для Rank: якщо сортуємо ASC, то None значення мають бути в кінці.
    
    candidates.sort(key=get_sort_key, reverse=reverse)

    # 6. Пагінація
    start = (req.page - 1) * req.limit
    end = start + req.limit
    page_items = candidates[start:end]

    return [create_anime_response(item["id"]) for item in page_items]