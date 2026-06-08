import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';

describe('App', () => {
  it('renders dashboard title', () => {
    render(<App />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders footer with all three texts', () => {
    render(<App />);
    expect(screen.getByText('Практика')).toBeInTheDocument();
    expect(screen.getByText('СМП - Система медленных платежей')).toBeInTheDocument();
    expect(screen.getByText('2026')).toBeInTheDocument();
  });

  it('has correct structure', () => {
    render(<App />);
    expect(document.querySelector('header')).toBeInTheDocument();
    expect(document.querySelector('main')).toBeInTheDocument();
    expect(document.querySelector('footer')).toBeInTheDocument();
  });

  it('shows loading then error for stats', async () => {
    render(<App />);
    expect(screen.getByText('Загрузка статистики...')).toBeInTheDocument();
    const errorMessage = await screen.findByText(/Ошибка загрузки статистики:/);
    expect(errorMessage).toBeInTheDocument();
  });

  it('shows loading then error for transactions', async () => {
    render(<App />);
    expect(screen.getByText('Загрузка транзакций...')).toBeInTheDocument();
    const errorMessage = await screen.findByText(/Ошибка загрузки транзакций:/);
    expect(errorMessage).toBeInTheDocument();

  });
});
