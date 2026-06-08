import { FormEvent, useState } from 'react';
import { Filter } from "../types";

const IssuersId = ['ISS001', 'ISS002', 'ISS003', 'ISS004', 'ISS005', 'ISS006', 'ISS007', 'ISS008', 'ISS009', 'ISS010']
const Mcc = ['5411', '5812', '5541', '5912', '5732', '5651', '4111', '4511', '7011', '7832', '5942', '4829', '5999']

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

    return (
        <form onSubmit={filterSubmit}>
            <div>Фильтр по таблице</div>
            <label htmlFor="status">Статус: </label>
            <select value={filter.status ?? ''}
                    onChange={(e) => setFilter({
                        ...filter,
                        status: e.target.value || undefined
                    })}
                    id="status">
                <option value="">Выберите статус</option>
                <option value="APPROVED">Одобренный</option>
                <option value="DECLINED">Отклоненный</option>
            </select>

            <label htmlFor="issuer">Банк эмитент: </label>
            <select value={filter.issuerId ?? ''}
                    onChange={(e) => setFilter({
                        ...filter,
                        issuerId: e.target.value || undefined
                    })}
                    id="issuer">
                <option value="">Выберите эмитента</option>
                {IssuersId.map((issuerId) => {
                    return <option key={issuerId} value={issuerId}>{issuers[issuerId] || issuerId}</option>;
                })}
            </select>

            <label htmlFor="mcc">MCC: </label>
            <select value={filter.mcc ?? ''}
                    onChange={(e) => setFilter({
                        ...filter,
                        mcc: e.target.value || undefined
                    })}
                    id="mcc">
                <option value="">Выберите MCC</option>
                {Mcc.map((mccCode) => {
                    return <option key={mccCode} value={mccCode}>{mccNames[mccCode] || mccCode}</option>;
                })}
            </select>

            <label htmlFor="dateFrom">Начало даты: </label>
            <input value={filter.dateFrom ?? ''}
                   onChange={(e) => setFilter({
                       ...filter,
                       dateFrom: e.target.value || undefined
                   })}
                   id="dateFrom"
                   type="date"/>

            <label htmlFor="dateTo">Конец даты: </label>
            <input value={filter.dateTo ?? ''}
                   onChange={(e) => setFilter({
                       ...filter,
                       dateTo: e.target.value || undefined
                   })}
                   id="dateTo"
                   type="date"/>

            <button type="submit">Найти</button>
        </form>
    )
}
