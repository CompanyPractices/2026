import {KpiCardData} from "./KpiCards.tsx";
import {KpiCards} from "./KpiCards.tsx";

type HeaderProps = {
    cards: KpiCardData[];
};

export function Header( { cards } : HeaderProps) {
    return <header className="flex flex-col items-center font-mono m-5">
        <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
        <KpiCards cards={cards} />
    </header>;
}