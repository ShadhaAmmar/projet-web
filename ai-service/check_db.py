import chromadb
client = chromadb.PersistentClient(path="chroma_db")
col = client.get_or_create_collection(name="publications")
print(col.get())
