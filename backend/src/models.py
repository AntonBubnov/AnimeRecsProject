from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from .database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    password_hash = Column(String)  # Зберігаємо ТІЛЬКИ хеш, не пароль!

    # Зв'язок з оцінками (один до багатьох)
    ratings = relationship("Rating", back_populates="owner")

class Rating(Base):
    __tablename__ = "ratings"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    anime_id = Column(Integer)  # ID з metadata.json
    rating_type = Column(String)  # 'LIKE', 'DISLIKE', 'SKIP', 'PLAN'
    timestamp = Column(DateTime, default=datetime.utcnow)

    # Зв'язок з користувачем
    owner = relationship("User", back_populates="ratings")
