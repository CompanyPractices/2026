import React, {useState, useEffect} from "react";

function useStats() {
    const [totalTransactions, setTotalTransactions] = useState(0);
    const [approvalRate, setApprovalRate] = useState(0);
    const [totalAmount, setTotalAmount] = useState(0);
    const [avgTime, setAvgTime] = useState(0);

    useEffect(() => {
        fetchStats()
    }
}