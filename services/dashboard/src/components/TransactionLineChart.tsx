import { LineChart, CartesianGrid, Line, XAxis, YAxis, ResponsiveContainer } from 'recharts'
import { Transaction } from '../types/index.ts'
import { format, getHours } from 'date-fns';

type LineChartProps = {
    transactions: Transaction[];
    isConnected: boolean;
}

export default function TransactionLineChart({transactions, isConnected} : LineChartProps) {
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
    console.log("Данные для графика:", txData);

    return (
        <ResponsiveContainer width="80%" height={300} className="mx-auto my-auto" >
            {!isConnected &&
                <div className="grid place-content-center text-zinc-700 font-mono" >Ожидание транзакций...</div>
            }
            {isConnected && txData.length === 0 &&
                <div className="grid place-content-center text-zinc-700 font-mono" >Нет новых транзакций за последний час</div>
            }
            {isConnected && txData.length > 0 &&
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
            }
        </ResponsiveContainer>
    )
}
