type KpiCardData = {
    label: string;
    value: number | string;
    unit?: string;
};

export type KpiCardsProps = {
    cards: KpiCardData[];
};