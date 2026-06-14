from fastapi import APIRouter
from models.schemas import SearchResponse, SearchResponseItem
from services.vector_store import vector_store

router = APIRouter()

@router.get("/search", response_model=SearchResponse)
async def semantic_search(q: str):
    results = vector_store.search(q, n_results=20)
    
    items = []
    if results and "ids" in results and len(results["ids"]) > 0:
        ids = results["ids"][0]
        distances = results.get("distances", [[]])[0] if "distances" in results else []
        documents = results.get("documents", [[]])[0] if "documents" in results else []
        
        for i, doc_id in enumerate(ids):
            score = distances[i] if i < len(distances) else 0.0
            doc_text = documents[i] if i < len(documents) and documents[i] is not None else ""
            
            # Hybrid match: semantic similarity OR keyword match
            # For short queries, we relax the distance threshold slightly or rely on keyword
            is_semantic_match = score < 1.65
            
            q_clean = q.lower().strip()
            q_robust = q_clean.replace(" ", "").replace("-", "")
            doc_robust = doc_text.lower().replace(" ", "").replace("-", "")
            
            is_keyword_match = q_clean in doc_text.lower() or (len(q_robust) > 3 and q_robust in doc_robust)
            
            if is_semantic_match or is_keyword_match:
                items.append(SearchResponseItem(publicationId=doc_id, score=score))
            
    # Return top 5 items
    items = sorted(items, key=lambda x: x.score)[:5]
    return SearchResponse(results=items)
