import {KpiCards, KpiCardsStats} from "./KpiCards.tsx";
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type HeaderProps = {
    stats: KpiCardsStats,
    loading: boolean,
    error: string | null,
    isConnected: boolean,
};

export function Header({ stats, loading, error, isConnected }: HeaderProps) {
    const { theme, setTheme } = useContext(ThemeContext)!;

    let content;
    if (error) {
        content = (
            <div className="text-center py-4 text-red-500 text-lg">
                Ошибка загрузки статистики: {error}
            </div>
        );
    } else if (loading) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg dark:text-sage-100" >
                Загрузка статистики...
            </div>
        );
    } else if (!stats) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg dark:text-sage-50">
                Данные отсутствуют
            </div>
        );
    } else {
        content = <KpiCards stats={stats} />;
    }

    const isDark = theme === 'dark';

    return (
        <header className="relative flex flex-col items-center font-mono m-5 w-full">

            <div className="absolute top-0 left-0 ml-8 mt-3 flex items-center gap-2">
                <label className="group flex items-center gap-4 cursor-pointer select-none">
                    <div className="relative">
                        <input
                            type="checkbox"
                            role="switch"
                            checked={isDark}
                            onChange={() => setTheme(isDark ? 'light' : 'dark')}
                            className="peer appearance-none w-12 h-7 bg-zinc-400 checked:bg-sage-100 rounded-full cursor-pointer transition-colors outline-offset-4"
                        />
                        <div className="absolute top-0.5 left-0.5 w-6 h-6 bg-white rounded-full shadow-md transition-transform duration-200 peer-checked:translate-x-5 peer-checked:bg-sage-400"></div>
                    </div>
                </label>
            </div>

            <div className="absolute top-0 right-0 mr-8 mt-3 flex items-center gap-2">
                <span
                    className={`w-7 h-7 rounded-full drop-shadow-lg ${
                        isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-red-500 animate-pulse'
                    }`}
                    title={isConnected ? 'WebSocket подключен' : 'Соединение потеряно'}
                />
            </div>

            <h1 className="text-5xl font-semibold drop-shadow-lg mb-6 dark:text-sage-50">Dashboard</h1>
            {content}

        </header>
    );
}
