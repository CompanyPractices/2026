import {DashboardStats} from "../types";

type KpiCardsProps = {
    stats: DashboardStats;
};

export function KpiCards( { stats } : KpiCardsProps) {
    const kpiCards = [
        { label: 'Всего ТХ', value: stats.totalTransactions },
        { label: 'Одобрено', value: stats.approvalRate, unit: '%' },
        { label: 'Общая сумма',
            value: (stats.totalAmount / 100).toLocaleString('ru-RU', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
            }).replace(',', '.') + ' ₽'
        },
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