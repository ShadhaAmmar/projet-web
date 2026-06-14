import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "ai-service"
    PORT: int = 8085
    GROQ_API_KEY: str = ""
    CHROMA_DB_DIR: str = "./chroma_db"
    MODEL_NAME: str = "all-MiniLM-L6-v2"

    class Config:
        env_file = ".env"

settings = Settings()
