import {hidePan, convertPenniesToRubles, formatTime} from '../utils/format.ts';
import { getStatusIcon } from '../utils/statusIcon.ts';
import {Filter, Transaction} from "../types";
import { useState } from 'react';
import { TransactionModal } from './TransactionModal';
import { ArrowDownToLine } from 'lucide-react';
import {exportToCsv} from "../utils/exportToCsv.ts";
import {Filters} from "./Filters.tsx";
import {ISSUERS_NAMES, MCC_NAMES} from "../mockData.ts";

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
    'Time': formatTime(tx.transmissionDateTime)
});

type TransactionTableProps = {
    liveTransactions: Transaction[],
    error: string | null
    loading: boolean
    search: (filter: Filter) => void;
};

export function TransactionTable({ liveTransactions, error, loading, search }: TransactionTableProps){
    const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

    const handleExportCsv = () => {
        const csvRows = liveTransactions.map(mapTransactionToCsvRow);
        const now = new Date();
        const dateStr = now.toISOString().slice(0, 10);
        const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
        const filename = `transactions_${dateStr}_${timeStr}.csv`;
        exportToCsv(filename, csvRows);
    };

    return (
        <div className="font-mono w-full">
            <div className="m-8">
                <Filters issuers={ISSUERS_NAMES} mccNames={MCC_NAMES} onSearch={search}/>
            </div>

            <div className="flex items-center justify-center gap-3 m-4">
                <h2 className="text-2xl font-bold text-center drop-shadow-lg">
                    Последние 20 транзакций
                </h2>
                <button
                    className="
                    px-5 py-1  text-lg rounded-3xl bg-emerald-400 font-semibold cursor-pointer
                    hover:bg-emerald-500 hover:text-zinc-200 transition-colors duration-200
                    flex items-center gap-1
                    disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-emerald-400 disabled:hover:text-white"
                    onClick={handleExportCsv}
                    disabled={liveTransactions.length === 0} >
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
                <div className="text-center py-8 text-gray-500">
                    Загрузка транзакций...
                </div>
            }

            {!loading && !error && liveTransactions.length === 0 &&
                <div>Транзакций не найдено</div>
            }

            {!loading && !error && liveTransactions.length > 0 &&
                <div className="rounded-3xl border-2 border-emerald-600 shadow-lg mb-5">

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
                        {liveTransactions.map((transaction) => {
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
