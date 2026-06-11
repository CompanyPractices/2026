import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';

class MockWebSocket {
  url: string;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  readyState = 1;

  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  static instances: MockWebSocket[] = [];

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  close = vi.fn();
  send = vi.fn();
}

const mockUseStats = vi.hoisted(() => vi.fn());
const mockUseWebSocket = vi.hoisted(() => vi.fn());

const MOCK_STATS_SUCCESS = {
  transactionStats: {
    totalTransactions: 100,
    approvedTransactions: 80,
    totalAmount: 500000,
    totalProcessingTimeMs: 5000,
  },
  loading: false,
  error: null,
};

const MOCK_STATS_ERROR = {
  transactionStats: null,
  loading: false,
  error: 'Network error',
};

vi.mock('./hooks/useStats', () => ({
  default: mockUseStats,
}));

vi.mock('./hooks/useWebSocket', () => ({
  useWebSocket: mockUseWebSocket,
}));

describe('App', () => {
  beforeEach(() => {
    MockWebSocket.instances = [];
    global.WebSocket = MockWebSocket as unknown as typeof WebSocket;

    vi.clearAllMocks();

    mockUseWebSocket.mockReturnValue({
      liveTransactions: [],
      isConnected: false,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders dashboard title', async () => {
    mockUseStats.mockReturnValue(MOCK_STATS_SUCCESS);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });
  });

  it('renders footer with all three texts', async () => {
    mockUseStats.mockReturnValue(MOCK_STATS_SUCCESS);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Практика')).toBeInTheDocument();
      expect(screen.getByText('СМП - Система медленных платежей')).toBeInTheDocument();
      expect(screen.getByText('2026')).toBeInTheDocument();
    });
  });

  it('has correct structure', async () => {
    mockUseStats.mockReturnValue(MOCK_STATS_SUCCESS);

    render(<App />);

    await waitFor(() => {
      expect(document.querySelector('header')).toBeInTheDocument();
      expect(document.querySelector('main')).toBeInTheDocument();
      expect(document.querySelector('footer')).toBeInTheDocument();
    });
  });

  it('shows loading then error for stats', async () => {
    mockUseStats.mockReturnValue(MOCK_STATS_ERROR);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/Ошибка загрузки статистики:/)).toBeInTheDocument();
    });
  });
});
