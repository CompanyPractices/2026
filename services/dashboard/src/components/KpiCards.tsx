import {convertPenniesToRubles} from "../utils/format.ts";

export type KpiCardsStats = {
    totalTransactions: number,
    approvalRate: number,
    totalAmount: number,
    avgProcessingTimeMs: number,
};

type KpiCardsProps = {
    stats: KpiCardsStats,
}

export function KpiCards( {stats }: KpiCardsProps ) {
    const kpiCards = [
        { label: 'Всего ТХ', value: stats.totalTransactions },
        { label: 'Одобрено', value: stats.approvalRate, unit: '%' },
        { label: 'Общая сумма', value: convertPenniesToRubles(stats.totalAmount) },
        { label: 'Среднее время', value: stats.avgProcessingTimeMs, unit: 'ms' },
    ];

    return (
        <div className="grid grid-cols-4 gap-10 m-5">
            {kpiCards.map((kpiCard) => (
                <div
                    key={kpiCard.label}
                    className="bg-emerald-300 rounded-xl p-4 text-xl items-center shadow-lg text-center"
                >
                    <p>{kpiCard.value} {kpiCard.unit}</p>
                    <p>{kpiCard.label}</p>
                </div>
            ))}
        </div>
    );
}
