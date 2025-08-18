import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { 
  Container, 
  Typography, 
  List, 
  ListItem, 
  ListItemButton, 
  ListItemText, 
  Paper 
} from '@mui/material';

const stockList = [
  { symbol: 'AAPL', name: 'Apple Inc.' },
  { symbol: 'TSLA', name: 'Tesla, Inc.' },
  { symbol: 'MSFT', name: 'Microsoft Corp.' },
  { symbol: 'GOOGL', name: 'Alphabet Inc.' },
  { symbol: 'AMZN', name: 'Amazon.com, Inc.' },
  { symbol: 'NVDA', name: 'Nvidia Corporation' },
  { symbol: 'META', name: 'Meta Platforms, Inc.' },
];

export default function HomePage() {
  return (
    <Container maxWidth="lg" sx={{ mt: 4 }}>
      <Typography variant="h4" gutterBottom>
        Choose a Stock
      </Typography>
      <Paper elevation={3}>
        <List>
          {stockList.map((stock) => (
            <ListItem key={stock.symbol} disablePadding>
              <ListItemButton 
                component={RouterLink} 
                to={`/stock/${stock.symbol}`}
              >
                <ListItemText 
                  primary={stock.name} 
                  secondary={stock.symbol} 
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Paper>
    </Container>
  );
}
