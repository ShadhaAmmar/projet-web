from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import SummarizeResponse
from services.rag_service import summarize_pdf
from config import settings

router = APIRouter()

@router.post("/summarize", response_model=SummarizeResponse)
async def summarize_publication_pdf(file: UploadFile = File(...)):
    """
    Accept a PDF upload and return an AI-generated academic abstract.
    Uses a RAG pipeline: text extraction → chunking → retrieval → Groq LLM.
    """
    if not settings.GROQ_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="GROQ_API_KEY is not configured on the server. Please add it to the .env file."
        )

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are accepted.")

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="Uploaded file is empty.")

    try:
        resume = await summarize_pdf(file_bytes, settings.GROQ_API_KEY)
        return SummarizeResponse(resume=resume)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=502, detail=str(e))
