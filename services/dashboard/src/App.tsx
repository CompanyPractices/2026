import TransactionLineChart from "./components/TransactionLineChart.tsx";
import {Header} from "./components/Header.tsx";
import TransactionPieChart from "./components/TransactionPieChart.tsx";
import {TransactionTable} from "./components/TransactionTable.tsx";
import {useLiveStats} from "./hooks/useLiveStats.ts";
import {useWebSocket} from "./hooks/useWebSocket.ts";
import useTransactions from './hooks/useTransactions.ts'

function App() {
    const { liveTransactions, isConnected } = useWebSocket();
    const { transactions: initialTransactions, loading, error, searchTransactions } = useTransactions();
    const { stats, loading: liveLoading, error: liveError } = useLiveStats(liveTransactions);

    const allTransactions = [
        ...liveTransactions,
        ...(initialTransactions || []),
    ];

    const uniqueTransactions = allTransactions.filter((tx, index, self) =>
        index === self.findIndex(t => t.id === tx.id)
    );

    const displayedTransactions = uniqueTransactions.slice(0, 20);

    return (
        <div className="bg-zinc-200 min-h-screen flex flex-col items-center justify-items-stretch">
            <Header stats={stats} loading={liveLoading} error={liveError} isConnected={isConnected}/>
            <main className="w-2/3 flex-grow grid grid-cols-4 gap-4">
                <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-lg place-content-center">
                    <TransactionLineChart transactions={uniqueTransactions} loading={loading} error={error} />
                </div>
                <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-lg place-content-center">
                    <TransactionPieChart transactions={uniqueTransactions} loading={loading} error={false}/>
                </div>
                <div className="col-span-4 m-4 place-content-center">
                    <TransactionTable liveTransactions={displayedTransactions} error={error} loading={loading} search={searchTransactions} />
                </div>
            </main>

            <footer className="rounded-2xl m-4 w-5/6 h-24 grid grid-cols-3 gap-4">
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
