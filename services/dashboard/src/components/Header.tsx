import {KpiCards, KpiCardsStats} from "./KpiCards.tsx";

type HeaderProps = {
    stats: KpiCardsStats,
    loading: boolean,
    error: string | null,
    isConnected: boolean,
};

export function Header({ stats, loading, error, isConnected }: HeaderProps) {
    let content;

    if (error) {
        content = (
            <div className="text-center py-4 text-red-500 text-lg">
                Ошибка загрузки статистики: {error}
            </div>
        );
    } else if (loading) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg">
                Загрузка статистики...
            </div>
        );
    } else if (!stats) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg">
                Данные отсутствуют
            </div>
        );
    } else {
        content = <KpiCards stats={stats} />;
    }

    return (
        <header className="relative flex flex-col items-center font-mono m-5 w-full">

            <div className="absolute top-0 right-0 mr-8 mt-3 flex items-center gap-2">
                <span
                    className={`w-7 h-7 rounded-full drop-shadow-lg ${
                        isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-red-500 animate-pulse'
                    }`}
                    title={isConnected ? 'WebSocket подключен' : 'Соединение потеряно'}
                />
            </div>

            <h1 className="text-5xl font-semibold drop-shadow-lg mb-6">Dashboard</h1>
            {content}

        </header>
    );
}
