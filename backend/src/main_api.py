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

# --- ЗАВАНТАЖЕННЯ ДАНИХ ---
@app.on_event("startup")
def load_data():
    global METADATA, EVALUATION_QUEUE, GRAPH_ADJ, SEARCH_INDEX
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
    page: int = 1,          # Номер сторінки
    limit: int = 50,        # Кількість на сторінці
    current_user: models.User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    user_ratings = db.query(models.Rating).filter(models.Rating.user_id == current_user.id).all()
    rated_ids = {r.anime_id for r in user_ratings if r.rating_type in ['LIKE', 'DISLIKE', 'PLAN']}
    
    # Розрахунок Score
    scores = {}
    for r in user_ratings:
        if r.anime_id not in GRAPH_ADJ: continue
        impact = 1.0 if r.rating_type == 'LIKE' else (-1.0 if r.rating_type == 'DISLIKE' else (0.2 if r.rating_type == 'PLAN' else 0))
        if impact == 0: continue
        for neighbor_id, weight in GRAPH_ADJ[r.anime_id].items():
            scores[neighbor_id] = scores.get(neighbor_id, 0.0) + (impact * weight)

    # Формуємо повний список кандидатів
    # Оптимізація: для пагінації нам все одно треба відсортувати все, 
    # але ми створюємо повні об'єкти AnimeResponse тільки для потрібної сторінки
    
    candidates = []
    for aid in METADATA:
        if aid in rated_ids: continue
        # Зберігаємо кортеж (score, popularity, id) для сортування
        sc = scores.get(aid, 0.0)
        pop = METADATA[aid].get("num_list_users", 0)
        candidates.append((sc, pop, aid))
    
    # Сортування
    candidates.sort(key=lambda x: (x[0], x[1]), reverse=True)
    
    # Пагінація (Slicing)
    start = (page - 1) * limit
    end = start + limit
    page_items = candidates[start:end]
    
    # Формування фінальної відповіді
    result = []
    for sc, pop, aid in page_items:
        result.append(create_anime_response(aid, score=sc))
        
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