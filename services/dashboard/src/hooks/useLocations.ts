import { useMemo } from 'react';
import { Transaction } from '../types';
import { getTransactionLocation } from '../utils/geoGenerator';

export type TransactionWithLocation = {
    city: string;
    coordinates: [number, number];
    transaction: Transaction;
};

export function useLocations(transactions: Transaction[]): TransactionWithLocation[] {
    return useMemo(() => {
        return transactions.map((tx) => {
            const location = getTransactionLocation(tx.issuerId);
            return {
                id: tx.id,
                city: location.city,
                coordinates: location.coordinates,
                transaction: tx,
            };
        });
    }, [transactions]);
}
