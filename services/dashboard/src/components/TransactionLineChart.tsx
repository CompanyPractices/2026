import { LineChart, CartesianGrid, Line, XAxis, YAxis, ResponsiveContainer } from 'recharts'
import { Transaction } from '../types/index.ts'
import { format, getHours } from 'date-fns';

type LineChartProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}

export default function TransactionLineChart({transactions, loading, error} : LineChartProps) {
    const currentHour = getHours(new Date());

    function prepareData(transactions: Transaction[]){
        const txCount: Record<string, number> = {};
        transactions.filter((tx) => getHours(tx.transmissionDateTime) === currentHour).map((tr) => {
            const time = format(tr.transmissionDateTime, 'HH:mm');
            txCount[time] = (txCount[time] || 0) + 1;
        });
        return Object.entries(txCount).map(([name, count]) => ({name, count})).sort((a, b) => a.name.localeCompare(b.name))
    }

    const txData = prepareData(transactions)

    if (error) {
        return (
            <div className="text-center py-8 text-red-500">
                Ошибка загрузки транзакций: {error}
            </div>
        )
    }

    if (loading) {
        return (
            <div className="text-center py-8 text-gray-500">
                Загрузка транзакций...
            </div>
        )
    }

    if (!loading && !error && transactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500">
                Транзакций не найдено
            </div>
        )
    }

    return (
        <ResponsiveContainer width="80%" height={300} className="mx-auto my-auto" >
            <LineChart
                data = {txData}
            >
                <CartesianGrid strokeDasharray="3 3"/>
                <XAxis
                    dataKey="name"
                    stroke="red"
                />
                <YAxis
                    dataKey="count"
                    stroke="blue"
                />
                <Line
                    type="monotone"
                    dataKey="count"
                />
            </LineChart>
        </ResponsiveContainer>
    )
}
