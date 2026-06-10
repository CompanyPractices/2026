import useWebSocket from '../hooks/useWebSocket'
import { Pie, PieChart, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts';
import { Transaction } from '../types/index.ts'

type PieChartProps = {
    transactions: Transaction[];
    isConnected: Boolean;
}
export default function TransactionPieChart({transactions, isConnected}: PieChartProps) {
    const approved = transactions?.filter((s) => s.status === 'APPROVED').length || 0;
    const declined = transactions?.filter((s) => s.status === 'DECLINED').length || 0;

    return (
        <ResponsiveContainer width="80%" height={300} className="mx-auto my-auto" >
            {!isConnected &&
                <div className="grid place-content-center text-zinc-700 font-mono" >Ожидание транзакций...</div>
            }
            {isConnected &&
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
            }
        </ResponsiveContainer>
    )
}
