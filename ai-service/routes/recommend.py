from fastapi import APIRouter
from models.schemas import RecommendResponse
from services.vector_store import vector_store

router = APIRouter()

@router.get("/recommend/{publicationId}", response_model=RecommendResponse)
async def recommend_publications(publicationId: str):
    recommended_ids = vector_store.recommend(publicationId, n_results=5)
    return RecommendResponse(recommendedIds=recommended_ids)
