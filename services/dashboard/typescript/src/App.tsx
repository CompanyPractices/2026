import { useEffect, useState } from 'react';
import KpiCards from "./components/KpiCards.tsx";
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
    <div className="bg-zinc-200 min-h-screen flex flex-col items-center justify-items-stretch">
      <header className="rounded-2xl m-8 w-5/6 bg-violet-400 shadow-lg p-8">
        <h1 className="m-4 text-center text-5xl font-sans font-bold text-zinc-100">СМП — Симулятор процессингового центра</h1>

        {error && (
          <div className="m-8 bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
            ❌ Error: {error}
          </div>
        )}

        {health && (
          <div className="m-8 bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-green-700 font-semibold">
              ✅ Status: {health.status}
            </p>
            <p className="text-green-600 text-sm mt-1">
              Service: {health.service}
            </p>
          </div>
        )}

        {!health && !error && (
          <div className="m-4 text-gray-400 text-center py-8">
            ⏳ Connecting...
          </div>
        )}
          <KpiCards />
      </header>

      <main className="w-2/3 m-12 grid grid-cols-4 gap-4 h-screen m-4">
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <LineChart />
          </div>
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <PieChart />
          </div>
          <div className="col-span-4 bg-lime-300 m-4 rounded-lg shadow-xl place-content-center">
              <TransactionTable />
          </div>
          <div className="col-span-4 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <TransactionModal />
          </div>
      </main>

      <footer className="rounded-2xl m-8 w-5/6 bg-violet-400 w-full h-24 grid grid-cols-3 gap-4">
          <h1 className="p-8 text-center text-xl font-sans font-bold text-zinc-100">
              Практика
          </h1>
          <h1 className="p-8 text-center text-xl font-sans font-bold text-zinc-100">
              СМП - Система медленных платежей
          </h1>
          <h1 className="p-8 text-center text-xl font-sans font-bold text-zinc-100">
              2026
          </h1>
      </footer>
    </div>
  );
}

export default App;
