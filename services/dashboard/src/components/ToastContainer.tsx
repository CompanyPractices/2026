import { Toast } from '../types/toast.ts'
import { ToastItem } from './ToastItem.tsx'
import { AnimatePresence } from 'framer-motion'

type ToastContainerProps = {
    toasts: Toast[];
    onClose: (id: string) => void;
}

export function ToastContainer({toasts, onClose}: ToastContainerProps){
    return (
        <div className="fixed top-4 right-4 z-[9999] flex flex-col gap-3 pointer-events-none
                        max-h-[80vh] overflow-y-auto w-full max-w-sm sm:max-w-md">
            <AnimatePresence>
                {toasts.map((ts) => (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.3 }}
                        key={ts.id}
                        className="pointer-events-auto">
                        <ToastItem toast={ts} onClose={onclose} />
                    </motion.div>
                ))}
            </AnimatePresence>
        </div>
    )
}
