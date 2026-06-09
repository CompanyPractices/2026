import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useWebSocket } from './useWebSocket';

class MockWebSocket {
    url: string;
    onopen: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;
    readyState = 1;

    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;

    static instances: MockWebSocket[] = [];

    constructor(url: string) {
        this.url = url;
        MockWebSocket.instances.push(this);
    }

    close = vi.fn();
    send = vi.fn();
}

describe('useWebSocket', () => {
    beforeEach(() => {
        MockWebSocket.instances = [];
        global.WebSocket = MockWebSocket as unknown as typeof WebSocket;
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should connect to WebSocket on mount', () => {
        renderHook(() => useWebSocket({ url: 'ws://localhost:8088/ws/transactions' }));

        expect(MockWebSocket.instances.length).toBe(1);
        expect(MockWebSocket.instances[0].url).toBe('ws://localhost:8088/ws/transactions');
    });

    it('should update isConnected when connection opens', () => {
        const { result } = renderHook(() => useWebSocket());

        expect(result.current.isConnected).toBe(false);

        act(() => {
            MockWebSocket.instances[0].onopen?.(new Event('open'));
        });

        expect(result.current.isConnected).toBe(true);
    });

    it('should add new transaction when message received', () => {
        const { result } = renderHook(() => useWebSocket());

        const mockTransaction = {
            id: 'test-1',
            amount: 1000,
            status: 'APPROVED',
            transmissionDateTime: '2026-06-09T10:00:00Z',
            pan: '4111111111111111',
            merchantId: 'MERCH001',
        };

        act(() => {
            MockWebSocket.instances[0].onmessage?.({
                data: JSON.stringify(mockTransaction),
            } as MessageEvent);
        });

        expect(result.current.liveTransactions).toHaveLength(1);
        expect(result.current.liveTransactions[0].id).toBe('test-1');
    });

    it('should limit transactions to 20', () => {
        const { result } = renderHook(() => useWebSocket());

        act(() => {
            for (let i = 0; i < 25; i++) {
                MockWebSocket.instances[0].onmessage?.({
                    data: JSON.stringify({
                        id: `tx-${i}`,
                        amount: 1000,
                        status: 'APPROVED',
                        transmissionDateTime: '2026-06-09T10:00:00Z',
                        pan: '4111111111111111',
                        merchantId: 'MERCH001',
                    }),
                } as MessageEvent);
            }
        });

        expect(result.current.liveTransactions).toHaveLength(20);
        expect(result.current.liveTransactions[0].id).toBe('tx-24');
    });

    it('should close WebSocket on unmount', () => {
        const { unmount } = renderHook(() => useWebSocket());

        unmount();

        expect(MockWebSocket.instances[0].close).toHaveBeenCalled();
    });
});
