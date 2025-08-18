import React, { useState } from "react";
import { useParams } from "react-router-dom";
import StockChart from "../components/StockChart";
import { Container, Typography, Button, Select, MenuItem } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

export default function StockPage() {
  const { symbol } = useParams();
  const [range, setRange] = useState("7d");
  
  return (
    <div>
      <Container maxWidth="lg" sx={{ mt: 4, mb: 2 }}>
        <Typography variant="h4" gutterBottom>
          Stock Chart: {symbol}
        </Typography>
        <Button 
          variant="outlined" 
          component={RouterLink} 
          to="/"
          sx={{ mb: 2 }}
        >
          Back to Stocks
        </Button>

        {/* Live chart */}
        <div style={{ padding: "0 20px", marginBottom: "40px" }}>
          <StockChart stock={symbol} mode="live" />
        </div>

        {/* Trend Range Selector */}
        <div style={{ marginBottom: "20px" }}>
          <Typography variant="subtitle1" gutterBottom>
            Select Trend Range:
          </Typography>
          <Select 
            value={range} 
            onChange={(e) => setRange(e.target.value)}
            size="small"
          >
            <MenuItem value="1d">1 Day</MenuItem>
            <MenuItem value="7d">1 Week</MenuItem>
            <MenuItem value="1m">1 Month</MenuItem>
          </Select>
        </div>

        {/* Trend chart */}
        <div style={{ padding: "0 20px" }}>
          <StockChart stock={symbol} mode="trend" range={range} />
        </div>
      </Container>
    </div>
  );
}