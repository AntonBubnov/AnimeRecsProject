from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os

# Отримуємо URL з змінних середовища (для Render)
# Якщо змінної немає, використовуємо локальний SQLite (для розробки)
SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL")

if SQLALCHEMY_DATABASE_URL:
    # Виправлення для сумісності (Render видає postgres://, а SQLAlchemy хоче postgresql://)
    if SQLALCHEMY_DATABASE_URL.startswith("postgres://"):
        SQLALCHEMY_DATABASE_URL = SQLALCHEMY_DATABASE_URL.replace("postgres://", "postgresql://", 1)
        
    # Для PostgreSQL check_same_thread не потрібен
    engine = create_engine(SQLALCHEMY_DATABASE_URL)
else:
    # Шлях до файлу бази даних
    BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    DB_PATH = os.path.join(BASE_DIR, 'data', 'database.db')
    SQLALCHEMY_DATABASE_URL = f"sqlite:///{DB_PATH}"

    # Створення двигуна (engine)
    engine = create_engine(
        SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
    )

# Фабрика сесій (через неї ми будемо робити запити)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Базовий клас для моделей
Base = declarative_base()

# Функція для отримання сесії (Dependency Injection)
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
