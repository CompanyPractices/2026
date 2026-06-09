import { useState, useEffect, useRef } from 'react';
import { Transaction } from '../types';

type UseWebSocketOptions = {
    url?: string;
    maxRetries?: number;
    retryDelayMs?: number;
}

export function useWebSocket({
      url = 'ws://localhost:3000/ws/transactions',
      maxRetries = 5,
      retryDelayMs = 3000
  }: UseWebSocketOptions = {}){
    const [liveTransactions, setLiveTransactions] = useState<Transaction[]>([]);
    const [isConnected, setIsConnected] = useState(false);

    const wsRef = useRef<WebSocket | null>(null);
    const reconnectTimeoutRef = useRef<number | null>(null);
    const retryCountRef = useRef(0);
    const isMountedRef = useRef(true);

    useEffect(() => {
        isMountedRef.current = true;
        retryCountRef.current = 0;

        const connect = () => {
            const ws = new WebSocket(url);
            wsRef.current = ws;

            ws.onopen = () => {
                if (!isMountedRef.current) return;
                console.log('WebSocket подключен к transaction-logger');
                setIsConnected(true);
                retryCountRef.current = 0;

                if (reconnectTimeoutRef.current) {
                    clearTimeout(reconnectTimeoutRef.current);
                    reconnectTimeoutRef.current = null;
                }
            };

            ws.onmessage = (event) => {
                if (!isMountedRef.current) return;
                try {
                    const newTransaction: Transaction = JSON.parse(event.data);
                    console.log('Новая транзакция:', newTransaction);

                    setLiveTransactions((prev) => [newTransaction, ...prev].slice(0, 20));
                } catch (error) {
                    console.error('Ошибка парсинга WebSocket-сообщения:', error);
                }
            };

            ws.onerror = (error) => {
                console.error('Ошибка WebSocket:', error);
            };

            ws.onclose = (event) => {
                if (!isMountedRef.current) return;
                console.log(`WebSocket закрыт. Код: ${event.code}`);
                setIsConnected(false);
                wsRef.current = null;

                if (event.code !== 1000 && retryCountRef.current < maxRetries) {
                    retryCountRef.current++;
                    console.log(`Попытка переподключения ${retryCountRef.current}/${maxRetries}...`);
                    reconnectTimeoutRef.current = setTimeout(connect, retryDelayMs);
                } else if (retryCountRef.current >= maxRetries) {
                    console.warn('Превышен лимит попыток переподключения');
                }
            };
        };

        connect();

        return () => {
            console.log('Закрываем WebSocket и отменяем таймеры');
            isMountedRef.current = false;

            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
                wsRef.current.close(1000, 'Component unmounted');
            }
        };
    }, [url, maxRetries, retryDelayMs]);

    return { liveTransactions, isConnected };
}
