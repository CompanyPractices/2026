import { useState, useCallback } from 'react';
import { Filter } from '../types';
import { fetchApiBlob } from '../api/client';

const buildFilterQuery = (filter: Filter): string => {
    const params = new URLSearchParams();
    if (filter.status) params.append('status', filter.status);
    if (filter.dateFrom) params.append('dateFrom', filter.dateFrom.slice(0, 10));
    if (filter.dateTo) params.append('dateTo', filter.dateTo.slice(0, 10));
    if (filter.issuerId) params.append('issuerId', filter.issuerId);
    if (filter.mcc) params.append('mcc', filter.mcc);
    const qs = params.toString();
    return qs ? `?${qs}` : '';
};

export function useExportCsv(currentFilter: Filter) {
    const [exporting, setExporting] = useState(false);
    const [exportError, setExportError] = useState<string | null>(null);

    const handleExportCsv = useCallback(async () => {
        setExportError(null);
        setExporting(true);
        try {
            const url = `/api/transactions/export${buildFilterQuery(currentFilter)}`;
            const blob = await fetchApiBlob(url);

            const now = new Date();
            const dateStr = now.toISOString().slice(0, 10);
            const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
            const filename = `transactions_${dateStr}_${timeStr}.csv`;

            const link = document.createElement('a');
            const objectUrl = URL.createObjectURL(blob);
            link.href = objectUrl;
            link.download = filename;
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(objectUrl);
        } catch (e) {
            setExportError(e instanceof Error ? e.message : 'Не удалось выгрузить CSV');
        } finally {
            setExporting(false);
        }
    }, [currentFilter]);

    return {
        exporting,
        exportError,
        handleExportCsv,
    };
}
