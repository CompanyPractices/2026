import { Transaction } from '../types/index.ts'
import { BarChart, XAxis, YAxis, Tooltip, CartesianGrid, Bar, ResponsiveContainer } from 'recharts'
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type TransactionHistogramProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}

export default function TransactionHistogram({transactions, loading, error}: TransactionHistogramProps){
    const { theme } = useContext(ThemeContext)!;
    const isDark = theme === 'dark';

    const textColor = isDark ? '#ECF6DA' : '#273338';
    const gridColor = isDark ? '#E5E7EB' : '#344148';
    const barColor = isDark ? 'oklch(79.2% 0.209 151.711)' : 'green'

    if (error) {
        return (
            <div className="text-center py-8 text-red-500 font-mono">
                Ошибка загрузки транзакций: {error}
            </div>
        )
    }

    if (loading) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Загрузка транзакций...
            </div>
        )
    }

    if (!loading && !error && transactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Транзакций не найдено
            </div>
        )
    }

    const histogramData: Record<number, number> = {};
    transactions.map((tx) => {
        histogramData[tx.amount] = (histogramData[tx.amount] || 0) + 1;
    })
    const txData = Object.entries(histogramData).map(([amount, count]) => ({amount, count})).sort((a, b) => a.amount.localeCompare(b.amount));

    return (
        <div className="w-full h-full">
            <ResponsiveContainer width="100%" height="100%">
                <BarChart
                    data={txData}
                    margin={{ top: 20, right: 30, left: 0, bottom: 20 }}
                >
                    <CartesianGrid strokeDasharray="3 3" stroke={gridColor}/>
                    <XAxis
                        dataKey="amount"
                        stroke={textColor}
                        tick={{ fill: textColor }}
                        tickLine={false}
                        label={{value: 'Суммы', position:"insideBottom", offset:-10 }}
                    />
                    <YAxis
                        dataKey="count"
                        allowDecimals={false}
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                    />
                    <Bar
                        dataKey="count"
                        fill={barColor}
                        activeBar={{
                            fill:"pink",
                            stroke: 'blue'
                        }}
                        radius={[10, 10, 0, 0]}
                    />
                    <Tooltip cursor={{ fill: 'oklch(92.8% 0.006 264.531)' }}/>
                </BarChart>
            </ResponsiveContainer>
        </div>
    )
}
