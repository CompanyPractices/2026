import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TransactionTable } from '../components/TransactionTable';
import { Transaction, Filter } from '../types';
import { hidePan, convertPenniesToRubles, formatTime } from '../utils/format';
import { exportToCsv } from '../utils/exportToCsv';

vi.mock('../utils/format', () => ({
    hidePan: vi.fn((pan: string) => `****${pan.slice(-4)}`),
    convertPenniesToRubles: vi.fn((amount: number) => `${(amount / 100).toFixed(2)} ₽`),
    formatTime: vi.fn(() => '10:00:00'),
}));

vi.mock('../utils/statusIcon', () => ({
    getStatusIcon: vi.fn(() => ({
        icon: () => <span data-testid="status-icon">Icon</span>,
        color: 'text-green-500',
        size: 16,
        label: 'Approved',
    })),
}));

vi.mock('../utils/exportToCsv', () => ({
    exportToCsv: vi.fn(),
}));

vi.mock('../components/TransactionModal', () => ({
    TransactionModal: ({ transaction, onClose }: { transaction: Transaction; onClose: () => void }) => (
        <div data-testid="transaction-modal" onClick={onClose}>
            Modal for {transaction.id}
        </div>
    ),
}));

vi.mock('../components/Filters', () => {
    const React = require('react');
    const { useState } = React;

    return {
        Filters: ({ onSearch }: { onSearch: (f: Filter) => void }) => {
            const [mockStatus, setMockStatus] = useState('');

            return React.createElement('div', { 'data-testid': 'filters' }, [
                React.createElement('select', {
                    key: 'select',
                    'data-testid': 'mock-status-select',
                    value: mockStatus,
                    onChange: (e: React.ChangeEvent<HTMLSelectElement>) => setMockStatus(e.target.value)
                }, [
                    React.createElement('option', { key: 'all', value: '' }, 'Все'),
                    React.createElement('option', { key: 'approved', value: 'APPROVED' }, 'Одобрен'),
                ]),
                React.createElement('button', {
                    key: 'search',
                    'data-testid': 'mock-search-btn',
                    onClick: () => onSearch(mockStatus ? { status: mockStatus as Filter['status'] } : {})
                }, 'Найти'),
                React.createElement('button', {
                    key: 'reset',
                    'data-testid': 'mock-reset-btn',
                    onClick: () => {
                        setMockStatus('');
                        onSearch({});
                    }
                }, 'Сбросить')
            ]);
        },
    };
});

vi.mock('../mockData', () => ({
    ISSUERS_NAMES: ['Issuer A', 'Issuer B'],
    MCC_NAMES: ['MCC 1', 'MCC 2'],
}));

vi.mock('lucide-react', () => ({
    ArrowDownToLine: () => <span data-testid="csv-icon">ArrowDownToLine</span>,
}));

const mockedHidePan = vi.mocked(hidePan);
const mockedConvertPennies = vi.mocked(convertPenniesToRubles);
const mockedFormatTime = vi.mocked(formatTime);
const mockedExportToCsv = vi.mocked(exportToCsv);

const createMockTransaction = (overrides: Partial<Transaction>): Transaction => ({
    id: 'tx-default',
    mti: '0200',
    stan: '123456',
    pan: '4111111111111111',
    processingCode: '000000',
    processingTimeMs: 100,
    amount: 150000,
    currencyCode: 'RUB',
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

describe('TransactionTable', () => {
    const mockSearch = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render loading state correctly', () => {
        render(
            <TransactionTable
                liveTransactions={[]}
                error={null}
                loading={true}
                search={mockSearch}
            />
        );

        expect(screen.getByText('Загрузка транзакций...')).toBeInTheDocument();
        expect(screen.queryByText('Последние 20 транзакций')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('should render error state correctly', () => {
        render(
            <TransactionTable
                liveTransactions={[]}
                error="Network Error"
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByText('Ошибка загрузки транзакций: Network Error')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('should render empty state when no transactions, no error, and not loading', () => {
        render(
            <TransactionTable
                liveTransactions={[]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByText('Транзакций не найдено')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();

        const exportBtn = screen.getByRole('button', { name: /CSV/i });
        expect(exportBtn).toBeDisabled();
    });

    it('should render transactions table with correct data and call utility functions', () => {
        const tx1 = createMockTransaction({ id: 'tx-1', pan: '4111111111111111', amount: 150000, merchantId: 'SHOP-1' });
        const tx2 = createMockTransaction({ id: 'tx-2', pan: '5555555555554444', amount: 250000, status: 'DECLINED', merchantId: 'SHOP-2' });

        render(
            <TransactionTable
                liveTransactions={[tx1, tx2]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByRole('table')).toBeInTheDocument();

        expect(mockedHidePan).toHaveBeenCalledWith('4111111111111111');
        expect(mockedHidePan).toHaveBeenCalledWith('5555555555554444');

        expect(mockedConvertPennies).toHaveBeenCalledWith(150000);
        expect(mockedConvertPennies).toHaveBeenCalledWith(250000);

        expect(mockedFormatTime).toHaveBeenCalledWith('2023-10-27T10:00:00Z');

        expect(screen.getByText('****1111')).toBeInTheDocument();
        expect(screen.getByText('1500.00 ₽')).toBeInTheDocument();
        expect(screen.getByText('SHOP-1')).toBeInTheDocument();
        expect(screen.getAllByTestId('status-icon')).toHaveLength(2);
    });

    it('should open TransactionModal when a row is clicked', () => {
        const tx1 = createMockTransaction({ id: 'tx-123' });

        render(
            <TransactionTable
                liveTransactions={[tx1]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        const row = screen.getByText('****1111').closest('tr');
        expect(row).toBeInTheDocument();

        fireEvent.click(row!);

        expect(screen.getByTestId('transaction-modal')).toBeInTheDocument();
        expect(screen.getByText('Modal for tx-123')).toBeInTheDocument();
    });

    it('should close TransactionModal when onClose is triggered', () => {
        const tx1 = createMockTransaction({ id: 'tx-123' });

        render(
            <TransactionTable
                liveTransactions={[tx1]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        fireEvent.click(screen.getByText('****1111').closest('tr')!);
        expect(screen.getByTestId('transaction-modal')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('transaction-modal'));

        expect(screen.queryByTestId('transaction-modal')).not.toBeInTheDocument();
    });

    it('should call search function when Filters trigger it', () => {
        render(
            <TransactionTable
                liveTransactions={[]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        const statusSelect = screen.getByTestId('mock-status-select');
        fireEvent.change(statusSelect, { target: { value: 'APPROVED' } });

        const searchBtn = screen.getByTestId('mock-search-btn');
        fireEvent.click(searchBtn);

        expect(mockSearch).toHaveBeenCalledTimes(1);
        expect(mockSearch).toHaveBeenCalledWith({ status: 'APPROVED' });
    });

    it('should handle CSV export correctly with valid transactions', () => {
        const tx1 = createMockTransaction({
            id: 'tx-1',
            stan: '000001',
            rrn: '123456789012',
            authCode: 'A1B2C3',
            terminalType: 'POS',
            issuerId: 'ISS-1'
        });

        render(
            <TransactionTable
                liveTransactions={[tx1]}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        const exportBtn = screen.getByRole('button', { name: /CSV/i });
        expect(exportBtn).not.toBeDisabled();

        fireEvent.click(exportBtn);

        expect(mockedExportToCsv).toHaveBeenCalledTimes(1);

        const calledFilename = mockedExportToCsv.mock.calls[0][0];
        expect(calledFilename).toMatch(/^transactions_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.csv$/);

        const calledData = mockedExportToCsv.mock.calls[0][1];
        expect(calledData).toHaveLength(1);
        expect(calledData[0]).toEqual({
            'STAN': '000001',
            'RRN': '123456789012',
            'PAN': '****1111',
            'Amount': 150000,
            'Status': 'APPROVED',
            'Auth code': 'A1B2C3',
            'Terminal': 'T123 (POS)',
            'Merchant ID': 'M123',
            'MCC': '5411',
            'Acquirer ID': 'A123',
            'Issuer ID': 'ISS-1',
            'Time': '10:00:00',
        });
    });

    it('should handle full user flow: live data -> filter search -> reset -> back to live data', () => {
        const initialLiveTxs = [
            createMockTransaction({ id: 'live-1', amount: 10000, status: 'APPROVED' }),
            createMockTransaction({ id: 'live-2', amount: 5000, status: 'DECLINED' })
        ];

        const { rerender } = render(
            <TransactionTable
                liveTransactions={initialLiveTxs}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByText('100.00 ₽')).toBeInTheDocument();
        expect(screen.getByText('50.00 ₽')).toBeInTheDocument();

        const statusSelect = screen.getByTestId('mock-status-select');
        fireEvent.change(statusSelect, { target: { value: 'APPROVED' } });

        const searchBtn = screen.getByTestId('mock-search-btn');
        fireEvent.click(searchBtn);

        expect(mockSearch).toHaveBeenCalledTimes(1);
        expect(mockSearch).toHaveBeenCalledWith({ status: 'APPROVED' });

        const filteredTxs = [
            createMockTransaction({ id: 'history-1', amount: 99900, status: 'APPROVED' })
        ];

        rerender(
            <TransactionTable
                liveTransactions={filteredTxs}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByText('999.00 ₽')).toBeInTheDocument();
        expect(screen.queryByText('100.00 ₽')).not.toBeInTheDocument();

        const resetBtn = screen.getByTestId('mock-reset-btn');
        fireEvent.click(resetBtn);

        expect(mockSearch).toHaveBeenCalledTimes(2);
        expect(mockSearch).toHaveBeenLastCalledWith({});

        const newLiveTxs = [
            createMockTransaction({ id: 'live-3', amount: 15000, status: 'APPROVED' })
        ];

        rerender(
            <TransactionTable
                liveTransactions={newLiveTxs}
                error={null}
                loading={false}
                search={mockSearch}
            />
        );

        expect(screen.getByText('150.00 ₽')).toBeInTheDocument();
        expect(screen.queryByText('999.00 ₽')).not.toBeInTheDocument();
    });
});
