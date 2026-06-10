import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Filters } from '../components/Filters'

describe('Filters', () => {
    const onSearchMock = vi.fn();
    const issuersMock = {'ISS001': 'Сбербанк', 'ISS002': 'Т-Банк'};
    const mccMock = {'5411': 'Супермаркеты', '5812': 'Рестораны'};

    beforeEach(() => {
        onSearchMock.mockClear();
    });

    it('all filter fields and buttons correct', () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock}/>)
        expect(screen.getByLabelText('Статус:')).toBeInTheDocument();
        expect(screen.getByLabelText('Банк эмитент:')).toBeInTheDocument();
        expect(screen.getByLabelText('MCC:')).toBeInTheDocument();
        expect(screen.getByLabelText('Начало даты:')).toBeInTheDocument();
        expect(screen.getByLabelText('Конец даты:')).toBeInTheDocument();

        expect(screen.getByRole('button', {name: 'Найти'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Сбросить'})).toBeInTheDocument();
    });

    it('call search with correct data', async () => {
        const user = userEvent.setup();
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock}/>);
        expect(screen.getByLabelText('Статус:')).toBeInTheDocument();
        await user.selectOptions(screen.getByLabelText('Статус:'), 'APPROVED');
        await user.selectOptions(screen.getByLabelText('Банк эмитент:'), 'ISS001');
        await user.clear(screen.getByLabelText('Начало даты:'));
        await user.type(screen.getByLabelText('Начало даты:'), '2026-01-01');

        await user.click(screen.getByRole('button', {name: 'Найти'}));

        expect(onSearchMock).toHaveBeenCalledTimes(1);
        expect(onSearchMock).toHaveBeenCalledWith({
            status: 'APPROVED',
            issuerId: 'ISS001',
            dateFrom: '2026-01-01'
        });
    });

    it('clear form and call search with empty filter', async() => {
        const user = userEvent.setup();
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock}/>);
        await user.selectOptions(screen.getByLabelText('Статус:'), 'DECLINED');
        await user.selectOptions(screen.getByLabelText('MCC:'), '5411');

        await user.click(screen.getByRole('button', {name: 'Сбросить'}));

        expect(screen.getByLabelText('Статус:')).toHaveValue('');
        expect(screen.getByLabelText('MCC:')).toHaveValue('');
        expect(onSearchMock).toHaveBeenCalledWith({});
    });
})
