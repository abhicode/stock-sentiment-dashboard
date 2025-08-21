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
  const [connectionStatus, setConnectionStatus] = useState('Connecting...');
  const clientRef = useRef(null);
  const stockSubscriptionRef = useRef(null);
  const sentimentSubscriptionRef = useRef(null);
  const [priceSeries, setPriceSeries] = useState([]);
  const [sentimentSeries, setSentimentSeries] = useState([]);
  const [isInitialLoad, setIsInitialLoad] = useState(true);

  useEffect(() => {
    console.log('STOCK:', stock);

    let url = mode === "trend"
      ? `/api/chart/${stock}/trend?range=${range}`
      : `/api/chart/${stock}/live`;
    
    axios.get(url)
      .then((res) => {
        const data = res.data;

        const price = data
          .filter(d => d.price && d.price !== undefined && d.price !== null)
          .map(d => ({
          x: new Date(d.timestamp),
          y: d.price
          }));

        const sentiment = data
          .filter(d => d.sentiment && d.compound !== undefined && d.compound !== null)
          .map(d => ({
            x: new Date(d.timestamp),
            y: d.compound,
            sentiment: d.sentiment
          }));

        setPriceSeries(price);
        setSentimentSeries(sentiment);
        setIsInitialLoad(false);
      })
      .catch((err) => {
        console.error(err);
        setIsInitialLoad(false);
      });
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
              setPriceSeries(prev => {
                if (!prev) return [];

                const existingIndex = prev.findIndex(
                  item => new Date(item.x).getTime() === new Date(stockUpdate.timestamp).getTime()
                );

                if (existingIndex !== -1) {
                  // replace existing point - should never happen
                  const updated = [...prev];
                  updated[existingIndex] = { x: new Date(stockUpdate.timestamp), y: stockUpdate.price };
                  return updated;
                } else {
                  // append new point, keep last 100
                  return [
                    ...prev,
                    { x: new Date(stockUpdate.timestamp), y: stockUpdate.price }
                  ].slice(-100);
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
              setSentimentSeries(prev => {
                if (!prev) return [];

                const existingIndex = prev.findIndex(
                  item => new Date(item.x).getTime() === new Date(sentimentUpdate.timestamp).getTime()
                );

                if (existingIndex !== -1) {
                  // replace existing point - should never happen - we are merging news with same timestamp
                  const updated = [...prev];
                  updated[existingIndex] = { 
                    x: new Date(sentimentUpdate.timestamp), 
                    y: sentimentUpdate.compound,
                    sentiment: sentimentUpdate.sentiment
                  };
                  return updated;
                } else {
                  // append new point
                  return [
                    ...prev,
                    { 
                      x: new Date(sentimentUpdate.timestamp), 
                      y: sentimentUpdate.compound,
                      sentiment: sentimentUpdate.sentiment
                    }
                  ].slice(-100);
                }
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

      onDisconnect: () => {
        console.log('WebSocket Disconnected');
        setConnectionStatus('Disconnected');
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
      console.log('Cleaning up WebSocket connections...');
      try {
        if (stockSubscriptionRef.current) {
          stockSubscriptionRef.current.unsubscribe();
          stockSubscriptionRef.current = null;
        }
        if (sentimentSubscriptionRef.current) {
          sentimentSubscriptionRef.current.unsubscribe();
          sentimentSubscriptionRef.current = null;
        }
        if (clientRef.current && clientRef.current.connected) {
          clientRef.current.deactivate();
        }
        clientRef.current = null;
      } catch (error) {
        console.error('Error during WebSocket cleanup:', error);
      }
    };
  }, [stock]); // Re-run when stock changes

  // Show loading only during initial load
  if (isInitialLoad) {
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

  const datasets = [
    {
      label: "Stock Price (USD)",
      data: priceSeries || [],
      yAxisID: "y1",
      borderColor: "blue",
      backgroundColor: "blue",
      tension: 0.3,
      pointRadius: 3,
      pointHoverRadius: 5,
      hidden: !priceSeries || priceSeries.length === 0, // Hide if no data
    },
    {
      label: "Sentiment Score",
      data: sentimentSeries || [],
      yAxisID: "y2",
      borderColor: "rgba(0,0,0,0.3)",
      backgroundColor: "transparent",
      pointBackgroundColor: (sentimentSeries || []).map(d =>
        d.y > 0.1 ? "green" : d.y < -0.1 ? "red" : "gray"
      ),
      pointBorderColor: (sentimentSeries || []).map(d =>
        d.y > 0.1 ? "darkgreen" : d.y < -0.1 ? "darkred" : "darkgray"
      ),
      spanGaps: false,
      tension: 0,
      hidden: !sentimentSeries || sentimentSeries.length === 0, // Hide if no data
    }
  ];

  const data = { datasets };

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
        time: { 
          unit: mode === "live" ? "minute" : "hour",
          displayFormats: {
            minute: 'HH:mm',
            hour: 'MMM dd HH:mm'
          }
        },
        title: { 
          display: true, 
          text: "Time" 
        }
      },
      y1: {
        type: "linear",
        display: true,
        position: "left",
        title: { display: true, text: "Price (USD)" },
        grid: {
          drawOnChartArea: true,
        }
      },
      y2: {
        type: "linear",
        display: true,
        position: "right",
        title: { display: true, text: "Sentiment Score" },
        min: -1,
        max: 1,
        grid: {
          drawOnChartArea: false, // Don't overlap grid lines
        },
        ticks: {
          callback: function(value) {
            if (value === 1) return 'Very Positive';
            if (value === 0.5) return 'Positive';
            if (value === 0) return 'Neutral';
            if (value === -0.5) return 'Negative';
            if (value === -1) return 'Very Negative';
            return value.toFixed(1);
          }
        }
      }
    },
    plugins: {
      tooltip: {
        callbacks: {
          label: function (context) {
            const datasetLabel = context.dataset.label;
            const d = context.raw;

            if (datasetLabel === "Stock Price (USD)") {
              return `Price: $${d.y.toFixed(2)}`;
            }

            if (datasetLabel === "Sentiment Score") {
              return [
                `Sentiment: ${d.sentiment || 'neutral'}`,
                `Score: ${d.y.toFixed(3)}`,
                `Time: ${new Date(d.x).toLocaleString()}`
              ];
            }

            return null;
          }
        }
      },
      legend: {
        display: true,
        labels: {
          generateLabels: function(chart) {
            const original = ChartJS.defaults.plugins.legend.labels.generateLabels;
            const labels = original.call(this, chart);
            
            // Add custom info for sentiment dataset
            labels.forEach(label => {
              if (label.text === 'Sentiment Score') {
                if (!priceSeries || priceSeries.length === 0) {
                  label.text += ' (Points only when news available)';
                } else {
                  label.text += ' (as of last news before price time)';
                }
              }
            });
            
            return labels;
          }
        }
      }
    },
    animation: {
      duration: 750,
      easing: 'easeInOutQuart'
    }
  };

  const hasAnyData = (priceSeries && priceSeries.length > 0) || 
    (sentimentSeries && sentimentSeries.length > 0);

  if (!hasAnyData) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        {mode === "live" ? 
          (<p>No chart data available for {stock} in last one hour</p>) :
          (<p>No chart data available for {stock}</p>)}
        <p>WebSocket Status: <span style={{ 
          color: connectionStatus === 'Connected' ? 'green' : 
                 connectionStatus === 'Connecting...' ? 'orange' : 'red' 
        }}>{connectionStatus}</span></p>
        <p style={{ fontSize: '12px', color: '#666' }}>
          {mode === "live" ? "Waiting for real-time data..." : "No historical data found"}
        </p>
      </div>
    );
  }

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
          <h3>
            {stock} - {mode === "live" ? "Real-time Chart" : `Trend (${range})`}
          </h3>
          <small>
            Last price update:{" "}
            {priceSeries.length > 0
              ? new Date(priceSeries[priceSeries.length - 1].x).toLocaleString()
              : "N/A"}
          </small>
        </div>

        <div style={{ textAlign: "right" }}>
          {mode === "live" && (
            <div
              style={{
                color:
                  connectionStatus === "Connected"
                    ? "green"
                    : connectionStatus === "Connecting..."
                    ? "orange"
                    : "red",
                fontWeight: "bold",
              }}
            >
              ● {connectionStatus}
            </div>
          )}

          {priceSeries.length > 0 && (
            <div>
              <div>
                Price: ${priceSeries[priceSeries.length - 1].y}
              </div>
              {sentimentSeries.length > 0 && (
                <div
                  style={{
                    color:
                      sentimentSeries[sentimentSeries.length - 1].sentiment === "positive"
                        ? "green"
                        : sentimentSeries[sentimentSeries.length - 1].sentiment === "negative"
                        ? "red"
                        : "gray",
                  }}
                >
                  Sentiment: {sentimentSeries[sentimentSeries.length - 1].sentiment} (
                  {sentimentSeries[sentimentSeries.length - 1].y?.toFixed(2)})
                </div>
              )}
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
            Showing {priceSeries?.length || 0} price data points (last 100) • 
            Showing {sentimentSeries?.length || 0} sentiment data points (last 100) • 
            Real-time updates via WebSocket • 
            Chart updates automatically
          </>
        ) : (
          <>
            Showing {priceSeries?.length || 0} price data points • 
            Showing {sentimentSeries?.length || 0} sentiment data points • 
            Trend range: {range}
          </>
        )}
      </div>
    </div>
  );
}
