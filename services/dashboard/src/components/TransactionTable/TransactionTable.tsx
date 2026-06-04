//import { hidePan, convertPenniesToRubles, formatTime, formatDateTime } from '../../utils/format';
import {TransactionTableRowData} from "./types";

type TransactionTableProps = {
    transactions: TransactionTableRowData[]
}

export function TransactionTable({ transactions } : TransactionTableProps ){
    return <div>
        <table>
            <thead> Последние 20 транзакций </thead>
            <tr>
                <th> Время </th>
                <th> PAN </th>
                <th> Сумма </th>
                <th> Мерчант </th>
                <th> Статус </th>
            </tr>
            {transactions.map((transaction) => (
                <tr>
                    <td> {transaction.time} </td>
                    <td> {transaction.pan} </td>
                    <td> {transaction.amount} </td>
                    <td> {transaction.merchantId} </td>
                    <td> {transaction.status} </td>
                </tr>
            ))
            }
        </table>
    </div>;
}