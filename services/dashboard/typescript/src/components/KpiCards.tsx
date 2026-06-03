export type KpiCardData = {
    label: string;
    value: number | string;
    unit?: string;
};

export type KpiCardsProps = {
    cards: KpiCardData[];
};

export function KpiCards( { cards } : KpiCardsProps) {
    return <div>
        {cards.map((card) => (
            <div key={card.label}>
                <p> {card.value} {card.unit}</p>
                <p> {card.label} </p>
            </div>
        ))}
    </div>;
}