from sentence_transformers import SentenceTransformer
from config import settings

class EmbeddingService:
    def __init__(self):
        self.model = SentenceTransformer(settings.MODEL_NAME)

    def encode(self, text: str) -> list[float]:
        return self.model.encode(text).tolist()

embedding_service = EmbeddingService()
