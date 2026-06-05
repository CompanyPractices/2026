import { hidePan, convertPenniesToRubles, formatTime } from '../utils/format.ts';
import { getStatusIcon } from '../utils/statusIcon.ts';
import {TransactionStatus} from "../types";

export type TransactionTableRowData = {
    id: string,
    time: string,
    pan: string,
    amount: number,
    merchantId: string,
    status: TransactionStatus,
}

type TransactionTableProps = {
    transactions: TransactionTableRowData[]
}

export function TransactionTable({ transactions } : TransactionTableProps ){
    return (
        <div className="font-mono flex flex-col items-center">
            <h2 className="text-2xl font-bold mb-3 text-center drop-shadow-lg ">Последние 20 транзакций</h2>

            <div className="rounded-3xl overflow-hidden border-2 border-emerald-600 min-w-[50%] max-w-[75%] shadow-lg mb-5">
                <table className="table-auto w-full">
                    <tr className=" border-b-2 border-emerald-600 text-center text-semibold">
                        <th className="px-10 py-5"> Время </th>
                        <th className="px-10 py-5"> PAN </th>
                        <th className="px-10 py-5"> Сумма </th>
                        <th className="px-10 py-5"> Мерчант </th>
                        <th className="px-10 py-5"> Статус </th>
                    </tr>
                    {transactions.map((transaction) => {
                        const statusIconData = getStatusIcon(transaction.status);

                        return(
                            <tr key={transaction.id}
                                className=" text-center">
                                <td className="px-10 py-2"> {formatTime(transaction.time)} </td>
                                <td className="px-10 py-2"> {hidePan(transaction.pan)} </td>
                                <td className="px-10 py-2 text-right"> {convertPenniesToRubles(transaction.amount)} </td>
                                <td className="px-10 py-2 text-left"> {transaction.merchantId} </td>
                                <td className="px-10 py-2">
                                    <div className="flex justify-center" >
                                        <statusIconData.icon
                                            className={statusIconData.color}
                                            size={statusIconData.size}
                                            aria-hidden="true"
                                        />
                                        <span className="sr-only" >{statusIconData.label}</span>
                                    </div>
                                </td>
                            </tr>)})}
                </table>
            </div>
       </div>
    );
}