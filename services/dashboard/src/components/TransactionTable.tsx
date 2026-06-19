import {hidePan, convertPenniesToRubles, formatTime, formatDate, formatDateTime} from '../utils/format';
import { getStatusIcon } from '../utils/statusIcon';
import {Filter, PaginationMeta, Transaction} from '../types';
import { useState, useMemo } from 'react';
import { TransactionModal } from './TransactionModal';
import { ArrowDownToLine } from 'lucide-react';
import {exportToCsv} from '../utils/exportToCsv';
import {Filters} from './Filters';
import {ISSUERS_NAMES, MCC_NAMES} from '../mockData';

const mapTransactionToCsvRow = (tx: Transaction) => ({
    'STAN': tx.stan,
    'RRN': tx.rrn || '—',
    'PAN': hidePan(tx.pan),
    'Amount': tx.amount,
    'Status': tx.status,
    'Auth code': tx.authCode || '—',
    'Terminal': `${tx.terminalId}${tx.terminalType ? ` (${tx.terminalType})` : ''}`,
    'Merchant ID': tx.merchantId,
    'MCC': tx.mcc,
    'Acquirer ID': tx.acquirerId,
    'Issuer ID': tx.issuerId || '—',
    'Time': formatTime(tx.createdAt),
    'Date': formatDate(tx.createdAt)
});

type TransactionTableProps = {
    liveTransactions: Transaction[],
    error: string | null,
    loading: boolean,
    search: (filter: Filter) => void,
    pagination: PaginationMeta,
    onPageChange: (page: number) => void,
    onPageSizeChange: (size: number) => void,
};

export function TransactionTable({ liveTransactions, error, loading, search }: TransactionTableProps){
    const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

    const sortedTransactions = useMemo(() => {
        return [...liveTransactions].sort((a, b) => {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
    }, [liveTransactions]);

    const handleExportCsv = () => {
        const csvRows = sortedTransactions.map(mapTransactionToCsvRow);
        const now = new Date();
        const dateStr = now.toISOString().slice(0, 10);
        const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
        const filename = `transactions_${dateStr}_${timeStr}.csv`;
        exportToCsv(filename, csvRows);
    };

    return (
        <div className="font-mono w-full">
            <Filters issuers={ISSUERS_NAMES} mccNames={MCC_NAMES} onSearch={search}/>

            <div className="flex items-center justify-center gap-3 m-4">
                <h2 className="text-2xl font-bold text-center drop-shadow-lg dark:text-sage-50">
                    Последние 20 транзакций
                </h2>
                <button
                    className="
                    px-3 md:px-5 py-1 md:py-1 text-base md:text-lg rounded-xl md:rounded-3xl bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 font-semibold cursor-pointer
                    hover:bg-emerald-500 hover:text-zinc-200 transition-none hover:transition-colors hover:duration-200
                    dark:hover:bg-sage-100 dark:hover:text-sage-500
                    flex items-center gap-1
                    disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-emerald-400 disabled:hover:text-white
                    disabled:hover:bg-sage-200 disabled:hover:text-sage-50"
                    onClick={handleExportCsv}
                    disabled={sortedTransactions.length === 0} >
                    <ArrowDownToLine size={16} strokeWidth={3} className="inline-block"/>
                    CSV
                </button>
            </div>

            {error &&
                <div className="text-center py-8 text-red-500">
                    Ошибка загрузки транзакций: {error}
                </div>
            }

            {loading &&
                <div className="text-center py-8 text-gray-500 dark:text-sage-50">
                    Загрузка транзакций...
                </div>
            }

            {!loading && !error && sortedTransactions.length === 0 &&
                <div className="text-center py-8 dark:text-sage-50">Транзакций не найдено</div>
            }

            {!loading && !error && sortedTransactions.length > 0 &&
                <div className="rounded-xl border-2 border-emerald-600 dark:border-sage-200 shadow-lg mb-5 overflow-hidden">

                <div className="overflow-x-auto">

                    <table className="w-full min-w-[900px] text-sm">
                        <thead>
                        <tr className="border-b-2 border-emerald-600 dark:border-sage-200 text-center font-semibold dark:text-sage-50">
                            <th className="px-3 md:px-6 py-2 md:py-4">Дата, Время</th>
                            <th className="px-3 md:px-6 py-2 md:py-4">PAN</th>
                            <th className="px-3 md:px-6 py-2 md:py-4 text-right">Сумма</th>
                            <th className="px-3 md:px-6 py-2 md:py-4">Мерчант</th>
                            <th className="px-3 md:px-6 py-2 md:py-4">Статус</th>
                        </tr>
                        </thead>
                        <tbody>
                        {sortedTransactions.map((transaction) => {
                            const statusIconData = getStatusIcon(transaction.status);

                            return (
                                <tr
                                    key={transaction.id}
                                    className="text-center cursor-pointer hover:bg-emerald-50 dark:hover:bg-sage-400 transition-colors border-b border-gray-100 dark:border-sage-400 last:border-0 dark:text-sage-50"
                                    onClick={() => setSelectedTx(transaction)}
                                >
                                    <td className="px-3 md:px-6 py-2 md:py-4">{formatDateTime(transaction.createdAt)}</td>
                                    <td className="px-3 md:px-6 py-2 md:py-4">{hidePan(transaction.pan)}</td>
                                    <td className="px-3 md:px-6 py-2 md:py-4 text-right">{convertPenniesToRubles(transaction.amount)}</td>
                                    <td className="px-3 md:px-6 py-2 md:py-4">{transaction.merchantId}</td>
                                    <td className="px-3 md:px-6 py-2 md:py-4">
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
            }
            {selectedTx && (
                <TransactionModal
                    transaction={selectedTx}
                    onClose={() => setSelectedTx(null)}
                />
            )}
        </div>
    );
}
