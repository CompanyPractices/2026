import LineChart from "./components/LineChart.tsx";
import {Header} from "./components/Header.tsx";
import PieChart from "./components/PieChart.tsx";
import {TransactionTable} from "./components/TransactionTable.tsx";
import {MOCK_DASHBOARD_STATS, MOCK_TRANSACTIONS} from "./mockData.ts";

function App() {
  return (
    <div className="bg-zinc-200 min-h-screen flex flex-col items-center justify-items-stretch">
      <Header stats={MOCK_DASHBOARD_STATS} />
      <main className="w-2/3 flex-grow grid grid-cols-4 gap-4">
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <LineChart />
          </div>
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <PieChart />
          </div>
          <div className="col-span-4 m-4 place-content-center">
              <TransactionTable transactions={MOCK_TRANSACTIONS}/>
          </div>
      </main>

      <footer className="rounded-2xl m-4 w-5/6 h-24 grid grid-cols-3 gap-4">
          <h1 className="p-8 text-center text-xl font-mono font-bold">
              Практика
          </h1>
          <h1 className="p-8 text-center text-xl font-mono font-bold">
              СМП - Система медленных платежей
          </h1>
          <h1 className="p-8 text-center text-xl font-mono font-bold">
              2026
          </h1>
      </footer>
    </div>
  );
}

export default App;
