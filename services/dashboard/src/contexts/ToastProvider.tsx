import { useState, useCallback, ReactNode } from 'react'
import { Toast, ToastType } from '../types/toast.ts'
import { ToastContext } from './ToastContext.ts'

interface ToastProviderProps {
    children: ReactNode;
}

export function ToastProvider ({children}: ToastProviderProps) {
    const [toasts, setToasts] = useState<Toast[]>([]);

    const addToast = useCallback((message: string, type: ToastType) => {
        const newToast: Toast = {id: crypto.randomUUID(), message, type};
        setToasts(prev => [...prev, newToast]);
    }, []);

    const removeToast = useCallback((id: string) => {
        setToasts(prev => prev.filter(t => t.id !== id));
    }, []);

    return (
        <ToastContext.Provider value={{addToast, removeToast}}>
            {children}
        </ToastContext.Provider>
    );
}
