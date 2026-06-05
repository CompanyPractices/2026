import {KpiCards} from "./KpiCards.tsx";
import useStats from "../hooks/useStats.ts"

export function Header() {
    const {transactionStats, error, loading} = useStats();
    if (error){
        return <header className="flex flex-col items-center font-mono m-5">
            <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
            <div>Ошибка загрузки статистики: {error}</div>
        </header>;
    }
    if (loading){
        return <header className="flex flex-col items-center font-mono m-5">
            <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
            <div>Загрузка статистики...</div>
        </header>;
    }
    if (!transactionStats){
        return <header className="flex flex-col items-center font-mono m-5">
            <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
            <div>Данные отсутствуют</div>
        </header>;
    }
    return <header className="flex flex-col items-center font-mono m-5">
        <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
        <KpiCards stats={transactionStats} />
    </header>;
}