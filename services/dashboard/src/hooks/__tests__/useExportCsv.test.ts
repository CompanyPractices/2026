import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useExportCsv } from '../useExportCsv';
import { fetchApiBlob } from '../../api/client';
import { Filter } from '../../types';

vi.mock('../../api/client', () => ({
    fetchApiBlob: vi.fn(),
}));

describe('useExportCsv', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        global.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
        global.URL.revokeObjectURL = vi.fn();
        HTMLAnchorElement.prototype.click = vi.fn();
    });

    it('should initialize with correct default state', () => {
        const { result } = renderHook(() => useExportCsv({}));

        expect(result.current.exporting).toBe(false);
        expect(result.current.exportError).toBe(null);
        expect(typeof result.current.handleExportCsv).toBe('function');
    });

    it('should call fetchApiBlob with correct URL when filter is empty', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
        vi.mocked(fetchApiBlob).mockResolvedValue(mockBlob);

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(fetchApiBlob).toHaveBeenCalledWith('/api/transactions/export');
        expect(fetchApiBlob).toHaveBeenCalledTimes(1);
    });

    it('should build correct query string from filter', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
        vi.mocked(fetchApiBlob).mockResolvedValue(mockBlob);

        const filter: Filter = {
            status: 'APPROVED',
            dateFrom: '2023-10-01',
            dateTo: '2023-10-31',
            issuerId: 'ISS001',
            mcc: '5411',
        };

        const { result } = renderHook(() => useExportCsv(filter));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        const calledUrl = vi.mocked(fetchApiBlob).mock.calls[0][0];
        expect(calledUrl).toContain('/api/transactions/export?');
        expect(calledUrl).toContain('status=APPROVED');
        expect(calledUrl).toContain('dateFrom=2023-10-01');
        expect(calledUrl).toContain('dateTo=2023-10-31');
        expect(calledUrl).toContain('issuerId=ISS001');
        expect(calledUrl).toContain('mcc=5411');
    });

    it('should set exporting to true during export', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
        let resolvePromise: (value: Blob) => void;
        const promise = new Promise<Blob>((resolve) => {
            resolvePromise = resolve;
        });
        vi.mocked(fetchApiBlob).mockReturnValue(promise);

        const { result } = renderHook(() => useExportCsv({}));

        let exportPromise: Promise<void>;
        act(() => {
            exportPromise = result.current.handleExportCsv();
        });

        expect(result.current.exporting).toBe(true);

        await act(async () => {
            resolvePromise!(mockBlob);
            await exportPromise;
        });

        expect(result.current.exporting).toBe(false);
    });

    it('should create download link with correct filename', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
        vi.mocked(fetchApiBlob).mockResolvedValue(mockBlob);

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
        expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    });

    it('should set exportError when fetchApiBlob throws', async () => {
        const errorMessage = 'Network error';
        vi.mocked(fetchApiBlob).mockRejectedValue(new Error(errorMessage));

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(result.current.exportError).toBe(errorMessage);
        expect(result.current.exporting).toBe(false);
    });

    it('should set default error message when non-Error is thrown', async () => {
        vi.mocked(fetchApiBlob).mockRejectedValue('String error');

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(result.current.exportError).toBe('Не удалось выгрузить CSV');
        expect(result.current.exporting).toBe(false);
    });

    it('should clear exportError on new export attempt', async () => {
        vi.mocked(fetchApiBlob)
            .mockRejectedValueOnce(new Error('First error'))
            .mockResolvedValueOnce(new Blob(['csv'], { type: 'text/csv' }));

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(result.current.exportError).toBe('First error');

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(result.current.exportError).toBe(null);
    });

    it('should revoke object URL after download', async () => {
        const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
        vi.mocked(fetchApiBlob).mockResolvedValue(mockBlob);

        const { result } = renderHook(() => useExportCsv({}));

        await act(async () => {
            await result.current.handleExportCsv();
        });

        expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    });
});
