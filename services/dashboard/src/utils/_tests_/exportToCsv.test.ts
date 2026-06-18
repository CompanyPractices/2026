import { exportToCsv } from '../exportToCsv';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

describe('exportToCsv', () => {
    let capturedBlobContent = '';

    const mockLink = {
        setAttribute: vi.fn(),
        style: { visibility: '' } as CSSStyleDeclaration,
        click: vi.fn()
    };

    beforeEach(() => {
        vi.clearAllMocks();
        vi.spyOn(console, 'warn').mockImplementation(() => {});

        vi.spyOn(document, 'createElement').mockReturnValue(mockLink as unknown as HTMLElement);
        vi.spyOn(document.body, 'appendChild').mockReturnValue(mockLink as unknown as HTMLElement);
        vi.spyOn(document.body, 'removeChild').mockReturnValue(mockLink as unknown as HTMLElement);

        vi.stubGlobal('Blob', class MockBlob {
            constructor(parts: (string | Blob | ArrayBuffer)[]) {
                capturedBlobContent = parts[0] as string;
            }
        });

        vi.stubGlobal('URL', {
            createObjectURL: vi.fn(() => 'blob:mock-url'),
            revokeObjectURL: vi.fn()
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it('should warn and return early if rows array is empty or null', () => {
        exportToCsv('test.csv', []);
        expect(console.warn).toHaveBeenCalledWith('Нет данных для экспорта');
        expect(document.createElement).not.toHaveBeenCalled();

        vi.clearAllMocks();
        exportToCsv('test.csv', null as unknown as Record<string, unknown>[]);
        expect(console.warn).toHaveBeenCalledWith('Нет данных для экспорта');
    });

    it('should generate correct CSV content and trigger download flow', () => {
        const rows = [
            { Name: 'Alice', Age: 30, Status: null },
            { Name: 'Bob', Age: undefined, Status: 'Active' }
        ];

        exportToCsv('users.csv', rows);

        const expectedCsv = 'sep=;\nName;Age;Status\n="Alice";="30";\n="Bob";;="Active"';
        expect(capturedBlobContent).toBe('\uFEFF' + expectedCsv);

        expect(document.createElement).toHaveBeenCalledWith('a');
        expect(mockLink.setAttribute).toHaveBeenCalledWith('href', 'blob:mock-url');
        expect(mockLink.setAttribute).toHaveBeenCalledWith('download', 'users.csv');
        expect(mockLink.style.visibility).toBe('hidden');
        expect(document.body.appendChild).toHaveBeenCalledWith(mockLink);
        expect(mockLink.click).toHaveBeenCalledTimes(1);
        expect(document.body.removeChild).toHaveBeenCalledWith(mockLink);
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    });

    it('should correctly escape double quotes in cell values', () => {
        const rows = [{ Description: 'Say "Hello" and "Goodbye"' }];
        exportToCsv('quotes.csv', rows);

        expect(capturedBlobContent).toContain('="Say ""Hello"" and ""Goodbye"""');
    });

    it('should use semicolon separator and handle empty strings correctly', () => {
        const rows = [{ Col1: '', Col2: 'Value' }];
        exportToCsv('empty.csv', rows);

        expect(capturedBlobContent).toContain('sep=;\nCol1;Col2\n;="Value"');
    });
});
