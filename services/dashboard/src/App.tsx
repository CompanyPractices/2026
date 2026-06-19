import TransactionLineChart from "./components/TransactionLineChart.tsx";
import {Header} from "./components/Header.tsx";
import TransactionPieChart from "./components/TransactionPieChart.tsx";
import {TransactionTable} from "./components/TransactionTable.tsx";
import {useLiveStats} from "./hooks/useLiveStats.ts";
import {useWebSocket} from "./hooks/useWebSocket.ts";
import useTransactions from './hooks/useTransactions.ts'
import { useMemo } from 'react'
import {TransactionMap} from "./components/TransactionsMap.tsx";
import TransactionHistogram from './components/TransactionHistogram.tsx'

function App() {
    const { liveTransactions, isConnected } = useWebSocket();
    const { transactions: initialTransactions, filteredTransactions, isFiltered, loading, error, searchTransactions } = useTransactions();
    const { stats, loading: liveLoading, error: liveError } = useLiveStats(liveTransactions);

    const uniqueTransactions = useMemo(() => {
        const allTransactions = [
            ...liveTransactions,
            ...(initialTransactions || []),
        ];
        const seen = new Set();
        return allTransactions
            .filter(tx => {
                if (seen.has(tx.id)) return false;
                seen.add(tx.id);
                return true;
            });
    }, [liveTransactions, initialTransactions]);

    const displayedTransactions = useMemo(() => {
        if (isFiltered){
            const filtered = filteredTransactions || [];
            const seen = new Set();
            return filtered
                .filter(tx => {
                    if (seen.has(tx.id)) return false;
                    seen.add(tx.id);
                    return true;
                })
                .slice(0, 20);
        }
        return uniqueTransactions.slice(0, 20);
    }, [isFiltered, filteredTransactions, uniqueTransactions]);

    return (
        <div className="bg-zinc-200 dark:bg-sage-500 min-h-screen flex flex-col items-center justify-items-stretch">
            <Header stats={stats} loading={liveLoading} error={liveError} isConnected={isConnected}/>

            <main className="w-[95%] md:w-[90%] xl:w-3/4 flex-grow grid grid-cols-1 md:grid-cols-2 gap-4 my-5">

                <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                        Распределение сумм транзакций
                    </h2>
                    <div className="flex-grow min-h-[300px]">
                        <TransactionHistogram transactions={uniqueTransactions} loading={loading} error={error} />
                    </div>
                </div>

                <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                        Статистика одобрения транзакций
                    </h2>
                    <div className="flex-grow min-h-[300px]">
                        <TransactionPieChart transactions={uniqueTransactions} loading={loading} error={error} />
                    </div>
                </div>

                <div className="col-span-1 md:col-span-2 flex justify-center p-4">
                    <div className="w-full max-w-[800px] bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                        <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                            Транзакции за последний час
                        </h2>
                        <div className="flex-grow min-h-[300px]">
                            <TransactionLineChart transactions={uniqueTransactions} loading={loading} error={error} />
                        </div>
                    </div>
                </div>

                <div className="col-span-1 md:col-span-2 pt-6 place-content-center">
                    <TransactionTable liveTransactions={displayedTransactions} error={error} loading={loading} search={searchTransactions} />
                </div>

                <div className="col-span-1 md:col-span-2 pt-6 bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                        Карта последних 20 транзакций
                    </h2>
                    <div className="flex-grow min-h-[550px]">
                        <TransactionMap transactions={displayedTransactions} />
                    </div>
                </div>
            </main>

            <footer className="m-4 w-5/6 py-4 grid grid-cols-1 md:grid-cols-3 gap-4 dark:text-sage-50">
                <h1 className="p-4 text-center text-xl font-mono font-bold">
                    Практика
                </h1>
                <h1 className="p-4 text-center text-xl font-mono font-bold">
                    СМП - Система медленных платежей
                </h1>
                <h1 className="p-4 text-center text-xl font-mono font-bold">
                    2026
                </h1>
            </footer>
        </div>
    );
}

export default App;
