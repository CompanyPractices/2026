import {Header} from "./components/Header"
import {TransactionTable} from "./components/TransactionTable/TransactionTable.tsx";
import {MOCK_KPI_CARDS, MOCK_TRANSACTIONS_TABLE} from "./mockData.ts";

function App() {
  return (
      <div>
        <Header cards={MOCK_KPI_CARDS} />
        <TransactionTable transactions={MOCK_TRANSACTIONS_TABLE} />
      </div>

  )
}

export default App;
