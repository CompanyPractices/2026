import {KpiCards, KpiCardsStats} from "./KpiCards.tsx";

type HeaderProps = {
    stats: KpiCardsStats,
    loading: boolean,
    error: string | null,
};

export function Header({ stats, loading, error }: HeaderProps) {
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
    if (!stats){
        return <header className="flex flex-col items-center font-mono m-5">
            <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
            <div>Данные отсутствуют</div>
        </header>;
    }
    return <header className="flex flex-col items-center font-mono m-5">
        <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
        <KpiCards stats={stats} />
    </header>;
}
