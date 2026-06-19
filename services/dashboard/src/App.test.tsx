import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';
import {Transaction} from "./types";

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

const mockUseLiveStats = vi.hoisted(() => vi.fn());
const mockUseWebSocket = vi.hoisted(() => vi.fn());
const mockUseTransactions = vi.hoisted(() => vi.fn());

const MOCK_LIVE_STATS_SUCCESS = {
  stats: {
    totalTransactions: 100,
    approvedTransactions: 80,
    totalAmount: 500000,
    totalProcessingTimeMs: 5000,
  },
  loading: false,
  error: null,
};

const MOCK_LIVE_STATS_ERROR = {
  stats: null,
  loading: false,
  error: 'Network error',
};

const createMockTransaction = (overrides: Partial<Transaction>): Transaction => ({
  id: 'tx-default',
  mti: '0200',
  stan: '123456',
  pan: '4111111111111111',
  processingCode: '000000',
  processingTimeMs: 100,
  amount: 1000,
  currencyCode: 'USD',
  terminalId: 'T123',
  responseCode: '00',
  merchantId: 'M123',
  mcc: '5411',
  acquirerId: 'A123',
  status: 'APPROVED',
  transmissionDateTime: '2023-10-27T10:00:00Z',
  createdAt: '2023-10-27T10:00:00Z',
  ...overrides,
});

vi.mock('./hooks/useTransactions.ts', () => ({
  default: mockUseTransactions
}));

vi.mock('./hooks/useLiveStats', () => ({
  useLiveStats: mockUseLiveStats,
}));

vi.mock('./hooks/useWebSocket', () => ({
  useWebSocket: mockUseWebSocket,
}));

describe('App', () => {
  beforeEach(() => {
    MockWebSocket.instances = [];
    global.WebSocket = MockWebSocket as unknown as typeof WebSocket;

    vi.clearAllMocks();

    mockUseTransactions.mockReturnValue({
      transactions: [],
      filteredTransactions: [],
      isFiltered: false,
      loading: false,
      error: null,
      searchTransactions: vi.fn(),
    });

    mockUseWebSocket.mockReturnValue({
      liveTransactions: [],
      isConnected: false,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    } as unknown as typeof ResizeObserver;
  });

  afterAll(() => {
    Reflect.deleteProperty(global, 'ResizeObserver');
  });

  it('renders dashboard title', async () => {
    mockUseLiveStats.mockReturnValue(MOCK_LIVE_STATS_SUCCESS);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });
  });

  it('renders footer with all three texts', async () => {
    mockUseLiveStats.mockReturnValue(MOCK_LIVE_STATS_ERROR);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Практика')).toBeInTheDocument();
      expect(screen.getByText('СМП - Система медленных платежей')).toBeInTheDocument();
      expect(screen.getByText('2026')).toBeInTheDocument();
    });
  });

  it('has correct structure', async () => {
    mockUseLiveStats.mockReturnValue(MOCK_LIVE_STATS_SUCCESS);

    render(<App />);

    await waitFor(() => {
      expect(document.querySelector('header')).toBeInTheDocument();
      expect(document.querySelector('main')).toBeInTheDocument();
      expect(document.querySelector('footer')).toBeInTheDocument();
    });
  });

  it('shows loading then error for stats', async () => {
    mockUseLiveStats.mockReturnValue(MOCK_LIVE_STATS_ERROR);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/Ошибка загрузки статистики:/)).toBeInTheDocument();
    });
  });

  it('after reset filters table shows live data', async() => {
    mockUseLiveStats.mockReturnValue(MOCK_LIVE_STATS_SUCCESS);

    const wsTransaction = createMockTransaction({id: 'ws-1', merchantId: 'WS_MERCHANT'});
    const searchTransaction = createMockTransaction({id: 'search-1', merchantId: 'SEARCH_MERCHANT'});

    mockUseTransactions.mockReturnValue({
      transactions: [createMockTransaction({id: 'api-1'})],
      filteredTransactions: [searchTransaction],
      isFiltered: true,
      loading: false,
      error: null,
      searchTransactions: vi.fn(),
    });
    mockUseWebSocket.mockReturnValue({
      liveTransactions: [wsTransaction],
      isConnected: true,
    });

    const { rerender } = render(<App />);
    await waitFor(() => {
      expect(screen.getByText('SEARCH_MERCHANT')).toBeInTheDocument();
      expect(screen.queryByText('WS_MERCHANT')).not.toBeInTheDocument();
    })

    mockUseTransactions.mockReturnValue({
      transactions: [createMockTransaction({id: 'api-1'})],
      filteredTransactions: [searchTransaction],
      isFiltered: false,
      loading: false,
      error: null,
      searchTransactions: vi.fn(),
    });

    rerender(<App />);
    await waitFor(() => {
      expect(screen.getByText('WS_MERCHANT')).toBeInTheDocument();
      expect(screen.queryByText('SEARCH_MERCHANT')).not.toBeInTheDocument();
    })
  })
});
