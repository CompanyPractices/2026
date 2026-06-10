import { hidePan, convertPenniesToRubles, formatTime } from '../utils/format.ts';
import { getStatusIcon } from '../utils/statusIcon.ts';
import { Transaction } from "../types";
import { useState } from 'react';
import { TransactionModal } from './TransactionModal';
import {useWebSocket} from "../hooks/useWebSocket.ts";
import useTransactions from "../hooks/useTransactions.ts";
import { ArrowDownToLine } from 'lucide-react';

export function TransactionTable(){
    const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
    const { transactions: initialTransactions, loading, error } = useTransactions();
    const { liveTransactions } = useWebSocket();

    const allTransactions = [
        ...liveTransactions,
        ...(initialTransactions || []),
    ];

    const uniqueTransactions = allTransactions.filter((tx, index, self) =>
        index === self.findIndex(t => t.id === tx.id)
    );

    const displayedTransactions = uniqueTransactions.slice(0, 20);

    if (loading && liveTransactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500">
                Загрузка транзакций...
            </div>
        );
    }

    if (error && liveTransactions.length === 0) {
        return (
            <div className="text-center py-8 text-red-500">
                Ошибка загрузки транзакций: {error}
            </div>
        );
    }

    if (displayedTransactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500">
                Ожидание первых транзакций...
            </div>
        );
    }

    return (
        <div className="font-mono w-full">
            <div className="flex items-center justify-center gap-3 mb-4">
                <h2 className="text-2xl font-bold text-center drop-shadow-lg">
                    Последние 20 транзакций
                </h2>
                <button className='px-5 py-1  text-lg rounded-3xl bg-emerald-400 font-semibold cursor-pointer hover:bg-emerald-500 hover:text-zinc-200 transition-colors duration-200 flex items-center gap-1' >
                    <ArrowDownToLine size={16} strokeWidth={3} className="inline-block"/>
                    CSV
                </button>
            </div>

            <div className="rounded-3xl overflow-hidden border-2 border-emerald-600 shadow-lg mb-5">

                <div className="overflow-x-auto">

                    <table className="w-full min-w-[900px] text-sm">
                        <thead>
                        <tr className="border-b-2 border-emerald-600 text-center font-semibold">
                            <th className="px-6 py-4">Время</th>
                            <th className="px-6 py-4">PAN</th>
                            <th className="px-6 py-4 text-right">Сумма</th>
                            <th className="px-6 py-4">Мерчант</th>
                            <th className="px-6 py-4">Статус</th>
                        </tr>
                        </thead>
                        <tbody>
                        {displayedTransactions.map((transaction) => {
                            const statusIconData = getStatusIcon(transaction.status);

                            return (
                                <tr
                                    key={transaction.id}
                                    className="text-center cursor-pointer hover:bg-emerald-50 transition-colors border-b border-gray-100 last:border-0"
                                    onClick={() => setSelectedTx(transaction)}
                                >
                                    <td className="px-6 py-3">{formatTime(transaction.transmissionDateTime)}</td>
                                    <td className="px-6 py-3">{hidePan(transaction.pan)}</td>
                                    <td className="px-6 py-3 text-right">{convertPenniesToRubles(transaction.amount)}</td>
                                    <td className="px-6 py-3 ">{transaction.merchantId}</td>
                                    <td className="px-6 py-3">
                                        <div className="flex justify-center items-center gap-2">
                                            <statusIconData.icon
                                                className={statusIconData.color}
                                                size={statusIconData.size}
                                                aria-hidden="true"
                                            />
                                            <span className="sr-only">{statusIconData.label}</span>
                                        </div>
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>
            </div>

            {selectedTx && (
                <TransactionModal
                    transaction={selectedTx}
                    onClose={() => setSelectedTx(null)}
                />
            )}
        </div>
    );
}
