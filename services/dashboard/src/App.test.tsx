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

  it('renders KPI cards with mock data', () => {
    render(<App />);
    expect(screen.getByText(/1250/)).toBeInTheDocument();
    expect(screen.getByText(/Всего ТХ/)).toBeInTheDocument();
    expect(screen.getByText(/88\s*%/)).toBeInTheDocument();
    expect(screen.getAllByText(/Одобрено/).length).toBeGreaterThan(0);
    expect(screen.getByText(/1 875 000.00/)).toBeInTheDocument();
    expect(screen.getByText(/Общая сумма/)).toBeInTheDocument();
    expect(screen.getByText(/38\s*ms/)).toBeInTheDocument();
    expect(screen.getByText(/Среднее время/)).toBeInTheDocument();
  });

  it ('render transaction table', () => {
    render(<App />);
    expect(screen.getByText(/Последние 20 транзакций/)).toBeInTheDocument();
  });
});
