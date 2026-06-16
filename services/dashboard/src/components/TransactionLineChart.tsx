import { LineChart, CartesianGrid, Line, XAxis, YAxis, ResponsiveContainer, Tooltip } from 'recharts'
import { Transaction } from '../types/index.ts'
import { subHours } from 'date-fns';
import { formatInTimeZone } from 'date-fns-tz';
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type LineChartProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}

export default function TransactionLineChart({transactions, loading, error} : LineChartProps) {
    const { theme } = useContext(ThemeContext)!;
    const isDark = theme === 'dark';

    const textColor = isDark ? '#ECF6DA' : '#273338';
    const gridColor = isDark ? '#E5E7EB' : '#344148';
    const lineColor = isDark ? 'oklch(79.2% 0.209 151.711)' : 'green'


    function prepareData(transactions: Transaction[]){
        const hourAgo = subHours(new Date(), 1)
        const txCount: Record<string, number> = {};
        transactions.filter((tx) => new Date(tx.transmissionDateTime) >= hourAgo).map((tr) => {
            const time = formatInTimeZone(tr.transmissionDateTime, 'UTC', 'HH:mm');
            txCount[time] = (txCount[time] || 0) + 1;
        });
        return Object.entries(txCount).map(([name, count]) => ({name, count})).sort((a, b) => a.name.localeCompare(b.name))
    }

    const txData = prepareData(transactions)

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

    return (
        <div className="w-full h-full">
            <ResponsiveContainer width="100%" height="100%">
                <LineChart
                    data = {txData}
                    margin={{ top: 30, right: 30, left: 20, bottom: 20 }}
                >
                    <CartesianGrid strokeDasharray="3 3" stroke={gridColor}/>
                    <XAxis
                        dataKey="name"
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                        label={{value: 'Время (UTC)', position:"insideBottom", offset:-5 }}
                    />
                    <YAxis
                        width={30}
                        dataKey="count"
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                        allowDecimals={false}
                        label={{value: 'TX/min', position:"top", offset:15}}
                    />
                    <Tooltip/>
                    <Line
                        type="monotone"
                        dataKey="count"
                        stroke={lineColor}
                        animationDuration={2000}
                    />
                </LineChart>
            </ResponsiveContainer>
        </div>
    )
}
