import TransactionLineChart from "./components/TransactionLineChart.tsx";
import {Header} from "./components/Header.tsx";
import TransactionPieChart from "./components/TransactionPieChart.tsx";
import {TransactionTable} from "./components/TransactionTable.tsx";
import {useLiveStats} from "./hooks/useLiveStats.ts";
import {useWebSocket} from "./hooks/useWebSocket.ts";
import useTransactions from './hooks/useTransactions.ts'
import { useMemo } from 'react'

function App() {
    const { liveTransactions, isConnected } = useWebSocket();
    const { transactions: initialTransactions, filteredTransactions, loading, error, searchTransactions } = useTransactions();
    const { stats, loading: liveLoading, error: liveError } = useLiveStats(liveTransactions);

    const uniqueTransactions = useMemo(() => {
        const allTransactions = [
            ...liveTransactions,
            ...(initialTransactions || []),
        ];
        return allTransactions.filter((tx, index, self) =>
            index === self.findIndex(t => t.id === tx.id));
    }, [liveTransactions, initialTransactions]);

    const displayedTransactions = useMemo(() => {
        const isSearch = filteredTransactions !== initialTransactions;
        if (isSearch){
            const filtered = filteredTransactions || [];
            const uniqueFiltered = filtered.filter((tx, index, self) =>
                index === self.findIndex(t => t.id === tx.id)
            );
            return uniqueFiltered.slice(0, 20);
        }
        return uniqueTransactions.slice(0, 20)
    }, [filteredTransactions, uniqueTransactions, initialTransactions]);

    return (
        <div className="bg-zinc-200 dark:bg-sage-500 min-h-screen flex flex-col items-center justify-items-stretch">
            <Header stats={stats} loading={liveLoading} error={liveError} isConnected={isConnected}/>
            <main className="w-2/3 flex-grow grid grid-cols-4 gap-4">

                <div className="col-span-2 bg-zinc-300 dark:bg-sage-400 m-4 rounded-lg shadow-lg flex flex-col p-4">
                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                        Транзакции за последний час
                    </h2>
                    <div className="flex-grow min-h-[300px]">
                        <TransactionLineChart transactions={uniqueTransactions} loading={loading} error={error} />
                    </div>
                </div>

                <div className="col-span-2 bg-zinc-300 dark:bg-sage-400 m-4 rounded-lg shadow-lg flex flex-col p-4">
                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                        Статистика одобрения транзакций
                    </h2>
                    <div className="flex-grow min-h-[300px]">
                        <TransactionPieChart transactions={uniqueTransactions} loading={loading} error={error} />
                    </div>
                </div>

                <div className="col-span-4 pt-6 m-4 place-content-center">
                    <TransactionTable liveTransactions={displayedTransactions} error={error} loading={loading} search={searchTransactions} />
                </div>
            </main>

            <footer className="rounded-2xl m-4 w-5/6 h-24 grid grid-cols-3 gap-4 dark:text-sage-50">
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
