import {Header} from "./components/Header"
import {MOCK_DASHBOARD_STATS, MOCK_TRANSACTIONS_TABLE} from "./mockData.ts";
import {TransactionTable} from "./components/TransactionTable/TransactionTable.tsx";

function App() {
  return
  return (
      <div>
          <Header stats={MOCK_DASHBOARD_STATS} />
          <TransactionTable transactions={MOCK_TRANSACTIONS_TABLE} />
      </div>

  )
}

export default App;
