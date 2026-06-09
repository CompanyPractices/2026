import {KpiCards, KpiCardsStats} from "./KpiCards.tsx";

type HeaderProps = {
    stats: KpiCardsStats,
    loading: boolean,
    error: string | null,
    isConnected: boolean,
};

export function Header({ stats, loading, error, isConnected }: HeaderProps) {
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
    return (
        <header className="relative flex flex-col items-center font-mono m-5 w-full">

            <div className="absolute top-0 right-0 mr-8 mt-3 flex items-center gap-2 ">
                <span
                    className={`w-7 h-7 rounded-full drop-shadow-lg ${
                        isConnected
                            ? 'bg-emerald-500 animate-pulse'
                            : 'bg-red-500 animate-pulse'
                    }`}
                    title={isConnected ? 'WebSocket подключен' : 'Соединение потеряно'}
                />
            </div>

            <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>

            <KpiCards stats={stats} />
        </header>
    );
}
