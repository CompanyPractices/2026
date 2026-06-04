import {TransactionStatus} from "../../types";

export type TransactionTableRowData = {
    time: string,
    pan: string,
    amount: number,
    merchantId: string,
    status: TransactionStatus,
}