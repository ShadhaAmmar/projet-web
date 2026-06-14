from pydantic import BaseModel
from typing import List

class SearchRequest(BaseModel):
    query: str

class SearchResponseItem(BaseModel):
    publicationId: str
    score: float

class SearchResponse(BaseModel):
    results: List[SearchResponseItem]

class KeywordRequest(BaseModel):
    title: str
    abstract: str

class KeywordResponse(BaseModel):
    keywords: List[str]

class RecommendResponse(BaseModel):
    recommendedIds: List[str]

class IndexRequest(BaseModel):
    publicationId: str
    title: str
    abstract: str
    keywords: List[str]

class IndexResponse(BaseModel):
    message: str

class SummarizeResponse(BaseModel):
    resume: str

class AutocompleteResponse(BaseModel):
    suggestions: List[str]
