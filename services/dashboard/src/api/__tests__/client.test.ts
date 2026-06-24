import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import fetchApi, { fetchApiBlob } from '../client'

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

    it('should return parsed json on successful response', async () => {
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

    it('should NOT retry on client errors (4xx)', async () => {
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 404,
            statusText: 'Not Found',
        })

        const promise = fetchApi('http://localhost/api/test');
        await expect(promise).rejects.toThrow('Client Error 404');
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should retry on server error 503 and succeed on second attempt', async () => {
        mockFetch
            .mockResolvedValueOnce({
                ok: false,
                status: 503,
                statusText: 'Service Unavailable',
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true })
            });

        const promise = fetchApi('http://localhost/api/test', { retries: 2 });
        await vi.advanceTimersByTimeAsync(2000);
        const answer = await promise;
        expect(answer).toEqual({ success: true });
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('should abort request if timeout is exceeded', async () => {
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

describe('fetchApiBlob', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        mockFetch.mockClear();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('should return blob on successful response', async () => {
        const mockBlob = new Blob(['test data'], { type: 'text/csv' });

        mockFetch.mockResolvedValueOnce({
            ok: true,
            blob: async () => mockBlob,
        })

        const answer = await fetchApiBlob('http://localhost/api/export');
        expect(answer).toBeInstanceOf(Blob);
        expect(answer).toEqual(mockBlob);
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should send Accept: text/csv header', async () => {
        const mockBlob = new Blob(['test data'], { type: 'text/csv' });

        mockFetch.mockResolvedValueOnce({
            ok: true,
            blob: async () => mockBlob,
        })

        await fetchApiBlob('http://localhost/api/export');

        expect(mockFetch).toHaveBeenCalledWith(
            'http://localhost/api/export',
            expect.objectContaining({
                headers: { Accept: 'text/csv' },
            })
        );
    });

    it('should NOT retry on client errors (4xx)', async () => {
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 400,
            statusText: 'Bad Request',
        })

        const promise = fetchApiBlob('http://localhost/api/export');
        await expect(promise).rejects.toThrow('Client Error 400');
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should retry on server error 503 and succeed on second attempt', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });

        mockFetch
            .mockResolvedValueOnce({
                ok: false,
                status: 503,
                statusText: 'Service Unavailable',
            })
            .mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob,
            });

        const promise = fetchApiBlob('http://localhost/api/export', { retries: 2 });
        await vi.advanceTimersByTimeAsync(2000);
        const answer = await promise;
        expect(answer).toBeInstanceOf(Blob);
        expect(answer).toEqual(mockBlob);
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('should abort request if timeout is exceeded', async () => {
        let rejectFn: (reason: Error) => void;
        const fetchPromise = new Promise<never>((_, reject) => {
            rejectFn = reject;
        });

        mockFetch.mockImplementation(() => fetchPromise);
        const promise = fetchApiBlob('http://localhost/api/export', { timeout: 1000 });
        await vi.advanceTimersByTimeAsync(1100);

        rejectFn!(new DOMException('The operation was aborted.', 'AbortError'));
        await expect(promise).rejects.toThrow(/aborted|Failed to fetch/i);
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should throw error after all retries exhausted on 503', async () => {
        mockFetch
            .mockResolvedValueOnce({
                ok: false,
                status: 503,
                statusText: 'Service Unavailable',
            })
            .mockResolvedValueOnce({
                ok: false,
                status: 503,
                statusText: 'Service Unavailable',
            })
            .mockResolvedValueOnce({
                ok: false,
                status: 503,
                statusText: 'Service Unavailable',
            });

        const promise = fetchApiBlob('http://localhost/api/export', { retries: 2 });

        const assertionPromise = expect(promise).rejects.toThrow('Server Error 503');

        await vi.advanceTimersByTimeAsync(4000);

        await assertionPromise;

        expect(mockFetch).toHaveBeenCalledTimes(3);
    });
});
