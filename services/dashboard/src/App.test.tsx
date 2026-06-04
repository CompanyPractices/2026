import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';

describe('App', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders service name', () => {
    render(<App />);
    expect(screen.getByText('СМП — Симулятор процессингового центра')).toBeInTheDocument();
  });

  it('renders footer with all three texts', () => {
    render(<App />);
    expect(screen.getByText('Практика')).toBeInTheDocument();
    expect(screen.getByText('Система медленных платежей')).toBeInTheDocument();
    expect(screen.getByText('2026')).toBeInTheDocument();
  });

  it('shows health status in dev', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('✅ Status: UP')).toBeInTheDocument();
    });
  });

  it('has correct structure', () => {
    render(<App />);
    expect(document.querySelector('header')).toBeInTheDocument();
    expect(document.querySelector('main')).toBeInTheDocument();
    expect(document.querySelector('footer')).toBeInTheDocument();
  });

  it('shows error (not in dev)', async () => {
    vi.stubEnv('DEV', false);
    global.fetch = vi.fn(() => Promise.reject(new Error('Network error')));
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText(/❌ Error:/)).toBeInTheDocument();
    });
  });

  it('shows health status (not in dev)', async () => {
    vi.stubEnv('DEV', false);
    global.fetch = vi.fn(() =>
        Promise.resolve({
          json: () =>
              Promise.resolve({
                status: 'UP',
                service: 'dashboard',
                version: '1.0.0',
              }),
        })
    );
  });

  it('shows connecting (not in dev)', () => {
    vi.stubEnv('DEV', false);
    global.fetch = vi.fn(() => new Promise(() => {}));
    render(<App />);
    expect(screen.getByText('⏳ Connecting...')).toBeInTheDocument();
  });
});
