import { describe, it, expect, vi, beforeEach, beforeAll, afterAll } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';
import '@testing-library/jest-dom';
import { Transaction } from "./types";
import type { PaginationMeta } from "./types";

type HeaderProps = {
  stats: { totalTransactions: number } | null;
  loading: boolean;
  error: string | null;
  isConnected: boolean;
};

type ChartProps = { transactions: Transaction[] };

type TableProps = {
  transactions: Transaction[];
  isFiltered: boolean;
  pagination: PaginationMeta;
};

type MapProps = { transactions: Transaction[] };

vi.mock('./components/ToastContainer.tsx', () => ({
  ToastContainer: () => null,
}));

vi.mock('./components/Header.tsx', () => ({
  Header: ({ stats, loading, error, isConnected }: HeaderProps) => (
      <header data-testid="header" data-connected={String(isConnected)}>
        {loading && <span>Stats loading...</span>}
        {error && <span>Stats error: {error}</span>}
        {stats && <span data-testid="stats-total">Total: {stats.totalTransactions}</span>}
      </header>
  ),
}));

vi.mock('./components/TransactionHistogram.tsx', () => ({
  default: ({ transactions }: ChartProps) => (
      <div data-testid="histogram">
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`histogram-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

vi.mock('./components/TransactionPieChart.tsx', () => ({
  default: ({ transactions }: ChartProps) => (
      <div data-testid="pie">
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`pie-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

vi.mock('./components/TransactionLineChart.tsx', () => ({
  default: ({ transactions }: ChartProps) => (
      <div data-testid="line">
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`line-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

vi.mock('./components/DeclineReasonChart.tsx', () => ({
  default: ({ transactions }: ChartProps) => (
      <div data-testid="decline">
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`decline-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

vi.mock('./components/TransactionTable.tsx', () => ({
  TransactionTable: ({ transactions, isFiltered, pagination }: TableProps) => (
      <div data-testid="table" data-filtered={String(isFiltered)}>
        <span data-testid="table-page">{pagination.currentPage}</span>
        <span data-testid="table-pagesize">{pagination.pageSize}</span>
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`table-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

vi.mock('./components/TransactionsMap.tsx', () => ({
  TransactionMap: ({ transactions }: MapProps) => (
      <div data-testid="map">
        {(transactions || []).map((t) => (
            <span key={t.id} data-testid={`map-${t.id}`}>{t.merchantId}</span>
        ))}
      </div>
  ),
}));

const mockUseLiveStats = vi.hoisted(() => vi.fn());
const mockUseWebSocket = vi.hoisted(() => vi.fn());
const mockUseTransactions = vi.hoisted(() => vi.fn());
const mockUseChartTransactions = vi.hoisted(() => vi.fn());

vi.mock('./hooks/useTransactions.ts', () => ({ default: mockUseTransactions }));
vi.mock('./hooks/useLiveStats.ts', () => ({ useLiveStats: mockUseLiveStats }));
vi.mock('./hooks/useWebSocket.ts', () => ({ useWebSocket: mockUseWebSocket }));
vi.mock('./hooks/useChartTransactions.ts', () => ({ default: mockUseChartTransactions }));

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
  terminalType: 'POS',
  responseCode: '00',
  merchantId: 'M123',
  mcc: '5411',
  acquirerId: 'A123',
  issuerId: 'ISS001',
  status: 'APPROVED',
  transmissionDateTime: '2023-10-27T10:00:00Z',
  createdAt: '2023-10-27T10:00:00Z',
  ...overrides,
});

const defaultPagination: PaginationMeta = {
  currentPage: 0,
  pageSize: 20,
  totalElements: 0,
  totalPages: 1,
};

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

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockUseTransactions.mockReturnValue({
      transactions: [],
      isFiltered: false,
      currentFilter: {},
      loading: false,
      error: null,
      applyFilter: vi.fn(),
      goToPage: vi.fn(),
      changePageSize: vi.fn(),
      pagination: defaultPagination,
    });

    mockUseWebSocket.mockReturnValue({
      liveTransactions: [],
      isConnected: false,
    });

    mockUseLiveStats.mockReturnValue({
      stats: null,
      loading: false,
      error: null,
    });

    mockUseChartTransactions.mockReturnValue({
      transactions: [],
      loading: false,
      error: null,
      period: 'day',
      setPeriod: vi.fn(),
      refresh: vi.fn(),
    });
  });

  it('renders footer with all three texts', () => {
    render(<App />);
    expect(screen.getByText('Практика')).toBeInTheDocument();
    expect(screen.getByText('СМП - Система медленных платежей')).toBeInTheDocument();
    expect(screen.getByText('2026')).toBeInTheDocument();
  });

  it('has correct structure (header, main, footer)', () => {
    render(<App />);
    expect(document.querySelector('header')).toBeInTheDocument();
    expect(document.querySelector('main')).toBeInTheDocument();
    expect(document.querySelector('footer')).toBeInTheDocument();
  });

  it('shows stats error when useLiveStats returns error', () => {
    mockUseLiveStats.mockReturnValue({
      stats: null,
      loading: false,
      error: 'Network error',
    });

    render(<App />);
    expect(screen.getByText('Stats error: Network error')).toBeInTheDocument();
  });

  it('renders all chart components and table with data', () => {
    const chartTx = createMockTransaction({ id: 'chart-1', merchantId: 'CHART_MERCHANT' });
    const tableTx = createMockTransaction({ id: 'table-1', merchantId: 'TABLE_MERCHANT' });

    mockUseChartTransactions.mockReturnValue({
      transactions: [chartTx],
      loading: false,
      error: null,
      period: 'day',
      setPeriod: vi.fn(),
      refresh: vi.fn(),
    });

    mockUseTransactions.mockReturnValue({
      transactions: [tableTx],
      isFiltered: false,
      currentFilter: {},
      loading: false,
      error: null,
      applyFilter: vi.fn(),
      goToPage: vi.fn(),
      changePageSize: vi.fn(),
      pagination: { ...defaultPagination, totalElements: 1, totalPages: 1 },
    });

    render(<App />);

    expect(screen.getByTestId('histogram')).toBeInTheDocument();
    expect(screen.getByTestId('pie')).toBeInTheDocument();
    expect(screen.getByTestId('line')).toBeInTheDocument();
    expect(screen.getByTestId('decline')).toBeInTheDocument();
    expect(screen.getByTestId('histogram-chart-1')).toBeInTheDocument();
    expect(screen.getByTestId('pie-chart-1')).toBeInTheDocument();
    expect(screen.getByTestId('line-chart-1')).toBeInTheDocument();
    expect(screen.getByTestId('decline-chart-1')).toBeInTheDocument();

    expect(screen.getByTestId('table')).toBeInTheDocument();
    expect(screen.getByTestId('map')).toBeInTheDocument();
    expect(screen.getByTestId('table-table-1')).toBeInTheDocument();
    expect(screen.getByTestId('map-table-1')).toBeInTheDocument();
  });

  describe('live data mixing into pageTransactions', () => {
    it('on page 0 without filters: live transactions are mixed into table (deduplicated)', () => {
      const liveTx = createMockTransaction({ id: 'live-1', merchantId: 'LIVE_MERCHANT' });
      const searchTx = createMockTransaction({ id: 'search-1', merchantId: 'SEARCH_MERCHANT' });

      mockUseTransactions.mockReturnValue({
        transactions: [searchTx],
        isFiltered: false,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 0, pageSize: 20 },
      });
      mockUseWebSocket.mockReturnValue({
        liveTransactions: [liveTx],
        isConnected: true,
      });

      render(<App />);

      expect(screen.getByTestId('table-live-1')).toBeInTheDocument();
      expect(screen.getByTestId('table-search-1')).toBeInTheDocument();
      expect(screen.getByTestId('map-live-1')).toBeInTheDocument();
      expect(screen.getByTestId('map-search-1')).toBeInTheDocument();
    });

    it('on page 0 without filters: deduplicates transactions present in both live and search', () => {
      const tx = createMockTransaction({ id: 'same-id', merchantId: 'SAME' });

      mockUseTransactions.mockReturnValue({
        transactions: [tx],
        isFiltered: false,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 0, pageSize: 20 },
      });
      mockUseWebSocket.mockReturnValue({
        liveTransactions: [tx],
        isConnected: true,
      });

      render(<App />);

      const tableItems = screen.getAllByTestId('table-same-id');
      expect(tableItems).toHaveLength(1);
    });

    it('on page 0 without filters: limits pageTransactions to pageSize', () => {
      const liveTxs = Array.from({ length: 15 }, (_, i) =>
          createMockTransaction({ id: `live-${i}`, merchantId: `LIVE_${i}` })
      );
      const searchTxs = Array.from({ length: 15 }, (_, i) =>
          createMockTransaction({ id: `search-${i}`, merchantId: `SEARCH_${i}` })
      );

      mockUseTransactions.mockReturnValue({
        transactions: searchTxs,
        isFiltered: false,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 0, pageSize: 20 },
      });
      mockUseWebSocket.mockReturnValue({
        liveTransactions: liveTxs,
        isConnected: true,
      });

      render(<App />);

      const tableItems = screen
          .getByTestId('table')
          .querySelectorAll('span[data-testid^="table-"]:not([data-testid="table-page"]):not([data-testid="table-pagesize"])');
      expect(tableItems).toHaveLength(20);
    });

    it('on page > 0: live transactions are NOT mixed into table', () => {
      const liveTx = createMockTransaction({ id: 'live-1', merchantId: 'LIVE_MERCHANT' });
      const searchTx = createMockTransaction({ id: 'search-1', merchantId: 'SEARCH_MERCHANT' });

      mockUseTransactions.mockReturnValue({
        transactions: [searchTx],
        isFiltered: false,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 2, pageSize: 20 },
      });
      mockUseWebSocket.mockReturnValue({
        liveTransactions: [liveTx],
        isConnected: true,
      });

      render(<App />);

      expect(screen.getByTestId('table-search-1')).toBeInTheDocument();
      expect(screen.queryByTestId('table-live-1')).not.toBeInTheDocument();
    });

    it('when isFiltered=true: live transactions are NOT mixed into table', () => {
      const liveTx = createMockTransaction({ id: 'live-1', merchantId: 'LIVE_MERCHANT' });
      const searchTx = createMockTransaction({ id: 'search-1', merchantId: 'SEARCH_MERCHANT' });

      mockUseTransactions.mockReturnValue({
        transactions: [searchTx],
        isFiltered: true,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 0, pageSize: 20 },
      });
      mockUseWebSocket.mockReturnValue({
        liveTransactions: [liveTx],
        isConnected: true,
      });

      render(<App />);

      expect(screen.getByTestId('table-search-1')).toBeInTheDocument();
      expect(screen.queryByTestId('table-live-1')).not.toBeInTheDocument();
      expect(screen.getByTestId('table')).toHaveAttribute('data-filtered', 'true');
    });
  });

  describe('chartTransactions comes from useChartTransactions', () => {
    it('charts receive data from useChartTransactions, not from useTransactions', () => {
      const chartTx = createMockTransaction({ id: 'chart-only', merchantId: 'CHART' });
      const tableTx = createMockTransaction({ id: 'table-only', merchantId: 'TABLE' });

      mockUseChartTransactions.mockReturnValue({
        transactions: [chartTx],
        loading: false,
        error: null,
        period: 'day',
        setPeriod: vi.fn(),
        refresh: vi.fn(),
      });

      mockUseTransactions.mockReturnValue({
        transactions: [tableTx],
        isFiltered: true,
        currentFilter: {},
        loading: false,
        error: null,
        applyFilter: vi.fn(),
        goToPage: vi.fn(),
        changePageSize: vi.fn(),
        pagination: { ...defaultPagination, currentPage: 5, pageSize: 20 },
      });

      render(<App />);

      expect(screen.getByTestId('histogram-chart-only')).toBeInTheDocument();
      expect(screen.getByTestId('pie-chart-only')).toBeInTheDocument();
      expect(screen.getByTestId('line-chart-only')).toBeInTheDocument();
      expect(screen.getByTestId('decline-chart-only')).toBeInTheDocument();

      expect(screen.queryByTestId('histogram-table-only')).not.toBeInTheDocument();
      expect(screen.queryByTestId('pie-table-only')).not.toBeInTheDocument();

      expect(screen.getByTestId('table-table-only')).toBeInTheDocument();
      expect(screen.queryByTestId('table-chart-only')).not.toBeInTheDocument();
    });
  });
});
