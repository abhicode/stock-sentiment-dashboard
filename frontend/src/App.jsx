import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import StockPage from "./pages/StockPage";
import HomePage from "./pages/HomePage";
import { createTheme, CssBaseline, ThemeProvider } from "@mui/material";

const theme = createTheme();

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/" element={<HomePage />} />
          {/* You could also add dynamic route: */}
          <Route path="/stock/:symbol" element={<StockPage />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App
