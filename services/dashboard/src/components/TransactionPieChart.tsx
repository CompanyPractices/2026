import { Pie, PieChart, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts';
import { Transaction } from '../types/index.ts'

type PieChartProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}
export default function TransactionPieChart({transactions, loading, error}: PieChartProps) {
    const approved = transactions?.filter((s) => s.status === 'APPROVED').length || 0;
    const declined = transactions?.filter((s) => s.status === 'DECLINED').length || 0;

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
            <PieChart>
                <Pie
                    data={[
                        { name: 'APPROVED', value: approved },
                        { name: 'DECLINED', value: declined },
                    ]}
                    dataKey="value" isAnimationActive={true}
                    cy="40%"
                >
                    <Cell fill="red" />
                    <Cell fill="lawnGreen" />
                </Pie>
                <Legend
                    layout="horizontal"
                    align="center"
                    verticalAlign="bottom"
                    iconType="circle"
                    formatter={(value) => {
                        const count = value === 'APPROVED' ? approved : declined;
                        const label = value === 'APPROVED' ? 'Одобрено' : 'Отклонено';
                        return (
                            <span className="text-zinc-700 font-mono">
                                { `${label}(${count})`}
                            </span>
                        )
                    }
                    }
                />
                <Tooltip/>
            </PieChart>
        </ResponsiveContainer>
    )
}
