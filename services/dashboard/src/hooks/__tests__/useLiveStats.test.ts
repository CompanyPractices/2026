import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useLiveStats } from '../useLiveStats';
import useStats from '../useStats';
import { Transaction, DashboardStats } from '../../types';

vi.mock('../useStats');

const mockedUseStats = vi.mocked(useStats);

const mockStats: DashboardStats = {
    totalTransactions: 10,
    approvedCount: 5,
    declinedCount: 5,
    approvalRate: 50,
    totalAmount: 1000,
    averageAmount: 100,
    avgProcessingTimeMs: 200,
    transactionsPerMinute: 5,
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

describe('useLiveStats', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should return loading=true and zeroed stats on initial render', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: null,
            loading: true,
            error: null,
        });

        const { result } = renderHook(() => useLiveStats([]));

        expect(result.current.loading).toBe(true);
        expect(result.current.error).toBeNull();
        expect(result.current.stats.totalTransactions).toBe(0);
        expect(result.current.stats.approvalRate).toBe(0);
    });

    it('should correctly map and calculate data from useStats', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: mockStats,
            loading: false,
            error: null,
        });

        const { result } = renderHook(() => useLiveStats([]));

        expect(result.current.loading).toBe(false);
        expect(result.current.stats.totalTransactions).toBe(10);
        expect(result.current.stats.approvalRate).toBe(50);
        expect(result.current.stats.totalAmount).toBe(1000);
        expect(result.current.stats.avgProcessingTimeMs).toBe(200);
    });

    it('should correctly update stats when receiving a new APPROVED transaction', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: mockStats,
            loading: false,
            error: null,
        });

        const { result, rerender } = renderHook(
            ({ transactions }: { transactions: Transaction[] }) => useLiveStats(transactions),
            { initialProps: { transactions: [] } }
        );

        const newApprovedTx = createMockTransaction({
            id: 'tx-1',
            status: 'APPROVED',
            amount: 500,
            processingTimeMs: 100,
        });

        act(() => {
            rerender({ transactions: [newApprovedTx] });
        });

        expect(result.current.stats.totalTransactions).toBe(11);
        expect(result.current.stats.approvalRate).toBe(55);
        expect(result.current.stats.totalAmount).toBe(1500);
        expect(result.current.stats.avgProcessingTimeMs).toBe(191);
    });

    it('should correctly handle a DECLINED transaction (without increasing approvedCount)', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: mockStats,
            loading: false,
            error: null,
        });

        const { result, rerender } = renderHook(
            ({ transactions }: { transactions: Transaction[] }) => useLiveStats(transactions),
            { initialProps: { transactions: [] } }
        );

        const newRejectedTx = createMockTransaction({
            id: 'tx-2',
            status: 'DECLINED',
            amount: 300,
            processingTimeMs: 50,
        });

        act(() => {
            rerender({ transactions: [newRejectedTx] });
        });

        expect(result.current.stats.totalTransactions).toBe(11);
        expect(result.current.stats.approvalRate).toBe(45);
        expect(result.current.stats.totalAmount).toBe(1300);
        expect(result.current.stats.avgProcessingTimeMs).toBe(186);
    });

    it('should correctly update stats when receiving multiple new transactions in a single render', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: mockStats,
            loading: false,
            error: null,
        });

        const { result, rerender } = renderHook(
            ({ transactions }: { transactions: Transaction[] }) => useLiveStats(transactions),
            { initialProps: { transactions: [] } }
        );

        const tx1 = createMockTransaction({
            id: 'tx-1',
            status: 'APPROVED',
            amount: 500,
            processingTimeMs: 100,
        });

        const tx2 = createMockTransaction({
            id: 'tx-2',
            status: 'DECLINED',
            amount: 300,
            processingTimeMs: 50,
        });

        act(() => {
            rerender({ transactions: [tx1, tx2] });
        });

        expect(result.current.stats.totalTransactions).toBe(12);
        expect(result.current.stats.approvalRate).toBe(50);
        expect(result.current.stats.totalAmount).toBe(1800);
        expect(result.current.stats.avgProcessingTimeMs).toBe(179);
    });

    it('should propagate errors from useStats', () => {
        mockedUseStats.mockReturnValue({
            transactionStats: null,
            loading: false,
            error: 'Failed to fetch dashboard stats',
        });

        const { result } = renderHook(() => useLiveStats([]));

        expect(result.current.error).toBe('Failed to fetch dashboard stats');
        expect(result.current.loading).toBe(false);
    });
});
