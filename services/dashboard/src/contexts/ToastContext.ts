import { createContext } from 'react';
import { Toast } from '../types/toast.ts'

export interface ToastContextType {
    addToast: (message: string, type: ToastType) => void;
    removeToast: (id: string) => void;
}

export const ToastContext = createContext<ToastContextType>({
    addToast: () => {},
    removeToast: () => {},
});
