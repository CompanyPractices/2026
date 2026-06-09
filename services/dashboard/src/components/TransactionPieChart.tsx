// import useWebSocket from '../hooks/useWebSocket'
import { Pie, PieChart, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts';

export default function TransactionPieChart() {
    // const {liveTransactions, isConnected} = useWebSocket();
    const approved = 100;
    const declined = 200;
    //
    // if (!isConnected) {
    //     return (
    //         <div>Нет подключения</div>
    //     )
    // }
    //
    // liveTransactions.Map(transaction => {
    //     if (transaction.status == 'APPROVED') approved++;
    //     if (transaction.status == 'DECLINED') declined++;
    // })

    return (
        <ResponsiveContainer width="80%" height={300} className="mx-auto my-auto" >
            <PieChart>
                <Pie
                    percent
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
