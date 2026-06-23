import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TransactionTable } from '../TransactionTable';
import { Transaction, Filter } from '../../types';
import { formatDateTime } from '../../utils/format';
import { fetchApiBlob } from '../../api/client';

vi.mock('../../utils/format', () => ({
    hidePan: vi.fn((pan: string) => `****${pan.slice(-4)}`),
    convertPenniesToRubles: vi.fn((amount: number) => `${(amount / 100).toFixed(2)} ₽`),
    formatTime: vi.fn(() => '10:00:00'),
    formatDate: vi.fn(() => '27.10.2023'),
    formatDateTime: vi.fn(() => '27.10.2023, 10:00:00'),
}));

vi.mock('../../contexts/ToastContext', () => ({
    useToastContext: () => ({
        addToast: vi.fn(),
    }),
}));

vi.mock('../../utils/statusIcon', () => ({
    getStatusIcon: vi.fn(() => ({
        icon: () => <span data-testid="status-icon">Icon</span>,
        color: 'text-green-500',
        size: 16,
        label: 'Approved',
    })),
}));

vi.mock('../../components/TransactionModal', () => ({
    TransactionModal: ({ transaction, onClose }: { transaction: Transaction; onClose: () => void }) => (
        <div data-testid="transaction-modal" onClick={onClose}>Modal for {transaction.id}</div>
    ),
}));

vi.mock('../../api/client', () => ({
    fetchApiBlob: vi.fn(),
}));

vi.mock('../../components/Filters', () => ({
    Filters: ({ onSearch }: { onSearch: (f: Filter) => void }) => (
        <div data-testid="filters">
            <button data-testid="mock-search-btn" onClick={() => onSearch({ status: 'APPROVED' })}>Найти</button>
            <button data-testid="mock-reset-btn" onClick={() => onSearch({})}>Сбросить</button>
        </div>
    ),
}));

vi.mock('../../mockData', () => ({ ISSUERS_NAMES: {}, MCC_NAMES: {} }));

vi.mock('lucide-react', () => ({
    ArrowDownToLine: () => <span data-testid="csv-icon">ArrowDownToLine</span>,
    ChevronLeft: () => <span>←</span>,
    ChevronRight: () => <span>→</span>,
}));

const mockedFormatDateTime = vi.mocked(formatDateTime);
const mockedFetchApiBlob = vi.mocked(fetchApiBlob);

const createMockTransaction = (overrides: Partial<Transaction>): Transaction => ({
    id: 'tx-default',
    mti: '0200',
    stan: '123456',
    rrn: '123',
    pan: '4111111111111111',
    processingCode: '000000',
    processingTimeMs: 100,
    amount: 150000,
    currencyCode: 'RUB',
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

const basePagination = { currentPage: 0, pageSize: 10, totalElements: 1, totalPages: 1 };

describe('TransactionTable', () => {
    const mockSearch = vi.fn();
    const mockOnPageChange = vi.fn();
    const mockOnPageSizeChange = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
        global.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
        global.URL.revokeObjectURL = vi.fn();
        HTMLAnchorElement.prototype.click = vi.fn();
    });

    it('should render loading state correctly', () => {
        render(<TransactionTable transactions={[]} currentFilter={{}} isFiltered={false} error={null} loading={true} search={mockSearch} pagination={basePagination} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);
        expect(screen.getByText('Загрузка транзакций...')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('should render error state correctly', () => {
        render(<TransactionTable transactions={[]} currentFilter={{}} isFiltered={false} error="Network Error" loading={false} search={mockSearch} pagination={basePagination} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);
        expect(screen.getByText('Ошибка загрузки транзакций: Network Error')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('should render empty state when no transactions', () => {
        render(<TransactionTable transactions={[]} currentFilter={{}} isFiltered={false} error={null} loading={false} search={mockSearch} pagination={basePagination} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);
        expect(screen.getByText('Транзакций не найдено')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('should render transactions table and sort by createdAt DESC internally', () => {
        const txOlder = createMockTransaction({ id: 'tx-older', createdAt: '2023-10-26T10:00:00Z', merchantId: 'SHOP-OLD' });
        const txNewer = createMockTransaction({ id: 'tx-newer', createdAt: '2023-10-28T10:00:00Z', merchantId: 'SHOP-NEW' });
        const pagination2 = { ...basePagination, totalElements: 2, totalPages: 1 };

        render(<TransactionTable transactions={[txOlder, txNewer]} currentFilter={{}} isFiltered={false} error={null} loading={false} search={mockSearch} pagination={pagination2} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);

        expect(screen.getByRole('table')).toBeInTheDocument();
        const rows = screen.getAllByRole('row');
        expect(rows).toHaveLength(3);

        expect(rows[1]).toHaveTextContent('SHOP-NEW');
        expect(rows[2]).toHaveTextContent('SHOP-OLD');

        expect(mockedFormatDateTime).toHaveBeenCalledWith('2023-10-28T10:00:00Z');
        expect(mockedFormatDateTime).toHaveBeenCalledWith('2023-10-26T10:00:00Z');
    });

    it('should show pagination controls when totalPages > 1 and data exists', () => {
        const paginationMulti = { ...basePagination, totalElements: 25, totalPages: 3 };
        render(<TransactionTable transactions={[createMockTransaction({ id: '1' })]} currentFilter={{}} isFiltered={false} error={null} loading={false} search={mockSearch} pagination={paginationMulti} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);

        expect(screen.getByRole('button', { name: '«' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '»' })).toBeInTheDocument();
    });

    it('should open and close TransactionModal on row click', () => {
        render(<TransactionTable transactions={[createMockTransaction({ id: 'tx-123' })]} currentFilter={{}} isFiltered={false} error={null} loading={false} search={mockSearch} pagination={basePagination} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);

        fireEvent.click(screen.getByText('****1111').closest('tr')!);
        expect(screen.getByTestId('transaction-modal')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('transaction-modal'));
        expect(screen.queryByTestId('transaction-modal')).not.toBeInTheDocument();
    });

    it('should call search function when Filters trigger it', () => {
        render(<TransactionTable transactions={[]} currentFilter={{}} isFiltered={false} error={null} loading={false} search={mockSearch} pagination={basePagination} onPageChange={mockOnPageChange} onPageSizeChange={mockOnPageSizeChange} />);
        fireEvent.click(screen.getByTestId('mock-search-btn'));
        expect(mockSearch).toHaveBeenCalledWith({ status: 'APPROVED' });
    });

    it('should call fetchApiBlob with correct URL when CSV button is clicked', async () => {
        const tx1 = createMockTransaction({ id: '1', stan: '001', createdAt: '2023-10-26T10:00:00Z' });
        const tx2 = createMockTransaction({ id: '2', stan: '002', createdAt: '2023-10-28T10:00:00Z' });

        const mockBlob = new Blob(['test csv data'], { type: 'text/csv' });
        mockedFetchApiBlob.mockResolvedValue(mockBlob);

        render(
            <TransactionTable
                transactions={[tx1, tx2]}
                currentFilter={{ status: 'APPROVED' }}
                isFiltered={true}
                error={null}
                loading={false}
                search={mockSearch}
                pagination={basePagination}
                onPageChange={mockOnPageChange}
                onPageSizeChange={mockOnPageSizeChange}
            />
        );

        fireEvent.click(screen.getByRole('button', { name: /CSV/i }));

        await waitFor(() => {
            expect(mockedFetchApiBlob).toHaveBeenCalledTimes(1);
        });

        const calledUrl = mockedFetchApiBlob.mock.calls[0][0];
        expect(calledUrl).toContain('/api/transactions/export');
        expect(calledUrl).toContain('status=APPROVED');
    });
});
