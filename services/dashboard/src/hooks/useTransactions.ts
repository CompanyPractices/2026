import { useEffect, useState } from 'react';
import fetchApi from "../api/client";
import { Transaction } from "../types/index.ts";
import { Filter } from "../types/index.ts";
import { SearchResponse } from "../types/index.ts";

function useTransactions() {
    const [transactions, setTransactions] = useState<Transaction[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        fetchApi<Transaction[]>("/api/dashboard/recent?limit=20")
            .then((data) => {
                setTransactions(data);
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)});
    }, []);

    function searchTransactions(filter: Filter) {
        setLoading(true);
        setError(null);
        const requestParams = new URLSearchParams();
        if (filter.status){
            requestParams.append('status', filter.status);
        }
        if (filter.dateFrom){
            requestParams.append('dateFrom', filter.dateFrom);
        }
        if (filter.dateTo){
            requestParams.append('dateTo', filter.dateTo);
        }
        if (filter.bin){
            requestParams.append('pan', filter.bin);
        }
        if (filter.mcc){
            requestParams.append('mcc', filter.mcc);
        }
        fetchApi<SearchResponse>(`/api/transactions/search?${requestParams.toString()}`)
            .then((data) => {
                setTransactions(data.transactions);
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)
            });

    }

    return {transactions, error, loading, searchTransactions}
}

export default useTransactions;