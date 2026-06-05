import {Header} from "./components/Header"
import {MOCK_DASHBOARD_STATS} from "./mockData.ts";

function App() {
  return <Header stats={MOCK_DASHBOARD_STATS} />
}

export default App;
