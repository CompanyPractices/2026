import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';

describe('App', () => {
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

  it('passes mock data to Header', () => {
    render(<App />);
    expect(screen.getByText('Всего ТХ')).toBeInTheDocument();
    expect(screen.getByText('Одобрено')).toBeInTheDocument();
    expect(screen.getByText('Общая сумма')).toBeInTheDocument();
    expect(screen.getByText('Среднее время')).toBeInTheDocument();
  });
});
