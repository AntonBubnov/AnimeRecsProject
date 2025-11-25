from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import datetime, timedelta
import json
import os
from typing import List, Dict, Optional
from pydantic import BaseModel

# Імпорти з наших модулів
from .database import get_db, engine
from . import models, auth, config

# Ініціалізація додатка
app = FastAPI(title="Anime Recommender API")

# Налаштування OAuth2 (вказуємо ендпоінт для отримання токена)
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

# --- ГЛОБАЛЬНІ ЗМІННІ (IN-MEMORY STORAGE) ---
METADATA: Dict = {}        # {anime_id: {title, popularity...}}
EVALUATION_QUEUE: List = [] # [{id, cluster_id, ...}, ...]
GRAPH_ADJ: Dict = {}       # {source_id: {target_id: weight, ...}}

# --- МОДЕЛІ Pydantic (для валідації JSON) ---
class UserCreate(BaseModel):
    username: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class AnimeResponse(BaseModel):
    id: int
    title: str
    popularity: int
    picture: str = ""  # <-- ДОДАНО ЦЕ ПОЛЕ
    cluster_id: Optional[int] = None
    score: Optional[float] = 0.0

class RateRequest(BaseModel):
    anime_id: int
    rating_type: str  # 'LIKE', 'DISLIKE', 'SKIP', 'PLAN'

# --- ЗАВАНТАЖЕННЯ ДАНИХ ---
@app.on_event("startup")
def load_data():
    global METADATA, EVALUATION_QUEUE, GRAPH_ADJ
    
    # Автоматично створюємо таблиці, якщо їх немає (для PostgreSQL на Render)
    models.Base.metadata.create_all(bind=engine)
    
    print("Завантаження даних у пам'ять...")
    
    # 1. Metadata
    if os.path.exists(config.METADATA_FILE):
        with open(config.METADATA_FILE, 'r', encoding='utf-8') as f:
            raw_meta = json.load(f)
            # Спрощуємо структуру для швидкого доступу
            for aid, data in raw_meta.items():
                # Витягуємо URL картинки (medium size)
                pic_url = data.get("main_picture", {}).get("medium", "")

                METADATA[int(aid)] = {
                    "title": data.get("title", "Unknown"),
                    "popularity": data.get("members", 0) or data.get("num_list_users", 0),
                    "picture": pic_url  # <-- ЗБЕРІГАЄМО КАРТИНКУ
                }
    
    # 2. Queue
    if os.path.exists(config.QUEUE_FILE):
        with open(config.QUEUE_FILE, 'r', encoding='utf-8') as f:
            EVALUATION_QUEUE = json.load(f)
            
    # 3. Graph (Edges) -> Adjacency List
    if os.path.exists(config.EDGES_FILE):
        with open(config.EDGES_FILE, 'r', encoding='utf-8') as f:
            edges = json.load(f)
            for e in edges:
                src, tgt, w = e['source'], e['target'], e['weight']
                if src not in GRAPH_ADJ: GRAPH_ADJ[src] = {}
                if tgt not in GRAPH_ADJ: GRAPH_ADJ[tgt] = {}
                # Граф неорієнтований, додаємо в обидва боки
                GRAPH_ADJ[src][tgt] = w
                GRAPH_ADJ[tgt][src] = w
                
    print(f"Дані завантажено! Аніме: {len(METADATA)}, Ребер: {len(edges) if 'edges' in locals() else 0}")

# --- ДОПОМІЖНІ ФУНКЦІЇ ---
def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    """Перевірка токена і отримання поточного користувача"""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = auth.jwt.decode(token, auth.SECRET_KEY, algorithms=[auth.ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
    except auth.jwt.JWTError:
        raise credentials_exception
        
    user = db.query(models.User).filter(models.User.username == username).first()
    if user is None:
        raise credentials_exception
    return user

# --- ENDPOINTS: AUTH ---

@app.post("/register", response_model=Token)
def register(user: UserCreate, db: Session = Depends(get_db)):
    # Перевірка чи існує юзер
    db_user = db.query(models.User).filter(models.User.username == user.username).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")
    
    # Створення юзера
    hashed_pw = auth.get_password_hash(user.password)
    new_user = models.User(username=user.username, password_hash=hashed_pw)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    # Видача токена одразу
    access_token = auth.create_access_token(data={"sub": new_user.username})
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/token", response_model=Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.username == form_data.username).first()
    if not user or not auth.verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = auth.create_access_token(data={"sub": user.username})
    return {"access_token": access_token, "token_type": "bearer"}

# --- ENDPOINTS: CORE LOGIC ---

@app.post("/rate")
def rate_anime(
    rate_req: RateRequest, 
    current_user: models.User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """Зберігає оцінку користувача"""
    # Перевіряємо, чи оцінював він це раніше
    existing_rating = db.query(models.Rating).filter(
        models.Rating.user_id == current_user.id,
        models.Rating.anime_id == rate_req.anime_id
    ).first()
    
    if existing_rating:
        # Оновлюємо існуючу
        existing_rating.rating_type = rate_req.rating_type
        existing_rating.timestamp = datetime.utcnow()
    else:
        # Створюємо нову
        new_rating = models.Rating(
            user_id=current_user.id,
            anime_id=rate_req.anime_id,
            rating_type=rate_req.rating_type
        )
        db.add(new_rating)
    
    db.commit()
    return {"status": "ok", "message": "Rating saved"}

@app.get("/evaluate", response_model=AnimeResponse)
def get_evaluation_item(
    current_user: models.User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """
    Повертає одне аніме з черги для оцінки.
    Реалізує 'Smart Logic' для повернення SKIP/PLAN.
    """
    # 1. Отримуємо всі оцінки користувача
    user_ratings = db.query(models.Rating).filter(models.Rating.user_id == current_user.id).all()
    
    # 2. Формуємо список ID, які треба виключити ПРЯМО ЗАРАЗ
    exclude_ids = set()
    now = datetime.utcnow()
    
    for r in user_ratings:
        if r.rating_type in ['LIKE', 'DISLIKE']:
            exclude_ids.add(r.anime_id)
        
        elif r.rating_type == 'PLAN':
            # Ховаємо, якщо пройшло менше 7 днів
            if now - r.timestamp < timedelta(days=7):
                exclude_ids.add(r.anime_id)
                
        elif r.rating_type == 'SKIP':
            # Ховаємо, якщо пройшло менше 30 днів
            if now - r.timestamp < timedelta(days=30):
                exclude_ids.add(r.anime_id)
    
    # 3. Шукаємо перше аніме в черзі, якого немає в exclude_ids
    for item in EVALUATION_QUEUE:
        if item['id'] not in exclude_ids:
            # Знайшли! Повертаємо дані з метаданих (бо в черзі може бути не все)
            meta = METADATA.get(item['id'], {})
            return {
                "id": item['id'],
                "title": meta.get("title", item['title']),
                "popularity": item['popularity'],
                "picture": meta.get("picture", ""), # <-- ДОДАНО
                "cluster_id": item.get("cluster_id")
            }
            
    # Якщо черга скінчилась (ого!)
    raise HTTPException(status_code=404, detail="No more anime to evaluate!")

@app.get("/feed", response_model=List[AnimeResponse])
def get_recommendation_feed(
    limit: int = 20,
    current_user: models.User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """
    Генерує персональну стрічку рекомендацій.
    Стратегія: Global Ranking (всі неоцінені аніме сортуються за Score + Popularity).
    """
    # 1. Отримуємо оцінки користувача
    user_ratings = db.query(models.Rating).filter(models.Rating.user_id == current_user.id).all()
    
    # Створюємо "чорний список" ID, які ми точно не хочемо показувати.
    # (Виключаємо LIKE, DISLIKE, PLAN. Аніме зі статусом SKIP залишаються кандидатами)
    rated_ids = {
        r.anime_id for r in user_ratings 
        if r.rating_type in ['LIKE', 'DISLIKE', 'PLAN']
    }
    
    # 2. Розрахунок ProfileScore (поширення впливу по графу)
    scores = {} 
    
    for r in user_ratings:
        if r.anime_id not in GRAPH_ADJ: continue
        
        impact = 0.0
        if r.rating_type == 'LIKE': impact = 1.0
        elif r.rating_type == 'DISLIKE': impact = -1.0
        elif r.rating_type == 'PLAN': impact = 0.2
        
        if impact == 0: continue
            
        neighbors = GRAPH_ADJ[r.anime_id]
        for neighbor_id, weight in neighbors.items():
            # Ми не перевіряємо rated_ids тут, щоб коректно порахувати всі впливи,
            # фільтрацію зробимо на етапі формування списку кандидатів.
            scores[neighbor_id] = scores.get(neighbor_id, 0.0) + (impact * weight)

    # 3. Формуємо повний список кандидатів з УСІХ метаданих
    candidates = []
    
    for aid, meta in METADATA.items():
        # Відсіюємо ті, що вже оцінені (крім SKIP)
        if aid in rated_ids:
            continue
            
        # Отримуємо розрахований score або 0.0
        score = scores.get(aid, 0.0)
        
        candidates.append({
            "id": aid,
            "title": meta['title'],
            "popularity": meta['popularity'],
            "score": score,
            "picture": meta.get("picture", ""), # <-- ДОДАНО
            # Якщо в метаданих немає кластера, ставимо None (це не критично для сортування)
            "cluster_id": None 
        })
            
    # 4. Глобальне сортування
    # Ключ: 
    #  1. Score (від більшого до меншого). Позитивні -> Нуль -> Негативні.
    #  2. Popularity (від більшого до меншого). При рівних Score перемагає популярність.
    candidates.sort(key=lambda x: (x['score'], x['popularity']), reverse=True)
    
    # 5. Повертаємо зріз (Pagination support)
    return candidates[:limit]
