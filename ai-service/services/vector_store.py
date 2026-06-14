import chromadb
from config import settings
from services.embedding_service import embedding_service

class VectorStore:
    def __init__(self):
        self.client = chromadb.PersistentClient(path=settings.CHROMA_DB_DIR)
        self.collection = self.client.get_or_create_collection(name="publications")

    def index_publication(self, publication_id: str, text: str):
        embedding = embedding_service.encode(text)
        # Check if exists to update or insert
        try:
            self.collection.upsert(
                ids=[publication_id],
                embeddings=[embedding],
                documents=[text]
            )
        except Exception as e:
            print(f"Error indexing publication: {e}")

    def search(self, query: str, n_results: int = 20):
        embedding = embedding_service.encode(query)
        try:
            # Handle empty collection case gracefully
            if self.collection.count() == 0:
                return {"ids": [[]], "distances": [[]]}
            results = self.collection.query(
                query_embeddings=[embedding],
                n_results=min(n_results, self.collection.count()),
                include=["distances", "documents", "metadatas"]
            )
            return results
        except Exception as e:
            print(f"Error searching: {e}")
            return {"ids": [[]], "distances": [[]]}

    def recommend(self, publication_id: str, n_results: int = 5):
        try:
            if self.collection.count() == 0:
                return []
            
            # First, check if the ID exists in ChromaDB
            result = self.collection.get(ids=[publication_id], include=["embeddings"])
            
            if not result or not result["embeddings"] or len(result["embeddings"]) == 0:
                return []

            embedding = result["embeddings"][0]
            
            # Query the closest n_results + 1 to exclude itself
            results = self.collection.query(
                query_embeddings=[embedding],
                n_results=min(n_results + 1, self.collection.count())
            )
            
            recommended_ids = []
            if results and "ids" in results and len(results["ids"]) > 0:
                for idx, pid in enumerate(results["ids"][0]):
                    if pid != publication_id:
                        recommended_ids.append(pid)
            
            return recommended_ids[:n_results]
        except Exception as e:
            print(f"Error recommending: {e}")
            return []

vector_store = VectorStore()
