import {hidePan, convertPenniesToRubles, formatDateTime} from '../utils/format';
import { getStatusIcon } from '../utils/statusIcon';
import {Filter, PaginationMeta, Transaction} from '../types';
import { useState, useMemo } from 'react';
import { TransactionModal } from './TransactionModal';
import { ArrowDownToLine, ChevronLeft, ChevronRight } from 'lucide-react';
import {Filters} from './Filters';
import {ISSUERS_NAMES, MCC_NAMES} from '../mockData';
import {useExportCsv} from "../hooks/useExportCsv.ts";

type TransactionTableProps = {
    transactions: Transaction[],
    isFiltered: boolean,
    currentFilter: Filter,
    error: string | null,
    loading: boolean,
    search: (filter: Filter) => void,
    pagination: PaginationMeta,
    onPageChange: (page: number) => void,
    onPageSizeChange: (size: number) => void,
};

export function TransactionTable({
     transactions,
     isFiltered,
     currentFilter,
     error,
     loading,
     search,
     pagination,
     onPageChange,
     onPageSizeChange
}: TransactionTableProps){
    const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
    const { exportError, handleExportCsv } = useExportCsv(currentFilter);

    const sortedTransactions = useMemo(() => {
        return [...transactions].sort((a, b) => {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
    }, [transactions]);

    const showNavigation = !loading && !error && pagination.totalElements > 0;

    const pageNumbers = useMemo(() => {
        const pages: (number | string)[] = [];
        const maxVisible = 5;

        if (pagination.totalPages <= maxVisible) {
            for (let i = 0; i < pagination.totalPages; i++) {
                pages.push(i);
            }
        } else {
            const start = Math.max(0, pagination.currentPage - 2);
            const end = Math.min(pagination.totalPages - 1, start + maxVisible - 1);

            if (start > 0) pages.push(0, '...');
            for (let i = start; i <= end; i++) pages.push(i);
            if (end < pagination.totalPages - 1) pages.push('...', pagination.totalPages - 1);
        }

        return pages;
    }, [pagination.currentPage, pagination.totalPages]);

    return (
        <div className="font-mono w-full">
            <Filters issuers={ISSUERS_NAMES} mccNames={MCC_NAMES} onSearch={search}/>

            <div className="flex flex-col sm:flex-row items-center justify-between mt-5 mb-3">
                <h2 className="text-2xl font-bold text-center drop-shadow-lg dark:text-sage-50">
                    {isFiltered ? 'Результаты поиска' : 'Последние транзакции'}
                </h2>

                {!loading && !error && sortedTransactions.length > 0 && (
                    <div className="flex items-center gap-3">
                        <div className="flex items-center ">
                            <label className="text-sm dark:text-sage-50">Записей:</label>
                            <select
                                value={pagination.pageSize}
                                onChange={(e) => onPageSizeChange(Number(e.target.value))}
                                className="border border-zinc-300 dark:border-sage-200 rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 p-1 text-sm"
                            >
                                <option value={10}>10</option>
                                <option value={20}>20</option>
                                <option value={50}>50</option>
                                <option value={100}>100</option>
                            </select>
                        </div>
                        <button
                            className="
                                px-3 md:px-3 py-[2px] md:py-0 text-base md:text-lg rounded-xl bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 font-semibold cursor-pointer
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
                )}
            </div>

            {exportError && (
                <div className="text-center py-2 text-red-500">
                    {exportError}
                </div>
            )}

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
                <>
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

                    {showNavigation && (
                        <div className="flex items-center justify-center gap-2 mb-4">
                            <button
                                onClick={() => onPageChange(0)}
                                disabled={pagination.currentPage === 0}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                «
                            </button>
                            <button
                                onClick={() => onPageChange(pagination.currentPage - 1)}
                                disabled={pagination.currentPage === 0}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                <ChevronLeft size={16} />
                            </button>

                            {pageNumbers.map((page, idx) => (
                                typeof page === 'string' ? (
                                    <span key={`ellipsis-${idx}`} className="px-2 dark:text-sage-50">...</span>
                                ) : (
                                    <button
                                        key={page}
                                        onClick={() => onPageChange(page)}
                                        className={`px-3 py-1 rounded-lg transition-colors ${
                                            page === pagination.currentPage
                                                ? 'bg-emerald-600 dark:bg-sage-100 text-white dark:text-sage-500 font-bold'
                                                : 'bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 hover:bg-emerald-500 dark:hover:bg-sage-100'
                                        }`}
                                    >
                                        {page + 1}
                                    </button>
                                )
                            ))}

                            <button
                                onClick={() => onPageChange(pagination.currentPage + 1)}
                                disabled={pagination.currentPage >= pagination.totalPages - 1}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                <ChevronRight size={16} />
                            </button>
                            <button
                                onClick={() => onPageChange(pagination.totalPages - 1)}
                                disabled={pagination.currentPage >= pagination.totalPages - 1}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                »
                            </button>
                        </div>
                    )}
                </>
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
