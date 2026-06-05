import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';

describe('App', () => {
  it('renders dashboard title', () => {
    render(<App />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders KPI cards with mock data', () => {
    render(<App />);

    expect(screen.getByText(/1250/)).toBeInTheDocument();
    expect(screen.getByText(/Всего ТХ/)).toBeInTheDocument();
    expect(screen.getByText(/88/)).toBeInTheDocument();
    expect(screen.getByText(/Одобрено/)).toBeInTheDocument();
    expect(screen.getByText(/1 875 000.00/)).toBeInTheDocument();
    expect(screen.getByText(/Общая сумма/)).toBeInTheDocument();
    expect(screen.getByText(/38/)).toBeInTheDocument();
    expect(screen.getByText(/Среднее время/)).toBeInTheDocument();
  });
});