enum TransactionStatus {
    APPROVED = "APPROVED",
    DECLINED = "DECLINED",
}

enum FilterStatus {
    ALL = "All",
    APPROVED = "Approved",
    DECLINED = "Declined"
}

enum TerminalType {
    POS = "POS",
    ATM = "ATM",
    ECOM = "ECOM",
}

export type Transaction = {
    id: string
    mti: string
    stan: string
    rrn: string
    pan: string
    processingCode: string
    processingTimeMs: number
    amount: number
    currencyCode: string
    terminalId: string
    terminalType?: TerminalType
    responseCode: string
    merchantId: string
    mcc: string
    acquirerId: string
    acquiringFee: number
    issuerId?: string
    status: TransactionStatus
    declineReason?: string
    authCode: string
    transmissionDateTime: string
    createdAt: string
}

export type DashboardStats = {
    totalTransactions: number
    approvedCount: number
    declinedCount: number
    approvalRate: number
    totalAmount: number
    averageAmount: number
    avgProcessingTimeMs: number
    transactionsPerMinute: number
}

export type Filter = {
    status: FilterStatus
    dateFrom?: string
    dateTo?: string
    bin?: string
    mcc?: string
}