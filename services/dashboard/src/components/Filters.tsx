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
            <div className="col-span-3" >Фильтр по таблице</div>
            <button type="submit">Найти</button>
            <button type="button" onClick={reset}>Сбросить</button>

            <div className="flex flex-col">
                <label htmlFor="status">Статус: </label>
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
                <label htmlFor="issuer">Банк эмитент: </label>
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
                <label htmlFor="mcc">MCC: </label>
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
                <label htmlFor="dateFrom">Начало даты: </label>
                <input value={filter.dateFrom ?? ''}
                       onChange={(e) => setFilter({
                           ...filter,
                           dateFrom: e.target.value || undefined
                       })}
                       id="dateFrom"
                       type="date"/>
            </div>

            <div className="flex flex-col">
                <label htmlFor="dateTo">Конец даты: </label>
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
