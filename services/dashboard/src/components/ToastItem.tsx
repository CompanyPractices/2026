import { Toast, ToastType } from '../types/toast.ts';
import { getTypeIcon } from '../utils/toastIcon.ts';
import { X, type LucideIcon } from 'lucide-react';

export function ToastItem({toast} : Toast) {
    const toastIcon = getTypeIcon(toast.type);
    const textColor = toastIcon.textColor;
    const backColor = toastIcon.backgroundColor;

    return (
        <div className={`grid grid-cols-2 gap-4 ${textColor} ${backColor}`} >
            <div>
                ${toastIcon.icon}
            </div>
            <div>
                ${toastIcon.label}:
                ${toast.message}
            </div>
            <button
                onClick={onclose}
            >
                Закрыть {X}
            </button>
        </div>
    )
};
