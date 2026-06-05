import {Transaction} from "../types";
import {getStatusIcon} from "../utils/statusIcon.ts";
import {convertPenniesToRubles, formatTime, hidePan} from "../utils/format.ts";

type TransactionModalProps = {
    transaction: Transaction;
    onClose: () => void;
}

export function TransactionModal({ transaction, onClose }: TransactionModalProps) {
    const statusIconData = getStatusIcon(transaction.status);

    const rows = [
        {label: "STAN", value: transaction.stan },
        {label: "RRN", value: transaction.rrn },
        {label: "PAN", value: hidePan(transaction.pan) },
        {label: "Сумма", value: convertPenniesToRubles(transaction.amount) },
        {label: "Статус", value: (
            <div className="flex items-center gap-2">
                <statusIconData.icon className={statusIconData.color} size={statusIconData.size} />
                <span>{transaction.status}</span>
            </div>
        ),},
        {label: "Код авторизации", value: transaction.authCode || "—"  },
        {label: "Терминал", value: (
            <div>{transaction.terminalId} ({transaction.terminalType || "—"})</div>
        )},
        {label: "ID мерчанта", value: transaction.merchantId },
        {label: "MCC", value: transaction.mcc },
        {label: "ID экваера", value: transaction.acquirerId},
        {label: "ID эмитента", value: transaction.issuerId || "—" },
        {label: "Время", value: formatTime(transaction.transmissionDateTime) }
    ];

    return (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center z-50" onClick={onClose}>
            <div className="bg-white dark:bg-gray-900 rounded-2xl p-6 shadow-2xl max-w-md w-full mx-4 relative"
                 onClick={(e) => e.stopPropagation()} >
                <h3> Детали транзакции </h3>
                <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
                    {rows.map((row) => (
                            <div>
                                <dt key={row.label + '-label'} >
                                    {row.label}:
                                </dt>
                                <dd key={row.label + '-value'}>
                                    {row.value}
                                </dd>
                            </div>
                        ))}
                </div>
            </div>
        </div>
    );
}