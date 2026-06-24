import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Filters } from '../Filters';
import { MemoryRouter } from 'react-router-dom';

if (typeof window !== 'undefined' && !window.ResizeObserver) {
    window.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
}

const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={['/']}>{children}</MemoryRouter>
);

describe('Filters', () => {
    const onSearchMock = vi.fn();
    const issuersMock = { 'ISS001': 'Сбербанк', 'ISS002': 'Т-Банк' };
    const mccMock = { '5411': 'Супермаркеты', '5812': 'Рестораны' };
    const user = userEvent.setup();

    beforeEach(() => onSearchMock.mockClear());

    it('renders all filter fields and buttons correctly', () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock} />, { wrapper: Wrapper });

        expect(screen.getByText('Статус:')).toBeInTheDocument();
        expect(screen.getByText('Банк эмитент:')).toBeInTheDocument();
        expect(screen.getByText('MCC:')).toBeInTheDocument();
        expect(screen.getByLabelText('Начало даты:')).toBeInTheDocument();
        expect(screen.getByLabelText('Конец даты:')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Найти' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Сбросить' })).toBeInTheDocument();
    });

    it('calls search with correct data when form is submitted', async () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock} />, { wrapper: Wrapper });

        const dateInput = screen.getByLabelText('Начало даты:');
        await user.clear(dateInput);
        await user.type(dateInput, '2026-01-01');

        const searchButton = screen.getByRole('button', { name: 'Найти' });
        await user.click(searchButton);

        expect(onSearchMock).toHaveBeenCalledTimes(1);
        expect(onSearchMock).toHaveBeenCalledWith({ dateFrom: '2026-01-01' });
    });

    it('clears form and calls search with empty filter on reset', async () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock} />, { wrapper: Wrapper });

        const dateInput = screen.getByLabelText('Конец даты:');
        await user.clear(dateInput);
        await user.type(dateInput, '2026-12-31');

        const resetButton = screen.getByRole('button', { name: 'Сбросить' });
        await user.click(resetButton);

        expect(onSearchMock).toHaveBeenCalledWith({});
        const allButtons = screen.getAllByText('Все');
        expect(allButtons.length).toBeGreaterThanOrEqual(3);
    });
});
