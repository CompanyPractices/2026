import { FormEvent, useState } from 'react';
import { Filter, FilterStatus } from "../types";

type FilterProps = {
    issuers: Record<string, string>
    mccNames: Record<string, string>
    onSearch: (filter: Filter) => void;
}

export function Filters({issuers, mccNames, onSearch}: FilterProps) {
    const [filter, setFilter] = useState<Filter>({})

    function filterSubmit(e: FormEvent) {
        e.preventDefault();
        onSearch(filter)
    }

    function reset(){
        setFilter({})
        onSearch({})
    }

    return (
        <form onSubmit={filterSubmit}>
            <div className="flex flex-wrap items-center gap-4 mb-4 dark:text-sage-50">
                <span className="font-bold flex-grow">Фильтр по таблице</span>
                <button
                    className="dark:bg-sage-400 dark:border-sage-200 dark:hover:bg-sage-300 border rounded px-3 py-1 transition-colors"
                    type="submit"
                >
                    Найти
                </button>
                <button
                    className="dark:bg-sage-400 dark:border-sage-200 dark:hover:bg-sage-300 border rounded px-3 py-1 transition-colors"
                    type="button"
                    onClick={reset}
                >
                    Сбросить
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4">
                <div className="flex flex-col dark:text-sage-50">
                    <label htmlFor="status">Статус:</label>
                    <select
                        value={filter.status ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            status: (e.target.value as FilterStatus) || undefined
                        })}
                        id="status"
                        className="dark:bg-sage-400 dark:text-sage-50 dark:border-sage-200 border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sage-50"
                    >
                        <option value="">Все</option>
                        <option value="APPROVED">Одобрен</option>
                        <option value="DECLINED">Отклонен</option>
                    </select>
                </div>

                <div className="flex flex-col dark:text-sage-50">
                    <label htmlFor="issuer">Банк эмитент:</label>
                    <select
                        value={filter.issuerId ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            issuerId: e.target.value || undefined
                        })}
                        id="issuer"
                        className="dark:bg-sage-400 dark:text-sage-50 dark:border-sage-200 border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sage-50"
                    >
                        <option value="">Все</option>
                        {Object.keys(issuers).map((issuerId) => (
                            <option key={issuerId} value={issuerId}>
                                {issuers[issuerId] || issuerId}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="flex flex-col dark:text-sage-50">
                    <label htmlFor="mcc">MCC:</label>
                    <select
                        value={filter.mcc ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            mcc: e.target.value || undefined
                        })}
                        id="mcc"
                        className="dark:bg-sage-400 dark:text-sage-50 dark:border-sage-200 border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sage-50"
                    >
                        <option value="">Все</option>
                        {Object.keys(mccNames).map((mccCode) => (
                            <option key={mccCode} value={mccCode}>
                                {mccNames[mccCode] || mccCode}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="flex flex-col dark:text-sage-50">
                    <label htmlFor="dateFrom">Начало даты:</label>
                    <input
                        value={filter.dateFrom ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            dateFrom: e.target.value || undefined
                        })}
                        id="dateFrom"
                        type="date"
                        className="dark:bg-sage-400 dark:text-sage-50 dark:border-sage-200 border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sage-50"
                    />
                </div>

                <div className="flex flex-col dark:text-sage-50">
                    <label htmlFor="dateTo">Конец даты:</label>
                    <input
                        value={filter.dateTo ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            dateTo: e.target.value || undefined
                        })}
                        id="dateTo"
                        type="date"
                        className="dark:bg-sage-400 dark:text-sage-50 dark:border-sage-200 border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sage-50"
                    />
                </div>
            </div>
        </form>
    );
}
