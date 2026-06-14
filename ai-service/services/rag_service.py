"""
RAG pipeline for PDF summarization.
Extracts text from a PDF, retrieves the most relevant chunks via
sentence-transformers, then calls the Groq LLM (free tier) to generate
a concise academic abstract.
"""

import sys

import httpx

# ─── Constants ──────────────────────────────────────────────────────────────

CHUNK_SIZE    = 1800
CHUNK_OVERLAP = 200
TOP_K         = 8

GROQ_API_URL   = "https://api.groq.com/openai/v1/chat/completions"
PRIMARY_MODEL  = "llama-3.3-70b-versatile"
FALLBACK_MODEL = "llama-3.1-8b-instant"

SYSTEM_PROMPT = (
    "You are an expert academic assistant. "
    "Based ONLY on the source material provided, write a concise and informative abstract "
    "(3–5 sentences) that summarizes the research objectives, methodology, and key findings. "
    "Write in the third person, in formal academic language. "
    "Output ONLY the abstract text — no titles, no labels, no markdown."
)


# ─── Text Extraction ─────────────────────────────────────────────────────────

def extract_text_from_pdf(file_bytes: bytes) -> str:
    try:
        import fitz  # pymupdf
    except ModuleNotFoundError:
        raise RuntimeError("pymupdf is not installed. Run: pip install pymupdf")

    doc = fitz.open(stream=file_bytes, filetype="pdf")
    pages = [page.get_text() for page in doc]
    doc.close()
    return "\n\n".join(pages)


# ─── Chunking ────────────────────────────────────────────────────────────────

def chunk_text(text: str) -> list[str]:
    if not text.strip():
        return []
    chunks = []
    start = 0
    while start < len(text):
        end = start + CHUNK_SIZE
        chunk = text[start:end]
        if end < len(text):
            last_period = chunk.rfind(". ")
            if last_period > CHUNK_SIZE // 2:
                chunk = chunk[: last_period + 1]
                end = start + last_period + 1
        chunks.append(chunk.strip())
        start = end - CHUNK_OVERLAP
    return [c for c in chunks if c]


# ─── Retrieval ───────────────────────────────────────────────────────────────

def retrieve_top_chunks(chunks: list[str], query: str, top_k: int = TOP_K) -> list[str]:
    """Use sentence-transformers if available, else fall back to keyword scoring."""
    try:
        from sentence_transformers import SentenceTransformer, util as st_util
        model = SentenceTransformer("all-MiniLM-L6-v2")
        chunk_embeddings = model.encode(chunks, convert_to_tensor=True, show_progress_bar=False)
        query_embedding  = model.encode(query, convert_to_tensor=True)
        scores = st_util.cos_sim(query_embedding, chunk_embeddings)[0]
        top_indices = scores.topk(min(top_k, len(chunks))).indices.tolist()
        return [chunks[i] for i in top_indices]
    except Exception:
        # Keyword fallback
        query_words = set(query.lower().split())
        ranked = sorted(
            chunks,
            key=lambda c: len(query_words & set(c.lower().split())),
            reverse=True,
        )
        return ranked[:top_k]


# ─── LLM Call (Groq) ─────────────────────────────────────────────────────────

async def call_groq(context: str, api_key: str) -> str:
    user_msg = (
        f"Source material:\n\"\"\"\n{context}\n\"\"\"\n\n"
        "Write the academic abstract now."
    )
    payload = {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user",   "content": user_msg},
        ],
        "temperature": 0.4,
        "max_tokens":  512,
    }
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type":  "application/json",
        "User-Agent":    "IAtech-RAG/1.0",
    }

    for model in [PRIMARY_MODEL, FALLBACK_MODEL]:
        payload["model"] = model
        try:
            async with httpx.AsyncClient(timeout=90) as client:
                resp = await client.post(GROQ_API_URL, json=payload, headers=headers)
            if resp.status_code == 429:
                print(f"[rag] {model} rate-limited, trying fallback…", file=sys.stderr)
                continue
            resp.raise_for_status()
            data = resp.json()
            return data["choices"][0]["message"]["content"].strip()
        except httpx.HTTPStatusError as e:
            raise RuntimeError(f"Groq API error {e.response.status_code}: {e.response.text[:200]}")

    raise RuntimeError(
        "All Groq models are rate-limited. Please wait a moment and retry."
    )


# ─── Public entry-point ───────────────────────────────────────────────────────

async def summarize_pdf(file_bytes: bytes, api_key: str) -> str:
    text = extract_text_from_pdf(file_bytes)
    if not text.strip():
        raise ValueError("No readable text found in the PDF.")

    chunks = chunk_text(text)
    top_chunks = retrieve_top_chunks(
        chunks,
        query="research objectives methodology results findings contributions",
    )
    context = "\n\n---\n\n".join(top_chunks)
    return await call_groq(context, api_key)
