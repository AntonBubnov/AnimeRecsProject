from passlib.context import CryptContext
from datetime import datetime, timedelta
from jose import jwt

# Налаштування хешування
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# СЕКРЕТНИЙ КЛЮЧ (у продакшені його треба ховати в змінні середовища!)
SECRET_KEY = "my_super_secret_key_change_me_please"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7  # Токен діє 7 днів

# 1. Функція хешування пароля
def get_password_hash(password):
    return pwd_context.hash(password)

# 2. Функція перевірки пароля
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

# 3. Створення токена доступу (JWT)
def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt
