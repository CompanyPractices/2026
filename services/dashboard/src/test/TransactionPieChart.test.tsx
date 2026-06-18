import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import TransactionPieChart from '../components/TransactionPieChart';
import {Transaction} from "../types";
import * as React from 'react';

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
    createdAt: new Date().toISOString(),
    ...overrides,
});

describe('TransactionPieChart', () => {
    it('should display loading state', () => {
        render(<TransactionPieChart transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка транзакций\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<TransactionPieChart transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки транзакций:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<TransactionPieChart transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Транзакций не найдено/i)).toBeInTheDocument();
    });

    it('should render chart and legend when transactions exist', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                status: 'APPROVED' }),
            createMockTransaction({
                id: 'tx-2',
                status: 'DECLINED' }),
        ];

        render(<TransactionPieChart transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();

        expect(screen.getByText(/Одобрено\(1\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Отклонено\(1\)/i)).toBeInTheDocument();
    });

    it('should correctly count approved vs declined transactions', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                status: 'APPROVED'
            }),
            createMockTransaction({
                id: 'tx-2',
                status: 'APPROVED'
            }),
            createMockTransaction({
                id: 'tx-3',
                status: 'APPROVED',
            }),
            createMockTransaction({
                id: 'tx-4',
                status: 'DECLINED'
            }),
        ];

        render(<TransactionPieChart transactions={transactions} loading={false} error={null} />);
        expect(screen.getByText(/Одобрено\(3\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Отклонено\(1\)/i)).toBeInTheDocument();
    });

    it('should show zero for missing status in legend', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                status: 'APPROVED'
            }),
            createMockTransaction({
                id: 'tx-2',
                status: 'APPROVED'
            }),
            createMockTransaction({
                id: 'tx-3',
                status: 'APPROVED',
            }),
        ];

        render(<TransactionPieChart transactions={transactions} loading={false} error={null} />);
        expect(screen.getByText(/Одобрено\(3\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Отклонено\(0\)/i)).toBeInTheDocument();
    });
})
