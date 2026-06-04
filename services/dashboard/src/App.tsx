import {Header} from "./components/Header"

const cards = [
  {label: "Всего ТХ", value: 1250},
  {label: "Одобрено", value: 88, unit: "%"},
  {label: "Общая сумма", value: "1,875,000", unit: "₽"},
  {label: "Среднее время", value: 38, unit: "ms"}
]

function App() {
  return <Header cards={cards} />
}

export default App;
