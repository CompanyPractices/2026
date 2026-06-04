import {TransactionStatus} from "../../types";

export type TransactionTableRowData = {
    time: string,
    pan: string,
    amount: string,
    merchantId: string,
    status: TransactionStatus,
}