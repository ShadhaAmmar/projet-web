from fastapi import APIRouter
from models.schemas import KeywordRequest, KeywordResponse
from services.keyword_service import keyword_service

router = APIRouter()

@router.post("/keywords", response_model=KeywordResponse)
async def extract_keywords(request: KeywordRequest):
    combined_text = f"{request.title} {request.abstract}"
    keywords = keyword_service.extract_keywords(combined_text)
    return KeywordResponse(keywords=keywords)
