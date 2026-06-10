import { useState, useEffect, useRef } from 'react';
import { Transaction } from '../types';
import useStats from "./useStats.ts";

type KpiLiveStats = {
    totalTx: number;
    approvedTx: number;
    totalAmount: number;
    totalTime: number;
};

export function useLiveStats(liveTransactions: Transaction[]) {
    const { transactionStats, error, loading } = useStats();

    const defaultStats: KpiLiveStats = {
        totalTx: 0,
        approvedTx: 0,
        totalAmount: 0,
        totalTime: 0
    };

    const [stats, setStats] = useState<KpiLiveStats>(defaultStats);

    const statsRef = useRef<KpiLiveStats>(defaultStats);

    useEffect(() => {
        if (transactionStats) {
            const mappedStats: KpiLiveStats = {
                totalTx: transactionStats.totalTransactions || 0,
                approvedTx: transactionStats.approvedCount || 0,
                totalAmount: transactionStats.totalAmount || 0,
                totalTime: transactionStats.avgProcessingTimeMs * transactionStats.totalTransactions || 0,
            };

            statsRef.current = mappedStats;
            setStats(mappedStats);
        }
    }, [transactionStats]);

    useEffect(() => {
        if (liveTransactions.length > 0) {
            const newTx = liveTransactions[0];

            const current = statsRef.current;

            const nextStats: KpiLiveStats = {
                totalTx: current.totalTx + 1,
                approvedTx: current.approvedTx + (newTx.status === 'APPROVED' ? 1 : 0),
                totalAmount: current.totalAmount + newTx.amount,
                totalTime: current.totalTime + (newTx.processingTimeMs || 0),
            };

            statsRef.current = nextStats;
            setStats(nextStats);
        }
    }, [liveTransactions]);

    const approvalRate = stats.totalTx > 0
        ? Math.round((stats.approvedTx / stats.totalTx) * 100)
        : 0;

    const avgProcessingTimeMs = stats.totalTx > 0
        ? Math.round(stats.totalTime / stats.totalTx)
        : 0;

    return {
        stats: {
            totalTransactions: stats.totalTx,
            approvalRate,
            totalAmount: stats.totalAmount,
            avgProcessingTimeMs,
        },
        liveTransactions,
        loading,
        error,
    };
}
