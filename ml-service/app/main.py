from fastapi import FastAPI
from typing import List
from app.models import NewsDTO, SentimentResponseDTO
from app.sentiment import analyze_sentiment

app = FastAPI()

@app.post("/analyze-sentiment", response_model=List[SentimentResponseDTO])
def analyze(news_list: List[NewsDTO]):
    results = []
    for news in news_list:
        sentiment, scores = analyze_sentiment(news.newsData)
        results.append(SentimentResponseDTO(
            timestamp=news.timestamp,
            stock=news.stock,
            sentiment=sentiment,
            scores=scores
        ))
    return results