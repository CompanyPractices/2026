import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';

describe('App', () => {
  it('renders dashboard', () => {
    render(<App />);
    expect(screen.getByText('DASHBOARD')).toBeInTheDocument();
  });

  it('shows connecting state initially', () => {
    render(<App />);
    expect(screen.getByText('⏳ Connecting...')).toBeInTheDocument();
  });
});
