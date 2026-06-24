import {Transaction} from "../../types";
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import DeclineReasonChart from '../DeclineReasonChart';

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
    status: 'DECLINED',
    transmissionDateTime: '2023-10-27T10:00:00Z',
    createdAt: new Date().toISOString(),
    declineReason: undefined,
    ...overrides,
});

describe('DeclineReasonChart', () => {
    it('should display loading state', () => {
        render(<DeclineReasonChart transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка данных об отказах\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<DeclineReasonChart transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки данных об отказах:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<DeclineReasonChart transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Нет данных об отказах/i)).toBeInTheDocument();
    });

    it('should render chart and legend when transactions exist', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                declineReason: 'Первая причина' }),
            createMockTransaction({
                id: 'tx-2',
                declineReason: 'Вторая причина' }),
            createMockTransaction({
                id: 'tx-3',
                declineReason: 'Третья причина' }),
        ];

        render(<DeclineReasonChart transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();

        expect(screen.getByText(/Первая причина/i)).toBeInTheDocument();
        expect(screen.getByText(/Вторая причина/i)).toBeInTheDocument();
        expect(screen.getByText(/Третья причина/i)).toBeInTheDocument();

        expect(screen.queryByText(/Четвертая причина/i)).not.toBeInTheDocument();
    });

    it('should not render chart when there are no declined transactions', () => {
        const transactions = [
            createMockTransaction({
                id: 'tx-1',
                status: 'APPROVED',
                declineReason: 'Первая причина' }),
            createMockTransaction({
                id: 'tx-2',
                status: 'DECLINED',
                declineReason: 'Вторая причина' }),
        ];

        render(<DeclineReasonChart transactions={transactions} loading={false} error={null} />);

        expect(screen.queryByText(/Загрузка/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Ошибка/i)).not.toBeInTheDocument();

        expect(screen.queryByText(/Первая причина/i)).not.toBeInTheDocument();
        expect(screen.getByText(/Вторая причина/i)).toBeInTheDocument();
    });
})
