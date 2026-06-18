import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import TransactionHistogram from "../TransactionHistogram";
import {Transaction} from "../../types";
import TransactionLineChart from "../TransactionLineChart";

class MockResizeObserver {
    constructor(private callback: ResizeObserverCallback) {}
    observe(element: Element) {
        this.callback([
            {
                contentRect: { width: 800, height: 400, top: 0, left: 0, bottom: 0, right: 0, x: 0, y: 0 },
                target: element,
                borderBoxSize: [],
                contentBoxSize: [],
                devicePixelContentBoxSize: []
            }
        ], this);
    }
    unobserve() {}
    disconnect() {}
}

if (typeof window !== 'undefined') {
    window.ResizeObserver = MockResizeObserver;
}

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
    ...overrides,
});

describe('TransactionHistogram', () => {
    it('should display loading state', () => {
        render(<TransactionHistogram transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка транзакций\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<TransactionHistogram transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки транзакций:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<TransactionHistogram transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Транзакций не найдено/i)).toBeInTheDocument();
    });

    it('should group transactions into correct amount ranges', async() => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', amount: 50000
            }),
            createMockTransaction({
                id: 'tx-2', amount: 120000
            }),
            createMockTransaction({
                id: 'tx-3', amount: 80000
            }),
            createMockTransaction({
                id: 'tx-4', amount: 250000
            }),
        ]

        render(<TransactionHistogram transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();
        expect(screen.getByText(/0k – 1k/i)).toBeInTheDocument();
        expect(screen.getByText(/1k – 2k/i)).toBeInTheDocument();
        expect(screen.getByText(/2k – 3k/i)).toBeInTheDocument();
    });

    it('should handle edge case of exact range boundary', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', amount: 100000
            }),
        ];

        render(<TransactionHistogram transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/0k – 1k/i)).not.toBeInTheDocument();
        expect(screen.getByText(/1k – 2k/i)).toBeInTheDocument();
    });

    it('should sort amount ranges in ascending order on X axis', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', amount: 250000
            }),
            createMockTransaction({
                id: 'tx-2', amount: 50000
            }),
            createMockTransaction({
                id: 'tx-3', amount: 150000
            })
        ];

        render(<TransactionHistogram transactions={transactions} loading={false} error={null} />);

        const ticks = screen.getAllByText(/\dk – \dk/i);
        expect(ticks[0]).toHaveTextContent(/0k – 1k/);
        expect(ticks[1]).toHaveTextContent(/1k – 2k/);
        expect(ticks[2]).toHaveTextContent(/2k – 3k/);
    });
})
