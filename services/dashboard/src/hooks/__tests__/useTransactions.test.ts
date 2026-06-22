import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import useTransactions from '../useTransactions';
import fetchApi from '../../api/client';
import { Transaction, SearchResponse } from '../../types';

vi.mock('../../api/client');

vi.mock('../../contexts/ToastContext', () => ({
    useToastContext: () => ({
        addToast: vi.fn(),
    }),
}));

const mockedFetchApi = vi.mocked(fetchApi);

const mockSearchParams = new URLSearchParams();
const mockSetSearchParams = vi.fn((params) => {
    const newParams = typeof params === 'function' ? params(mockSearchParams) : params;
    [...mockSearchParams.keys()].forEach((key) => mockSearchParams.delete(key));
    newParams.forEach((value, key) => mockSearchParams.set(key, value));
});

vi.mock('react-router-dom', () => ({
    useSearchParams: () => [mockSearchParams, mockSetSearchParams],
}));

const createMockTransaction = (overrides: Partial<Transaction>): Transaction => ({
    id: 'tx-default',
    mti: '0200',
    stan: '123456',
    rrn: '123',
    pan: '4111111111111111',
    processingCode: '000000',
    processingTimeMs: 100,
    amount: 150000,
    currencyCode: '643',
    terminalId: 'TERM3171',
    terminalType: 'POS',
    responseCode: '00',
    merchantId: 'MERCH1328400148',
    mcc: '5411',
    acquirerId: 'ACQ650',
    issuerId: 'ISS002',
    status: 'APPROVED',
    transmissionDateTime: '2023-10-27T10:00:00Z',
    createdAt: '2023-10-27T10:00:00Z',
    ...overrides,
});

const mockTransactions: Transaction[] = [
    createMockTransaction({ id: 'tx-1', status: 'APPROVED', amount: 500 }),
    createMockTransaction({ id: 'tx-2', status: 'DECLINED', amount: 300 }),
];

const mockSearchResponse: SearchResponse = { total: 25, transactions: mockTransactions };

describe('useTransactions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        [...mockSearchParams.keys()].forEach((key) => mockSearchParams.delete(key));
    });

    it('should return loading=true and null data on initial render', () => {
        mockedFetchApi.mockReturnValue(new Promise(() => {}));
        const { result } = renderHook(() => useTransactions());
        expect(result.current.loading).toBe(true);
        expect(result.current.transactions).toBeNull();
        expect(result.current.error).toBeNull();
    });

    it('should fetch from /search with default pagination on mount', async () => {
        mockedFetchApi.mockResolvedValue(mockSearchResponse);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.transactions).toEqual(mockTransactions);
        expect(mockedFetchApi).toHaveBeenCalledTimes(1);
        const url = mockedFetchApi.mock.calls[0][0];
        expect(url).toContain('/api/transactions/search?');
        expect(url).toContain('limit=20');
        expect(url).toContain('offset=0');
    });

    it('should handle network error on mount', async () => {
        mockedFetchApi.mockRejectedValue(new Error('Network Error'));
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => expect(result.current.loading).toBe(false));
        expect(result.current.error).toBe('Network Error');
        expect(result.current.transactions).toEqual([]);
    });

    it('should call applyFilter and reset page in URL', async () => {
        mockedFetchApi.mockResolvedValue(mockSearchResponse);
        const { result } = renderHook(() => useTransactions());
        await waitFor(() => expect(result.current.loading).toBe(false));

        act(() => {
            result.current.applyFilter({ status: 'APPROVED', mcc: '5411' });
        });

        expect(mockSearchParams.get('status')).toBe('APPROVED');
        expect(mockSearchParams.get('mcc')).toBe('5411');
        expect(mockSearchParams.get('page')).toBe('0');
    });

    it('should call goToPage and update URL', async () => {
        mockedFetchApi.mockResolvedValue(mockSearchResponse);
        const { result } = renderHook(() => useTransactions());
        await waitFor(() => expect(result.current.loading).toBe(false));

        act(() => {
            result.current.goToPage(2);
        });

        expect(mockSearchParams.get('page')).toBe('2');
    });

    it('should call changePageSize, reset page and update URL', async () => {
        mockedFetchApi.mockResolvedValue(mockSearchResponse);
        const { result } = renderHook(() => useTransactions());
        await waitFor(() => expect(result.current.loading).toBe(false));

        act(() => {
            result.current.changePageSize(50);
        });

        expect(mockSearchParams.get('pageSize')).toBe('50');
        expect(mockSearchParams.get('page')).toBe('0');
    });

    it('should update totalElements in state after successful fetch', async () => {
        mockedFetchApi.mockResolvedValue(mockSearchResponse);
        const { result } = renderHook(() => useTransactions());

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.pagination.totalElements).toBe(25);
        expect(result.current.pagination.totalPages).toBe(2);
    });
});
