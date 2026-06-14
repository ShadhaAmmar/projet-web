from fastapi import APIRouter, HTTPException, Query
from models.schemas import AutocompleteResponse
from config import settings
import httpx

router = APIRouter()

GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

@router.get("/autocomplete", response_model=AutocompleteResponse)
async def autocomplete(q: str = Query(..., min_length=2)):
    if not settings.GROQ_API_KEY:
        return AutocompleteResponse(suggestions=[])
    
    prompt = (
        f"You are a search autocomplete engine for an academic platform. "
        f"The user has typed the prefix: '{q}'. "
        f"Provide exactly 4 highly relevant, short autocomplete suggestions (max 4 words each) related to AI, computer science, or general science. "
        f"Make them sound natural, like Google Search suggestions. "
        f"Output ONLY a JSON array of strings. Do NOT output any other text."
    )

    payload = {
        "model": "llama-3.1-8b-instant", # Ultra-fast model for autocomplete
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.3,
        "max_tokens": 100,
        "response_format": {"type": "json_object"} # Wait, json_object requires a JSON object, not array.
    }
    
    # We will wrap the array in an object to satisfy Groq's json_object requirement if needed,
    # or just ask for an object containing the array.
    prompt = (
        f"You are a search autocomplete engine for an academic platform. "
        f"The user has typed: '{q}'. "
        f"Provide exactly 4 highly relevant, short autocomplete suggestions (max 4 words each) related to AI, computer science, or general science. "
        f"Output ONLY a JSON object with a single key 'suggestions' containing an array of strings."
    )
    payload["messages"][0]["content"] = prompt

    headers = {
        "Authorization": f"Bearer {settings.GROQ_API_KEY}",
        "Content-Type": "application/json",
        "User-Agent": "IAtech-Autocomplete/1.0"
    }

    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.post(GROQ_API_URL, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            import json
            parsed = json.loads(content)
            suggestions = parsed.get("suggestions", [])
            return AutocompleteResponse(suggestions=suggestions[:4])
    except Exception as e:
        print(f"Autocomplete error: {e}")
        return AutocompleteResponse(suggestions=[])
