from pydantic import BaseModel

class NewsDTO(BaseModel):
    timestamp: str
    stock: str
    newsData: str

class SentimentResponseDTO(BaseModel):
    timestamp: str
    stock: str
    sentiment: str
    scores: dict