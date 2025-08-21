# Real-Time Stock Market & News Sentiment Dashboard

### A real-time stock market sentiment dashboard that integrates:
- ```Spring Boot``` (backend for business logic, REST APIs, WebSocket broadcasting)
- ```React``` (frontend dashboard UI)
- ```FastAPI``` (ML microservice for sentiment analysis)
- ```Apache Kafka``` (real-time messaging between services)
- ```Docker``` & ```Docker Compose``` (containerized multi-service deployment)

### Features
- Fetches real-time stock data from external APIs
- Processes news headlines and description with an ML pipeline (```FastAPI``` + NLP model)
- Live sentiment visualization on stock charts via ```WebSockets```
- Microservice architecture (separate services for backend, frontend, ML)
- Fully containerized with ```Docker``` and ```docker-compose```
- Additional notes:-
  - Displays only the sentiment data when the US market is closed
  - Displays the latest sentiment mapped to the price during market hours
  - Same behaviour as above in the historical/trend chart
  - Fetches news and stock data every 10 minutes due to API constraints

## Getting Started

### Prerequisites
- ```Docker``` & ```Docker Compose```
- (Optional for dev) ```Java 21```, ```Node.js```, ```Python 3.9+```
- Stock API Key from https://finnhub.io/

### 1. Clone the Repository
```bash
git clone https://github.com/abhicode/stock-sentiment-dashboard.git"
cd stock-sentiment-dashboard
```
### 2. Set up Environment Variables

Create a ```.env``` file in the root.
```bash
STOCK_API_KEY=your_api_key_here
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/realtimeinsighthub
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=password
```

### 3. Run with Docker Compose
```bash
docker-compose up --build
```

### 4. Access the App

- Frontend: http://localhost:3000
- Backend (Spring Boot): http://localhost:8080
- FastAPI ML Service: http://localhost:8000

### 5. Services Overview
| Service | Tech | Description |
|:------------|:--------------:|:-------------|
| backend      | ```Spring Boot```    | REST APIs, WebSocket, Kafka Producer/Consumer        |
| frontend     | ```React``` + ```Vite```   | Dashboard UI with Real-Time Charts |
| ml-service   | ```FastAPI```, ```NLP```   | Sentiment Analysis pipeline for news |
| Kafka        | ```Apache Kafka```   | Event Streaming |

Home Page at http://localhost:3000

<img width="1458" height="827" alt="Screenshot from 2025-08-21 14-46-45" src="https://github.com/user-attachments/assets/8e5888ff-df1d-46a9-b17a-046e45f288b2" />

Stock Sentiment Analysis (Ranged, without WebSocket updates)

<img width="1458" height="827" alt="Screenshot from 2025-08-21 14-44-46" src="https://github.com/user-attachments/assets/43d6d96f-e5af-42c3-821c-c5abfc270943" />










