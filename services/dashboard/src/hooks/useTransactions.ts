import { useEffect, useState, useCallback } from 'react';
import fetchApi from "../api/client";
import { Transaction } from "../types/index.ts";
import { Filter } from "../types/index.ts";
import { SearchResponse } from "../types/index.ts";

function useTransactions() {
    const [transactions, setTransactions] = useState<Transaction[] | null>(null);
    const [filteredTransactions, setFilteredTransactions] = useState<Transaction[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        fetchApi<Transaction[]>("/api/dashboard/recent?limit=20")
            .then((data) => {
                setTransactions(data);
                setFilteredTransactions(data)
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)});
    }, []);

    const searchTransactions = useCallback((filter: Filter) => {
        const isEmpty = !filter || Object.values(filter).every(v => v === undefined || v === '');
        if (isEmpty && transactions){
            setFilteredTransactions(transactions)
        }

        setLoading(true);
        setError(null);
        const requestParams = new URLSearchParams();
        if (filter.status){
            requestParams.append('status', filter.status);
        }
        if (filter.dateFrom){
            requestParams.append('dateFrom', filter.dateFrom.slice(0, 10));
        }
        if (filter.dateTo){
            requestParams.append('dateTo', filter.dateTo.slice(0, 10));
        }
        if (filter.issuerId){
            requestParams.append('issuerId', filter.issuerId);
        }
        if (filter.mcc){
            requestParams.append('mcc', filter.mcc);
        }
        fetchApi<SearchResponse>(`/api/transactions/search?${requestParams.toString()}`)
            .then((data) => {
                setFilteredTransactions(data.transactions);
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)
            });

    }, [transactions]);

    return {transactions, filteredTransactions, error, loading, searchTransactions}
}

export default useTransactions;
