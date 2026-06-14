from fastapi import APIRouter
from models.schemas import IndexRequest, IndexResponse
from services.vector_store import vector_store

router = APIRouter()

@router.post("/index", response_model=IndexResponse)
async def index_publication(request: IndexRequest):
    combined_text = f"{request.title} {request.abstract} " + " ".join(request.keywords)
    vector_store.index_publication(request.publicationId, combined_text)
    return IndexResponse(message="Publication indexed successfully")
