import { FormEvent, useState } from 'react';
import { Filter, FilterStatus } from "../types";
import { Listbox } from '@headlessui/react'

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
        <form onSubmit={filterSubmit} className="grid grid-cols-5 gap-4 font-mono">
            <div className="col-span-3 text-lg font-bold drop-shadow-lg dark:text-sage-50" >
                Фильтр по таблице
            </div>
            <button className="text-lg font-bold drop-shadow-lg bg-emerald-400 dark:bg-sage-200 rounded-lg dark:text-sage-50" type="submit">
                Найти
            </button>
            <button className="text-lg font-bold drop-shadow-lg bg-emerald-400 dark:bg-sage-200 rounded-lg dark:text-sage-50" type="button" onClick={reset}>
                Сбросить
            </button>

            <div className="flex flex-col relative w-full max-w-xs text-zinc-900 dark:text-sage-50">
                <label htmlFor="status" className="text-base font-bold mb-1">
                    Статус:
                </label>
                <Listbox value={filter.status ?? ''}
                         onChange={(val) => setFilter({
                             ...filter,
                             status: (val as FilterStatus) || undefined
                         })}>
                    <Listbox.Button className="border border-zinc-300 dark:border-sage-200
                    rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none
                    focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer
                    p-2 w-full text-left text-sm transition-all">
                        {filter.status === 'APPROVED' && 'Одобрен'}
                        {filter.status === 'DECLINED' && 'Отклонен'}
                        {!filter.status && 'Выберите статус'}
                    </Listbox.Button>

                    <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800
                    p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                        <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Выберите статус</Listbox.Option>
                        <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="APPROVED">Одобрен</Listbox.Option>
                        <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="DECLINED">Отклонен</Listbox.Option>
                    </Listbox.Options>
                </Listbox>
            </div>

            <div className="flex flex-col relative w-full max-w-xs text-zinc-900 dark:text-sage-50">
                <label htmlFor="issuer" className="text-base font-bold mb-1">
                    Банк эмитент:
                </label>
                <Listbox value={filter.issuerId ?? ''}
                         onChange={(val) => setFilter({
                             ...filter,
                             issuerId: val || undefined
                         })}>
                    <Listbox.Button className="border border-zinc-300 dark:border-sage-200
                    rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none
                    focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer
                    p-2 w-full text-left text-sm transition-all">
                        {filter.issuerId ? (issuers[filter.issuerId] || filter.issuerId) : 'Выберите эмитента'}
                    </Listbox.Button>

                    <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800
                    p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                        <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Выберите эмитента</Listbox.Option>
                        {Object.keys(issuers).map((issuerId) => {
                            return (<Listbox.Option
                                key={issuerId}
                                value={issuerId}
                                className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                {issuers[issuerId] || issuerId}
                            </Listbox.Option>);
                        })}
                    </Listbox.Options>
                </Listbox>
            </div>

            <div className="flex flex-col relative w-full max-w-xs text-zinc-900 dark:text-sage-50">
                <label htmlFor="mcc" className="text-base font-bold mb-1">
                    MCC:
                </label>
                <Listbox value={filter.mcc ?? ''}
                         onChange={(val) => setFilter({
                             ...filter,
                             mcc: val || undefined
                         })}>
                    <Listbox.Button className="border border-zinc-300 dark:border-sage-200
                    rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none
                    focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer
                    p-2 w-full text-left text-sm transition-all">
                        {filter.mcc ? (mccNames[filter.mcc] || filter.mcc) : 'Выберите категорию'}
                    </Listbox.Button>

                    <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800
                    p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                        <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Выберите категорию</Listbox.Option>
                        {Object.keys(mccNames).map((mccCode) => {
                            return (<Listbox.Option
                                key={mccCode}
                                value={mccCode}
                                className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                {mccNames[mccCode] || mccCode}
                            </Listbox.Option>);
                        })}
                    </Listbox.Options>
                </Listbox>
            </div>

            <div className="flex flex-col relative w-full max-w-xs text-zinc-900 dark:text-sage-50">
                <label htmlFor="dateFrom" className="text-base font-bold mb-1">
                    Начало даты:
                </label>
                <input className="border border-zinc-300 dark:border-sage-200
                    rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none
                    focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer
                    p-2 w-full text-left text-sm transition-all"
                       value={filter.dateFrom ?? ''}
                       onChange={(e) => setFilter({
                           ...filter,
                           dateFrom: e.target.value || undefined
                       })}
                       id="dateFrom"
                       type="date"/>
            </div>

            <div className="flex flex-col relative w-full max-w-xs text-zinc-900 dark:text-sage-50">
                <label htmlFor="dateTo" className="text-base font-bold mb-1">
                    Конец даты:
                </label>
                <input className="border border-zinc-300 dark:border-sage-200
                    rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none
                    focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer
                    p-2 w-full text-left text-sm transition-all"
                       value={filter.dateTo ?? ''}
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
