import { hidePan, convertPenniesToRubles, formatTime } from '../../utils/format';
import { getStatusIcon } from '../../utils/statusIcon';
import {TransactionTableRowData} from "./types";

type TransactionTableProps = {
    transactions: TransactionTbleRowData[]
}

export function TransactionTable({ transactions } : TransactionTableProps ){
    return (
        <div>
            <h2>Последние 20 транзакций</h2>

            <table>
                <thead> Последние 20 транзакций </thead>
                <tr>
                    <th> Время </th>
                    <th> PAN </th>
                    <th> Сумма </th>
                    <th> Мерчант </th>
                    <th> Статус </th>
                </tr>
                {transactions.map((transaction) => {
                    const statusIconData = getStatusIcon(transaction.status);

                    return(
                        <tr key={transaction.pan + transaction.time}>
                            <td> {formatTime(transaction.time)} </td>
                            <td> {hidePan(transaction.pan)} </td>
                            <td> {convertPenniesToRubles(transaction.amount)} </td>
                            <td> {transaction.merchantId} </td>
                            <td>
                                <div>
                                    <statusIconData.icon
                                        className={statusIconData.color}
                                        size={statusIconData.size}
                                        aria-hidden="true"
                                    />
                                    <span>{statusIconData.label}</span>
                                </div>
                            </td>
                        </tr>)})}
            </table>
       </div>
    );
}