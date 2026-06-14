from keybert import KeyBERT
from config import settings

class KeywordService:
    def __init__(self):
        self.kw_model = KeyBERT(model=settings.MODEL_NAME)

    def extract_keywords(self, text: str, top_n: int = 8) -> list[str]:
        keywords = self.kw_model.extract_keywords(text, keyphrase_ngram_range=(1, 2), stop_words='english', top_n=top_n)
        return [kw[0] for kw in keywords]

keyword_service = KeywordService()
