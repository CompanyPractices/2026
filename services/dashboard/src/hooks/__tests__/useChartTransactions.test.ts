import { renderHook, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import useChartTransactions from '../useChartTransactions';
import fetchApi from '../../api/client';
import { Transaction, SearchResponse } from '../../types';

vi.mock('../../api/client', () => ({
    default: vi.fn(),
}));

const mockFetchApi = vi.mocked(fetchApi);

const mockAddToast = vi.fn();
vi.mock('../../contexts/ToastContext', () => ({
    useToastContext: () => ({ addToast: mockAddToast }),
}));

const makeTx = (id: string, createdAt?: string): Transaction => ({
    id,
    mti: '0200',
    stan: '000001',
    pan: '4000000000000001',
    processingCode: '000000',
    processingTimeMs: 100,
    amount: 10000,
    currencyCode: '643',
    terminalId: 'T001',
    responseCode: '00',
    merchantId: 'M001',
    mcc: '5411',
    acquirerId: 'A001',
    status: 'APPROVED',
    transmissionDateTime: createdAt || new Date().toISOString(),
    createdAt: createdAt || new Date().toISOString(),
});

describe('useChartTransactions', () => {
    beforeEach(() => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        mockFetchApi.mockReset();
        mockAddToast.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('загружает транзакции за сегодня при монтировании', async () => {
        const tx = makeTx('tx-1');
        mockFetchApi.mockResolvedValueOnce({ total: 1, transactions: [tx] } as SearchResponse);

        const { result } = renderHook(() => useChartTransactions([]));

        expect(result.current.loading).toBe(true);

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.transactions).toEqual([tx]);
        expect(result.current.error).toBeNull();

        expect(mockFetchApi).toHaveBeenCalledTimes(1);
        const [url] = mockFetchApi.mock.calls[0];
        expect(url).toContain('/api/transactions/search');
        expect(url).toContain(`dateFrom=${new Date().toISOString().slice(0, 10)}`);
        expect(url).toContain('limit=500');
        expect(url).toContain('offset=0');
    });

    it('выполняет пагинацию, если данных больше 500', async () => {
        const batch1 = Array.from({ length: 500 }, (_, i) => makeTx(`tx-${i}`));
        const batch2 = Array.from({ length: 100 }, (_, i) => makeTx(`tx-${500 + i}`));

        mockFetchApi
            .mockResolvedValueOnce({ total: 600, transactions: batch1 } as SearchResponse)
            .mockResolvedValueOnce({ total: 600, transactions: batch2 } as SearchResponse);

        const { result } = renderHook(() => useChartTransactions([]));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(mockFetchApi).toHaveBeenCalledTimes(2);
        expect(result.current.transactions).toHaveLength(600);

        const secondUrl = mockFetchApi.mock.calls[1][0];
        expect(secondUrl).toContain('offset=500');
    });

    it('не делает лишних запросов, если бэк вернул меньше лимита', async () => {
        const txs = Array.from({ length: 50 }, (_, i) => makeTx(`tx-${i}`));
        mockFetchApi.mockResolvedValueOnce({ total: 50, transactions: txs } as SearchResponse);

        const { result } = renderHook(() => useChartTransactions([]));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(mockFetchApi).toHaveBeenCalledTimes(1);
        expect(result.current.transactions).toHaveLength(50);
    });

    it('объединяет REST и live с дедупликацией по id', async () => {
        const sharedTx = makeTx('shared-1');
        const restTx = makeTx('rest-1');
        const liveTx = makeTx('live-1');

        mockFetchApi.mockResolvedValueOnce({
            total: 2,
            transactions: [sharedTx, restTx],
        } as SearchResponse);

        const liveTransactions = [sharedTx, liveTx];

        const { result } = renderHook(() => useChartTransactions(liveTransactions));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.transactions).toHaveLength(3);
        expect(result.current.transactions.map(t => t.id)).toEqual(
            expect.arrayContaining(['shared-1', 'rest-1', 'live-1'])
        );
    });

    it('live-транзакции идут первыми, потом REST', async () => {
        const restTx = makeTx('rest-1');
        const liveTx = makeTx('live-1');

        mockFetchApi.mockResolvedValueOnce({
            total: 1,
            transactions: [restTx],
        } as SearchResponse);

        const { result } = renderHook(() => useChartTransactions([liveTx]));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.transactions[0].id).toBe('live-1');
        expect(result.current.transactions[1].id).toBe('rest-1');
    });


    it('обновляет данные через polling (refreshIntervalMs)', async () => {
        const tx1 = makeTx('tx-1');
        const tx2 = makeTx('tx-2');

        mockFetchApi
            .mockResolvedValueOnce({ total: 1, transactions: [tx1] } as SearchResponse)
            .mockResolvedValueOnce({ total: 1, transactions: [tx2] } as SearchResponse);

        renderHook(() => useChartTransactions([], { refreshIntervalMs: 5000 }));

        await waitFor(() => expect(mockFetchApi).toHaveBeenCalledTimes(1));

        act(() => {
            vi.advanceTimersByTime(5000);
        });

        await waitFor(() => expect(mockFetchApi).toHaveBeenCalledTimes(2));
    });

    it('не делает polling, если refreshIntervalMs = 0', async () => {
        mockFetchApi.mockResolvedValueOnce({
            total: 1,
            transactions: [makeTx('tx-1')],
        } as SearchResponse);

        renderHook(() => useChartTransactions([], { refreshIntervalMs: 0 }));

        await waitFor(() => expect(mockFetchApi).toHaveBeenCalledTimes(1));

        act(() => {
            vi.advanceTimersByTime(60_000);
        });

        expect(mockFetchApi).toHaveBeenCalledTimes(1);
    });

    it('ручной refresh перезагружает данные', async () => {
        mockFetchApi
            .mockResolvedValueOnce({ total: 1, transactions: [makeTx('tx-1')] } as SearchResponse)
            .mockResolvedValueOnce({ total: 1, transactions: [makeTx('tx-2')] } as SearchResponse);

        const { result } = renderHook(() => useChartTransactions([]));

        await waitFor(() => expect(mockFetchApi).toHaveBeenCalledTimes(1));

        act(() => {
            result.current.refresh();
        });

        await waitFor(() => expect(mockFetchApi).toHaveBeenCalledTimes(2));
    });

    it('не падает при unmount во время загрузки', async () => {
        mockFetchApi.mockImplementation(() => new Promise(() => {}));

        const { unmount } = renderHook(() => useChartTransactions([]));

        unmount();

        expect(() => vi.runAllTimers()).not.toThrow();
    });
});
