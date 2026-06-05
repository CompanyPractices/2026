import LineChart from "./components/LineChart.tsx";
import {Header} from "./components/Header.tsx";
import PieChart from "./components/PieChart.tsx";
import TransactionTable from "./components/TransactionTable.tsx";
import {MOCK_KPI_CARDS} from "./mockData.ts";

function App() {
  return (
    <div className="bg-zinc-200 min-h-screen flex flex-col items-center justify-items-stretch">
      <Header cards={MOCK_KPI_CARDS} />
      <main className="w-2/3 flex-grow grid grid-cols-4 gap-4">
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <LineChart />
          </div>
          <div className="col-span-2 bg-zinc-300 m-4 rounded-lg shadow-xl place-content-center">
              <PieChart />
          </div>
          <div className="col-span-4 bg-lime-300 m-4 rounded-lg shadow-xl place-content-center">
              <TransactionTable />
          </div>
      </main>

      <footer className="rounded-2xl m-8 w-5/6 bg-violet-400 h-24 grid grid-cols-3 gap-4">
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
