import { TriangleAlert, SquareX, MessageCircleCheck, Info, type LucideIcon } from 'lucide-react';
import { ToastType } from '../types/toast.ts';

type ToastIconType = {
    icon: LucideIcon,
    textColor: string,
    backgroundColor: string,
    label: string,
    size: number,
}

const ICON_TYPE: Record<ToastType, ToastIconType> = {
    'SUCCESS': {
        icon: MessageCircleCheck,
        textColor: 'text-green-500',
        backgroundColor: 'bg-green-100',
        label: 'УСПЕШНО',
        size: 18,
    },
    'ERROR': {
        icon: SquareX,
        textColor: 'text-red-500',
        backgroundColor: 'bg-red-100',
        label: 'ОШИБКА',
        size: 18,
    },
    'WARNING': {
        icon: TriangleAlert,
        textColor: 'text-orange-500',
        backgroundColor: 'bg-orange-100',
        label: 'ПРЕДУПРЕЖДЕНИЕ',
        size: 18,
    },
    'INFO': {
        icon: Info,
        textColor: 'text-blue-500',
        backgroundColor: 'bg-blue-100',
        label: 'ИНФОРМАЦИЯ',
        size: 18,
    }
}

export const getTypeIcon = (type: ToastType): ToastIconType => {
    return ICON_TYPE[type];
}
