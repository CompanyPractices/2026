import {KpiCards} from "./KpiCards.tsx";
import {DashboardStats} from "../types";

type HeaderProps = {
    stats: DashboardStats;
};

export function Header( { stats } : HeaderProps) {
    return <header className="flex flex-col items-center font-mono m-5">
        <h1 className="text-5xl font-semibold drop-shadow-lg">Dashboard</h1>
        <KpiCards stats={stats} />
    </header>;
}
