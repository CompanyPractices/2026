import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useLocations } from './useLocations';
import { Transaction } from '../types';

const { mockedGetLocation } = vi.hoisted(() => {
    return {
        mockedGetLocation: vi.fn(),
    };
});

vi.mock('../utils/geoGenerator', () => ({
    getTransactionLocation: mockedGetLocation,
}));

const createMockTx = (id: string, issuerId: string): Transaction =>
    ({ id, issuerId, amount: 100 } as Transaction);

describe('useLocations', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should return empty array if transactions array is empty', () => {
        const { result } = renderHook(() => useLocations([]));

        expect(result.current).toEqual([]);
        expect(mockedGetLocation).not.toHaveBeenCalled();
    });

    it('should group transactions by city and count them correctly', () => {
        mockedGetLocation.mockImplementation((issuerId) => {
            if (issuerId === 'issuer_msk_1') return { city: 'Владивосток', coordinates: [43.1056, 131.874] };
            if (issuerId === 'issuer_msk_2') return { city: 'Владивосток', coordinates: [43.1056, 131.874] };
            if (issuerId === 'issuer_ldn') return { city: 'Красноярск', coordinates: [56.0184, 92.8672] };
            return { city: 'Unknown', coordinates: [0, 0] };
        });

        const tx1 = createMockTx('tx1', 'issuer_msk_1');
        const tx2 = createMockTx('tx2', 'issuer_msk_2');
        const tx3 = createMockTx('tx3', 'issuer_ldn');

        const { result } = renderHook(() => useLocations([tx1, tx2, tx3]));

        expect(result.current).toHaveLength(2);

        const mskCluster = result.current.find(c => c.city === 'Владивосток');
        expect(mskCluster).toBeDefined();
        expect(mskCluster!.count).toBe(2);
        expect(mskCluster!.transactions).toEqual([tx1, tx2]);

        const ldnCluster = result.current.find(c => c.city === 'Красноярск');
        expect(ldnCluster).toBeDefined();
        expect(ldnCluster!.count).toBe(1);
    });

    it('should call getTransactionLocation with correct issuerId', () => {
        mockedGetLocation.mockReturnValue({ city: 'Красноярск', coordinates: [56.0184, 92.8672] });
        const tx = createMockTx('tx1', 'issuer_ber_1');

        renderHook(() => useLocations([tx]));

        expect(mockedGetLocation).toHaveBeenCalledTimes(1);
        expect(mockedGetLocation).toHaveBeenCalledWith('issuer_ber_1');
    });

    describe('Memoization', () => {
        it('should return the same reference if transactions reference has not changed', () => {
            mockedGetLocation.mockReturnValue({ city: 'Ухта', coordinates: [63.5671, 53.6835] });
            const transactions = [createMockTx('tx1', 'issuer_1')];

            const { result, rerender } = renderHook(
                ({ txs }) => useLocations(txs),
                { initialProps: { txs: transactions } }
            );

            const firstRenderResult = result.current;
            rerender({ txs: transactions });

            expect(result.current).toBe(firstRenderResult);
        });

        it('should recalculate if transactions reference changes', () => {
            mockedGetLocation.mockReturnValue({ city: 'Калининград', coordinates: [54.7065, 20.511] });
            const tx = createMockTx('tx1', 'issuer_1');

            const { result, rerender } = renderHook(
                ({ txs }) => useLocations(txs),
                { initialProps: { txs: [tx] } }
            );

            const firstRenderResult = result.current;
            rerender({ txs: [...[tx]] });

            expect(result.current).not.toBe(firstRenderResult);
            expect(result.current).toEqual(firstRenderResult);
        });
    });
});
