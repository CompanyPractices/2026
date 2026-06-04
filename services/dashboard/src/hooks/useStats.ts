import fetchApi from "../api/client";
import { DashboardStats } from "../types/index.ts";

function useStats() {
    const [transactionStats, setTransactionStats] = useState<DashboardStats | null>(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        fetchApi<DashboardStats>("/api/dashboard/stats")
            .then((data) => {
                setTransactionStats(data);
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)});
    }, []);
    return {transactionStats, error, loading}
}