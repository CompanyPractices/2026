import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { ThemeProvider } from './contexts/ThemeProvider';
import { BrowserRouter } from 'react-router-dom';
import { ToastProvider } from './contexts/ToastProvider';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ToastProvider>
            <BrowserRouter>
                <ThemeProvider>
                    <App />
                </ThemeProvider>
            </BrowserRouter>
        </ToastProvider>
    </React.StrictMode>
);
