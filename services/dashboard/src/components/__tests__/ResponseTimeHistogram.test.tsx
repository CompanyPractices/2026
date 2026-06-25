import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ResponseTimeHistogram from "../ResponseTimeHistogram";
import {Transaction} from "../../types";

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

describe('ResponseTimeHistogram', () => {
    it('should display loading state', () => {
        render(<ResponseTimeHistogram transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка данных\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<ResponseTimeHistogram transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки данных:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<ResponseTimeHistogram transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Данных не найдено/i)).toBeInTheDocument();
    });

    it('should group transactions into correct time ms ranges', async() => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', processingTimeMs: 49
            }),
            createMockTransaction({
                id: 'tx-2', processingTimeMs: 72
            }),
            createMockTransaction({
                id: 'tx-3', processingTimeMs: 156
            }),
            createMockTransaction({
                id: 'tx-4', processingTimeMs: 3
            }),
            createMockTransaction({
                id: 'tx-4', processingTimeMs: 101
            }),
        ]

        render(<ResponseTimeHistogram transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();
        expect(screen.getByText(/0 – 50ms/i)).toBeInTheDocument();
        expect(screen.getByText(/50 – 100ms/i)).toBeInTheDocument();
        expect(screen.getByText(/150 – 200ms/i)).toBeInTheDocument();
        expect(screen.getByText(/100 – 150ms/i)).toBeInTheDocument();
    });

    it('should handle edge case of exact range boundary', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', processingTimeMs: 300
            }),
        ];

        render(<ResponseTimeHistogram transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/250 – 300ms/i)).not.toBeInTheDocument();
        expect(screen.getByText(/300 – 350ms/i)).toBeInTheDocument();
    });

    it('should sort processing time ranges in ascending order on X axis', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1', processingTimeMs: 158
            }),
            createMockTransaction({
                id: 'tx-2', processingTimeMs: 228
            }),
            createMockTransaction({
                id: 'tx-3', processingTimeMs: 67
            })
        ];

        render(<ResponseTimeHistogram transactions={transactions} loading={false} error={null} />);

        const ticks = screen.getAllByText(/\d\s+\S+\s+\d+ms/i);
        expect(ticks[0]).toHaveTextContent(/50 – 100ms/);
        expect(ticks[1]).toHaveTextContent(/150 – 200ms/);
        expect(ticks[2]).toHaveTextContent(/200 – 250ms/);
    });
})
