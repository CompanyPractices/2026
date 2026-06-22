import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, act, fireEvent } from '@testing-library/react'
import { Toast } from '../../types/toast'
import { useToastContext } from '../../contexts/ToastContext.ts'
import { ToastProvider } from '../../contexts/ToastProvider'
import { useEffect } from 'react'
import { ToastContainer } from '../../components/ToastContainer';

vi.mock('framer-motion', () => ({
    AnimatePresence: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    motion: {
        div: 'div',
    },
}));

const CreateMockToast = (overrides: Partial<Toast>): Toast => ({
    id: '1',
    message: 'Опа, уведомление пришло',
    type: 'SUCCESS',
    duration: 4000,
    ...overrides,
});

function ToastTest({toast}: {toast: Toast}){
    const { addToast } = useToastContext();
    useEffect(() => {
        addToast(toast.message, toast.type, toast.duration);
    }, [addToast, toast]);
    return <div />;
};

describe('ToastProvider', () => {
    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it('should show toast when addToast is called', () => {
        vi.useFakeTimers();
        const toast = CreateMockToast({message: 'Файл успешно эскпортирован'});
        render(
            <ToastProvider>
                <ToastTest toast={toast} />
                <ToastContainer />
            </ToastProvider>
        );

        expect(screen.getByText(/Файл успешно эскпортирован/i)).toBeInTheDocument();
    });

    it('automatically delete toast after a duration time', async() => {
        vi.useFakeTimers();
        const toast = CreateMockToast({message: 'Тест для автоудаления', duration: 3000});
        render(
            <ToastProvider>
                <ToastTest toast={toast} />
                <ToastContainer />
            </ToastProvider>
        );

        expect(screen.getByText(/Тест для автоудаления/i)).toBeInTheDocument();

        await act(async () => {
            vi.advanceTimersByTime(3001);

        });
        expect(screen.queryByText(/Тест для автоудаления/i)).not.toBeInTheDocument();
    });

    it('delete toast when you click on close button', () => {
        vi.useFakeTimers();
        const toast = CreateMockToast({message: 'Тест для ручного закрытия'});
        render(
            <ToastProvider>
                <ToastTest toast={toast} />
                <ToastContainer />
            </ToastProvider>
        );
        const closeButton = screen.getByRole('button', { name: /закрыть/i });
        fireEvent.click(closeButton);

        expect(screen.queryByText(/Тест для ручного закрытия/i)).not.toBeInTheDocument();
    });
})
