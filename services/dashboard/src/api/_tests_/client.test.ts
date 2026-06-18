import { describe, it, expect, vi, beforeEach, afterEach} from 'vitest'
import fetchApi from '../client'

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

describe('fetchApi', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        mockFetch.mockClear();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('should return parsed json on successful response', async() => {
        const mockData = {
            id: 1, name: 'test'
        };

        mockFetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockData,
        })
        const answer = await fetchApi('http://localhost/api/test');
        expect(answer).toEqual(mockData);
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should NOT retry on client errors (4xx)', async() => {
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 404,
            statusText: 'Not Found',
        })

        const promise = fetchApi(('http://localhost/api/test'));
        await expect(promise).rejects.toThrow('Client Error 404');
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should retry on server error 503 and succeed on second attempt', async() => {
        mockFetch
            .mockResolvedValueOnce({
            ok: false,
            status: 503,
            statusText: 'Service Unavailable',
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({success: true})
            });

        const promise = fetchApi(('http://localhost/api/test', {retries: 2}));
        await vi.advanceTimersByTimeAsync(2000);
        const answer = await promise;
        await expect(answer).toEqual({ success: true });
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('should abort request if timeout is exceeded', async() => {
        let rejectFn: (reason: Error) => void;
        const fetchPromise = new Promise<never>((_, reject) => {
            rejectFn = reject;
        });

        mockFetch.mockImplementation(() => fetchPromise);
        const promise = fetchApi('http://localhost/api/test', { timeout: 1000 });
        await vi.advanceTimersByTimeAsync(1100);

        rejectFn!(new DOMException('The operation was aborted.', 'AbortError'));
        await expect(promise).rejects.toThrow(/aborted|Failed to fetch/i);
        expect(mockFetch).toHaveBeenCalledTimes(1);
    })
})
