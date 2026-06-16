import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import useStats from './useStats';
import fetchApi from '../api/client';
import { DashboardStats } from '../types';

vi.mock('../api/client', () => ({
    default: vi.fn(),
}));

const mockedFetchApi = vi.mocked(fetchApi);

describe('useStats', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockedFetchApi.mockResolvedValue({} as DashboardStats);
    });

    it('should return loading=true and null data in beginning render', () => {
        const { result } = renderHook(() => useStats());

        expect(result.current.loading).toBe(true);
        expect(result.current.transactionStats).toBeNull();
        expect(result.current.error).toBeNull();
    });

    it('should successfully load statistics and update state', async () => {
        const mockData: DashboardStats = {
            totalTransactions: 100,
            approvedCount: 80,
            totalAmount: 50000,
            avgProcessingTimeMs: 150,
        };

        mockedFetchApi.mockResolvedValue(mockData);

        const { result } = renderHook(() => useStats());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactionStats).toEqual(mockData);
        expect(result.current.error).toBeNull();
        expect(mockedFetchApi).toHaveBeenCalledTimes(1);
        expect(mockedFetchApi).toHaveBeenCalledWith('/api/dashboard/stats');
    });

    it('should handle network error and write error', async () => {
        const errorMessage = 'Network Error';

        mockedFetchApi.mockRejectedValue(new Error(errorMessage));

        const { result } = renderHook(() => useStats());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactionStats).toBeNull();
        expect(result.current.error).toBe(errorMessage);
    });
});
