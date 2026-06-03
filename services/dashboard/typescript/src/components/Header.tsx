import {KpiCardData} from "./KpiCards.tsx";
import {KpiCards} from "./KpiCards.tsx";

type HeaderProps = {
    cards: KpiCardData[];
};

export function Header( { cards } : HeaderProps) {
    return <header>
        <h1>Dashboard</h1>
        <KpiCards cards={cards} />
    </header>;
}