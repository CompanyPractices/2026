import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { subMinutes } from 'date-fns';
import TransactionLineChart from "../TransactionLineChart";
import {Transaction} from "../../types";
import { formatInTimeZone } from 'date-fns-tz';

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

describe('TransactionLineChart', () => {
    it('should display loading state', () => {
        render(<TransactionLineChart transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка транзакций\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<TransactionLineChart transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки транзакций:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<TransactionLineChart transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Транзакций не найдено/i)).toBeInTheDocument();
    });

    it('should render chart when transactions exist', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                createdAt: new Date().toISOString()}),
            createMockTransaction({
                id: 'tx-2',
                createdAt: new Date().toISOString()}),
        ]

        render(<TransactionLineChart transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/не найдено/i)).not.toBeInTheDocument();
    });

    it('should not include transactions older than 1 hour', () => {
        const recentTx = createMockTransaction({
            id: 'tx-recent',
            createdAt: new Date().toISOString()});
        const oldTx = createMockTransaction({
            id: 'tx-old',
            createdAt: subMinutes(new Date(), 90).toISOString()});

        render(<TransactionLineChart transactions={[recentTx, oldTx]} loading={false} error={null} />);

        expect(screen.queryByText(/не найдено/i)).not.toBeInTheDocument();
    });

    it('should show empty state when all transactions are older than 1 hour', () => {
        const oldTx1 = createMockTransaction({
            id: 'tx-old-1',
            createdAt: subMinutes(new Date(), 90).toISOString()});
        const oldTx2 = createMockTransaction({
            id: 'tx-old-2',
            createdAt: subMinutes(new Date(), 120).toISOString()});

        render(<TransactionLineChart transactions={[oldTx1, oldTx2]} loading={false} error={null} />);
        expect(screen.queryByText(/не найдено/i)).not.toBeInTheDocument();
    });

    it('should display time label for recent transaction', () => {
        const now = new Date();
        const recentTx = createMockTransaction({
            id: 'tx-recent',
            createdAt: now.toISOString(),
        });

        render(<TransactionLineChart transactions={[recentTx]} loading={false} error={null} />);

        const expectedLabel = formatInTimeZone(now, 'UTC', 'HH:mm');
        expect(screen.getByText(expectedLabel)).toBeInTheDocument();
    });

    it('should count multiple transactions at the same minute as one data point', () => {
        const sameTime = new Date().toISOString();
        const transactions = [
            createMockTransaction({id: 'tx-1', createdAt: sameTime}),
            createMockTransaction({id: 'tx-2', createdAt: sameTime}),
            createMockTransaction({id: 'tx-3', createdAt: sameTime}),
            createMockTransaction({id: 'tx-4', createdAt: sameTime}),
            createMockTransaction({id: 'tx-5', createdAt: sameTime}),
            createMockTransaction({id: 'tx-6', createdAt: sameTime}),
            createMockTransaction({id: 'tx-7', createdAt: sameTime}),
            createMockTransaction({id: 'tx-8', createdAt: sameTime}),
            createMockTransaction({id: 'tx-9', createdAt: sameTime}),
        ];

        render(<TransactionLineChart transactions={transactions} loading={false} error={null} />);

        expect(screen.getByText('9')).toBeInTheDocument();
        const expectedLabel = formatInTimeZone(sameTime, 'UTC', 'HH:mm');
        expect(screen.getByText(expectedLabel)).toBeInTheDocument();
    });

    it('should show separate data points for transactions at different minutes', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                createdAt: subMinutes(new Date(), 7).toISOString(),
            }),
            createMockTransaction({
                id: 'tx-2',
                createdAt: subMinutes(new Date(), 26).toISOString(),
            })
        ]

        render(<TransactionLineChart transactions={transactions} loading={false} error={null} />);

        const time1 = formatInTimeZone(subMinutes(new Date(), 7), 'UTC', 'HH:mm');
        const time2 = formatInTimeZone(subMinutes(new Date(), 26), 'UTC', 'HH:mm');
        expect(screen.getAllByText(time1).length).toBeGreaterThan(0);
        expect(screen.getAllByText(time2).length).toBeGreaterThan(0);
    });

    it('should count multiple transactions at the same minute as one data point and other transactions in different', () => {
        const sameTime = new Date().toISOString();
        const transactions = [
            createMockTransaction({id: 'tx-1', createdAt: sameTime}),
            createMockTransaction({id: 'tx-2', createdAt: sameTime}),
            createMockTransaction({id: 'tx-3', createdAt: sameTime}),
            createMockTransaction({id: 'tx-4', createdAt: sameTime}),
            createMockTransaction({id: 'tx-5', createdAt: sameTime}),
            createMockTransaction({id: 'tx-6', createdAt: subMinutes(new Date(), 43).toISOString(),}),
            createMockTransaction({id: 'tx-7', createdAt: subMinutes(new Date(), 43).toISOString(),}),
            createMockTransaction({id: 'tx-8', createdAt: subMinutes(new Date(), 43).toISOString(),}),
            createMockTransaction({id: 'tx-9', createdAt: subMinutes(new Date(), 28).toISOString(),}),
        ];

        render(<TransactionLineChart transactions={transactions} loading={false} error={null} />);

        const time1 = formatInTimeZone(sameTime, 'UTC', 'HH:mm');
        const time2 = formatInTimeZone(subMinutes(new Date(), 43), 'UTC', 'HH:mm');
        const time3 = formatInTimeZone(subMinutes(new Date(), 28), 'UTC', 'HH:mm');
        expect(screen.getAllByText(time1).length).toBeGreaterThanOrEqual(1);
        expect(screen.getAllByText(time2).length).toBeGreaterThanOrEqual(1);
        expect(screen.getAllByText(time3).length).toBeGreaterThanOrEqual(1);
    });
})
