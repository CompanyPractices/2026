import { KpiCardData } from "./types.ts"

type KpiCardsProps = {
    cards: KpiCardData[];
};

export function KpiCards( { cards } : KpiCardsProps) {
    return (
        <div className="grid grid-cols-4 gap-10 m-5">
        {cards.map((card) => (
            <div key={card.label} className="bg-emerald-300 rounded-xl p-4 text-xl items-center shadow-lg text-center">
                <p> {card.value} {card.unit}</p>
                <p> {card.label} </p>
            </div>
        ))}
    </div>);
}