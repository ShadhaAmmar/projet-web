from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from config import settings
from routes import search, keywords, recommend, index, summarize, autocomplete

app = FastAPI(title="AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Adjust to specific frontend URL in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routes
app.include_router(search.router,       prefix="/ai", tags=["Search"])
app.include_router(keywords.router,     prefix="/ai", tags=["Keywords"])
app.include_router(recommend.router,    prefix="/ai", tags=["Recommend"])
app.include_router(index.router,        prefix="/ai", tags=["Index"])
app.include_router(summarize.router,    prefix="/ai", tags=["Summarize"])
app.include_router(autocomplete.router, prefix="/ai", tags=["Autocomplete"])

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=settings.PORT, reload=False)
