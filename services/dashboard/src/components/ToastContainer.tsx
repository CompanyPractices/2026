import { Toast } from '../types/toast.ts'
import { ToastItem } from './ToastItem.tsx'

type ToastContainerProps = {
    toasts: Toast[];
    onClose: (id: string) => void;
}

export function ToastContainer({toasts, onClose}: ToastContainerProps){
    return (
        <div className="fixed top-4 right-4 z-[9999] flex flex-col gap-3 pointer-events-none
                        max-h-[80vh] overflow-y-auto w-full max-w-sm sm:max-w-md">
            {toasts.map((ts) => (
                <div key={ts.id} className="pointer-events-auto">
                    <ToastItem toast={ts} onClose={onclose} />
                </div>
            ))}
        </div>
    )
}
