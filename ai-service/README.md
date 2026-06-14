# AI Service

This is the AI microservice for the IA-Technology platform. It provides semantic search, auto keyword extraction, and publication recommendations.

## Requirements
- Python 3.9+
- The packages listed in `requirements.txt`

## Installation

1. Create a virtual environment (optional but recommended):
   ```bash
   python -m venv venv
   source venv/bin/activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running the Service

Ensure that Eureka is running. Then run:
```bash
uvicorn main:app --host 0.0.0.0 --port 8085
```
Or simply:
```bash
python main.py
```

## Endpoints

- `GET /ai/search?q=...` : Semantic Search
- `POST /ai/keywords` : Auto Keyword Extraction
- `GET /ai/recommend/{publicationId}` : Publication Recommender
- `POST /ai/index` : Index Publication
