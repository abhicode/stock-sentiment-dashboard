import React, { useEffect, useState, useRef } from "react";
import { Line } from "react-chartjs-2";
import axios from "axios";
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  Chart as ChartJS,
  TimeScale,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend,
  CategoryScale
} from "chart.js";
import "chartjs-adapter-date-fns";

ChartJS.register(
  TimeScale,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend,
  CategoryScale
);

export default function StockChart({ stock, mode = "live", range = "1d" }) {
  const [chartData, setChartData] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState('Connecting...');
  const clientRef = useRef(null);
  const stockSubscriptionRef = useRef(null);
  const sentimentSubscriptionRef = useRef(null);

  useEffect(() => {
    console.log('STOCK:', stock);

    let url = mode === "trend"
      ? `/api/chart/${stock}/trend?range=${range}`
      : `/api/chart/${stock}/live`;
    
    axios.get(url)
      .then((res) => setChartData(res.data))
      .catch((err) => console.error(err));
  }, [stock, mode, range]);

  useEffect(() => {

    if (mode !== "live") return; // no websocket for trend

    const client = new Client({
      webSocketFactory: () => new SockJS('/api/ws'),
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      onConnect: (frame) => {
        console.log('WebSocket Connected:', frame);
        setConnectionStatus('Connected');

        stockSubscriptionRef.current = client.subscribe('/topic/stock', (message) => {
          try {
            const stockUpdate = JSON.parse(message.body);
            console.log('Received stock update:', stockUpdate);

            if (stockUpdate.stock === stock) {
              setChartData(prevData => {
                if (!prevData) return prevData;

                const existingIndex = prevData.findIndex(
                  item => new Date(item.timestamp).getTime() === new Date(stockUpdate.timestamp).getTime()
                );

                if (existingIndex !== -1) {
                  const updatedData = [...prevData];
                  updatedData[existingIndex] = {
                    ...updatedData[existingIndex],
                    price: stockUpdate.price
                  };
                  return updatedData;
                } else {
                  // Add new entry (keep last 100 data points)
                  const newData = [...prevData, {
                    timestamp: stockUpdate.timestamp,
                    stock: stockUpdate.stock,
                    price: stockUpdate.price,
                    sentiment: prevData[prevData.length - 1]?.sentiment || 'neutral',
                    compound: prevData[prevData.length - 1]?.compound || 0
                  }].slice(-100);
                  
                  return newData;
                }
              });
            }
          } catch (error) {
            console.error('Error processing stock update:', error);
          }
        });

        sentimentSubscriptionRef.current = client.subscribe('/topic/sentiment', (message) => {
          try {
            const sentimentUpdate = JSON.parse(message.body);
            console.log('Received sentiment update:', sentimentUpdate);
            
            // Only update if it's for the current stock
            if (sentimentUpdate.stock === stock) {
              setChartData(prevData => {
                if (!prevData) return prevData;
                
                // Find the most recent entry for this stock or create new one
                const updatedData = [...prevData];
                const existingIndex = updatedData.findIndex(
                  item => new Date(item.timestamp).getTime() === new Date(sentimentUpdate.timestamp).getTime()
                );

                if (existingIndex !== -1) {
                  updatedData[existingIndex] = {
                    ...updatedData[existingIndex],
                    sentiment: sentimentUpdate.sentiment,
                    compound: sentimentUpdate.compound
                  };
                } else {
                  updatedData.push({
                    timestamp: sentimentUpdate.timestamp,
                    stock: sentimentUpdate.stock,
                    price: updatedData[updatedData.length - 1]?.price || 0,
                    sentiment: sentimentUpdate.sentiment,
                    compound: sentimentUpdate.compound
                  });
                }
                
                return updatedData.slice(-100); // Keep last 100 data points
              });
            }
          } catch (error) {
            console.error('Error processing sentiment update:', error);
          }
        });
      },

      onWebSocketError: (error) => {
        console.error('WebSocket Error:', error);
        setConnectionStatus('Connection Error');
      },
      
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
        setConnectionStatus('STOMP Error');
      },
      
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Store client reference and activate
    clientRef.current = client;
    client.activate();

    // Cleanup function
    return () => {
      if (stockSubscriptionRef.current) {
        stockSubscriptionRef.current.unsubscribe();
      }
      if (sentimentSubscriptionRef.current) {
        sentimentSubscriptionRef.current.unsubscribe();
      }
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [stock]); // Re-run when stock changes


  // if (!chartData) return <p>Loading chart...</p>;

  if (!chartData || chartData.length === 0) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <p>Loading chart data...</p>
        <p>WebSocket Status: <span style={{ 
          color: connectionStatus === 'Connected' ? 'green' : 
                 connectionStatus === 'Connecting...' ? 'orange' : 'red' 
        }}>{connectionStatus}</span></p>
      </div>
    );
  }

  const labels = chartData.map((d) => new Date(d.timestamp));
  const priceData = chartData.map((d) => d.price);
  const sentimentData = chartData.map((d) => d.compound);
  const sentimentColors = chartData.map((d) =>
    d.sentiment === "positive" ? "green" :
    d.sentiment === "negative" ? "red" : "gray"
  );

  const data = {
    labels,
    datasets: [
      {
        label: "Stock Price (USD)",
        data: priceData,
        yAxisID: "y1",
        borderColor: "blue",
        backgroundColor: "blue",
        tension: 0.3
      },
      {
        label: "Sentiment Score",
        data: sentimentData,
        yAxisID: "y2",
        borderColor: "black",
        pointBackgroundColor: sentimentColors,
        backgroundColor: sentimentColors,
        tension: 0.3
      }
    ]
  };

  const options = {
    responsive: true,
    interaction: {
      mode: "index",
      intersect: false
    },
    stacked: false,
    scales: {
      x: {
        type: "time",
        time: { unit: "hour" }
      },
      y1: {
        type: "linear",
        display: true,
        position: "left",
        title: { display: true, text: "Price (USD)" }
      },
      y2: {
        type: "linear",
        display: true,
        position: "right",
        title: { display: true, text: "Sentiment Score" },
        min: -1,
        max: 1
      }
    },
    plugins: {
      tooltip: {
        callbacks: {
          label: function (context) {
            const dataIndex = context.dataIndex;
            const d = chartData[dataIndex];
            return [
              `Price: $${d.price}`,
              `Sentiment: ${d.sentiment}`,
              `Score: ${d.compound}`
            ];
          }
        }
      }
    },
    animation: {
      duration: 750,
      easing: 'easeInOutQuart'
    }
  };

  const latestData = chartData[chartData.length - 1];

  return (
    <div>
      {/* Status Header */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        padding: '10px',
        backgroundColor: '#f5f5f5',
        borderRadius: '5px',
        marginBottom: '10px'
      }}>
        <div>
          <h3>{stock} - {mode === "live" ? "Real-time Chart" : `Trend (${range})`}</h3>
          {mode === "live" && <small>Last update: {latestData ? new Date(latestData.timestamp).toLocaleString() : 'N/A'}</small>}
        </div>
        <div style={{ textAlign: 'right' }}>
          {mode === "live" && (<div style={{ 
              color: connectionStatus === 'Connected' ? 'green' : 
                    connectionStatus === 'Connecting...' ? 'orange' : 'red',
              fontWeight: 'bold'
            }}>
            ● {connectionStatus}
          </div>)}
          {latestData && (
            <div>
              <div>Price: ${latestData.price}</div>
              <div style={{ 
                color: latestData.sentiment === 'positive' ? 'green' : 
                       latestData.sentiment === 'negative' ? 'red' : 'gray' 
              }}>
                Sentiment: {latestData.sentiment} ({latestData.compound?.toFixed(2)})
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Chart */}
      <Line options={options} data={data} />
      
      {/* Data Points Info */}
      <div style={{ 
        marginTop: '10px', 
        padding: '10px', 
        backgroundColor: '#f9f9f9', 
        borderRadius: '5px',
        fontSize: '12px',
        color: '#666'
      }}>
        {mode === "live" ? (
          <>
            Showing {chartData.length} data points (last 100) • 
            Real-time updates via WebSocket • 
            Chart updates automatically
          </>
        ) : (
          <>
            Showing {chartData.length} historical data points • 
            Trend range: {range}
          </>
        )}
      </div>
    </div>
  );
}
