import { useEffect, useState } from 'react';
import {KpiCards} from "./components/KpiCards.tsx";
import LineChart from "./components/LineChart.tsx";
import PieChart from "./components/PieChart.tsx";
import TransactionTable from "./components/TransactionTable.tsx";
import TransactionModal from "./components/TransactionModal.tsx";

interface HealthResponse {
  status: string;
  service: string;
  version: string;
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/health')
      .then((res) => res.json())
      .then(setHealth)
      .catch((err) => setError(err.message));
  }, []);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full">
        <h1 className="text-2xl font-bold mb-4">SERVICE_NAME</h1>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
            ❌ Error: {error}
          </div>
        )}

        {health && (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-green-700 font-semibold">
              ✅ Status: {health.status}
            </p>
            <p className="text-green-600 text-sm mt-1">
              Service: {health.service}
            </p>
          </div>
        )}

        {!health && !error && (
          <div className="text-gray-400 text-center py-8">
            ⏳ Connecting...
          </div>
        )}
      </div>
      <div className="container mx-auto mt-8">
        <WidgetGrid />
      </div>
    </div>
  );
}

function WidgetGrid(){
  return (
      <div className="grid grid-cols-4 gap-4 h-screen">
        <div className="bg-blue-500">
            <KpiCards />
        </div>
        <div className="bg-blue-500">
            <KpiCards />
        </div>
        <div className="bg-blue-500">
            <KpiCards />
        </div>
        <div className="bg-blue-500">
            <KpiCards />
        </div>
        <div className="col-span-4 bg-blue-500">
            <LineChart />
        </div>
        <div className="col-span-4 bg-blue-500">
            <PieChart />
        </div>
        <div className="col-span-4 bg-blue-500">
            <TransactionTable />
        </div>
        <div className="col-span-4 bg-blue-500">
            <TransactionModal />
        </div>
      </div>
  );
}

export default App;
