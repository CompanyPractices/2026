import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import useTransactions from '../useTransactions';
import fetchApi from '../../api/client';
import { Transaction, SearchResponse, Filter } from '../../types';

vi.mock('../../api/client');

vi.mock('../../contexts/ToastContext', () => ({
    useToastContext: () => ({
        addToast: vi.fn(),
    }),
}));

const mockedFetchApi = vi.mocked(fetchApi);

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

const mockTransactions: Transaction[] = [
    createMockTransaction({ id: 'tx-1', status: 'APPROVED', amount: 500 }),
    createMockTransaction({ id: 'tx-2', status: 'DECLINED', amount: 300 }),
];

const mockSearchResponse: SearchResponse = {
    total: 2,
    transactions: mockTransactions,
};

describe('useTransactions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should return loading=true and null data on initial render', () => {
        mockedFetchApi.mockReturnValue(new Promise(() => {}));

        const { result } = renderHook(() => useTransactions());

        expect(result.current.loading).toBe(true);
        expect(result.current.transactions).toBeNull();
        expect(result.current.error).toBeNull();
    });

    it('should fetch recent transactions on mount', async () => {
        mockedFetchApi.mockResolvedValue(mockTransactions);

        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactions).toEqual(mockTransactions);
        expect(result.current.error).toBeNull();
        expect(mockedFetchApi).toHaveBeenCalledWith('/api/dashboard/recent?limit=20');
    });

    it('should handle network error on mount', async () => {
        const errorMessage = 'Network Error';
        mockedFetchApi.mockRejectedValue(new Error(errorMessage));

        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactions).toBeNull();
        expect(result.current.error).toBe(errorMessage);
    });

    it('should search transactions with all filters', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        mockedFetchApi.mockResolvedValueOnce(mockSearchResponse);

        const filter: Filter = {
            status: 'APPROVED',
            dateFrom: '2023-10-01T12:00:00Z',
            dateTo: '2023-10-31T23:59:59Z',
            issuerId: 'ISSUER123',
            mcc: '5411',
        };

        act(() => {
            result.current.searchTransactions(filter);
        });

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactions).toEqual(mockTransactions);
        expect(result.current.error).toBeNull();

        expect(mockedFetchApi).toHaveBeenLastCalledWith(
            expect.stringContaining('/api/transactions/search?'),
            expect.objectContaining({ onError: expect.any(Function) })
        );

        const searchCall = mockedFetchApi.mock.calls.find(call =>
            call[0].includes('/api/transactions/search')
        );
        expect(searchCall?.[0]).toContain('status=APPROVED');
        expect(searchCall?.[0]).toContain('dateFrom=2023-10-01');
        expect(searchCall?.[0]).toContain('dateTo=2023-10-31');
        expect(searchCall?.[0]).toContain('issuerId=ISSUER123');
        expect(searchCall?.[0]).toContain('mcc=5411');
    });

    it('should search transactions with partial filters', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        mockedFetchApi.mockResolvedValueOnce(mockSearchResponse);

        const filter: Filter = {
            status: 'DECLINED',
            mcc: '5411',
        };

        act(() => {
            result.current.searchTransactions(filter);
        });

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const searchCall = mockedFetchApi.mock.calls.find(call =>
            call[0].includes('/api/transactions/search')
        );
        expect(searchCall?.[0]).toContain('status=DECLINED');
        expect(searchCall?.[0]).toContain('mcc=5411');
        expect(searchCall?.[0]).not.toContain('dateFrom');
        expect(searchCall?.[0]).not.toContain('dateTo');
        expect(searchCall?.[0]).not.toContain('issuerId');
    });

    it('should clear error when searching', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        mockedFetchApi.mockImplementationOnce(() => new Promise(() => {}));

        act(() => {
            result.current.searchTransactions({ status: 'APPROVED' });
        });

        expect(result.current.error).toBeNull();
    });

    it('should handle network error during search', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const errorMessage = 'Network Error';
        mockedFetchApi.mockRejectedValueOnce(new Error(errorMessage));

        act(() => {
            result.current.searchTransactions({ status: 'APPROVED' });
        });

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.transactions).toEqual(mockTransactions);
        expect(result.current.error).toBe(errorMessage);
    });

    it('should extract only date part from dateFrom and dateTo', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        mockedFetchApi.mockResolvedValueOnce(mockSearchResponse);

        const filter: Filter = {
            dateFrom: '2023-10-01T12:34:56.789Z',
            dateTo: '2023-10-31T23:59:59.999Z',
        };

        act(() => {
            result.current.searchTransactions(filter);
        });

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const searchCall = mockedFetchApi.mock.calls.find(call =>
            call[0].includes('/api/transactions/search')
        );
        expect(searchCall?.[0]).toContain('dateFrom=2023-10-01');
        expect(searchCall?.[0]).toContain('dateTo=2023-10-31');
        expect(searchCall?.[0]).not.toContain('12:34:56');
        expect(searchCall?.[0]).not.toContain('23:59:59');
    });

    it('should change isFiltered to true when filter is active and false when reset', async () => {
        mockedFetchApi.mockResolvedValueOnce(mockTransactions);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });
        expect(result.current.isFiltered).toBe(false);

        mockedFetchApi.mockResolvedValueOnce(mockSearchResponse)
        const filter: Filter = {
            mcc: '5411',
        };
        act(() => {
            result.current.searchTransactions(filter)
        });
        await waitFor(() => {
            expect(result.current.isFiltered).toBe(true);
        });

        const searchCall = mockedFetchApi.mock.calls.find(call =>
            call[0].includes('/api/transactions/search')
        );
        expect(searchCall?.[0]).toContain('5411');
        expect(searchCall?.[0]).not.toContain('status');

        const emptyFilter: Filter = {};
        act(() => {
            result.current.searchTransactions(emptyFilter)
        });
        await waitFor(() => {
            expect(result.current.isFiltered).toBe(false);
        });
    });
});
