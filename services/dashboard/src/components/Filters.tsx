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
        <form onSubmit={filterSubmit} className="grid grid-cols-5 gap-4">
            <h2 className="col-span-3 text-lg font-bold drop-shadow-lg">
                Фильтр по таблице
            </h2>
            <button type="submit" className="text-lg font-bold drop-shadow-lg">
                Найти
            </button>
            <button type="button" onClick={reset} className="text-lg font-bold drop-shadow-lg">
                Сбросить
            </button>

            <div className="flex flex-col">
                <label htmlFor="status" className="text-base font-bold">
                    Статус:
                </label>
                <select value={filter.status ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            status: (e.target.value as FilterStatus) || undefined
                        })}
                        id="status">
                    <option value="">Выберите статус</option>
                    <option value="APPROVED">Одобрен</option>
                    <option value="DECLINED">Отклонен</option>
                </select>
            </div>

            <div className="flex flex-col">
                <label htmlFor="issuer" className="text-base font-bold">
                    Банк эмитент:
                </label>
                <select value={filter.issuerId ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            issuerId: e.target.value || undefined
                        })}
                        id="issuer">
                    <option value="">Выберите эмитента</option>
                    {Object.keys(issuers).map((issuerId) => {
                        return <option key={issuerId} value={issuerId}>{issuers[issuerId] || issuerId}</option>;
                    })}
                </select>
            </div>

            <div className="flex flex-col">
                <label htmlFor="mcc" className="text-base font-bold">
                    MCC:
                </label>
                <select value={filter.mcc ?? ''}
                        onChange={(e) => setFilter({
                            ...filter,
                            mcc: e.target.value || undefined
                        })}
                        id="mcc">
                    <option value="">Выберите MCC</option>
                    {Object.keys(mccNames).map((mccCode) => {
                        return <option key={mccCode} value={mccCode}>{mccNames[mccCode] || mccCode}</option>;
                    })}
                </select>
            </div>

            <div className="flex flex-col">
                <label htmlFor="dateFrom" className="text-base font-bold">
                    Начало даты:
                </label>
                <input value={filter.dateFrom ?? ''}
                       onChange={(e) => setFilter({
                           ...filter,
                           dateFrom: e.target.value || undefined
                       })}
                       id="dateFrom"
                       type="date"/>
            </div>

            <div className="flex flex-col">
                <label htmlFor="dateTo" className="text-base font-bold">
                    Конец даты:
                </label>
                <input value={filter.dateTo ?? ''}
                       onChange={(e) => setFilter({
                           ...filter,
                           dateTo: e.target.value || undefined
                       })}
                       id="dateTo"
                       type="date"/>
            </div>
        </form>
    )
}
