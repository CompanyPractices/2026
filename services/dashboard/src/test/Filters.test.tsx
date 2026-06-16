import { render, screen, fireEvent } from '@testing-library/react';
import { Filters } from '../components/Filters'

if (typeof window !== 'undefined' && !window.ResizeObserver) {
    window.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
}

describe('Filters', () => {
    const onSearchMock = vi.fn();
    const issuersMock = {'ISS001': 'Сбербанк', 'ISS002': 'Т-Банк'};
    const mccMock = {'5411': 'Супермаркеты', '5812': 'Рестораны'};

    beforeEach(() => {
        onSearchMock.mockClear();
    });

    it('all filter fields and buttons correct', () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock}/>);
        expect(screen.getByText('Статус:')).toBeInTheDocument();
        expect(screen.getByText('Банк эмитент:')).toBeInTheDocument();
        expect(screen.getByText('MCC:')).toBeInTheDocument();
        expect(screen.getByLabelText('Начало даты:')).toBeInTheDocument();
        expect(screen.getByLabelText('Конец даты:')).toBeInTheDocument();

        expect(screen.getByRole('button', {name: 'Выберите статус'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Выберите эмитента'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Выберите категорию'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Найти'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Сбросить'})).toBeInTheDocument();
    });

    it('call search with correct data', async () => {
        render(<Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock}/>);
        const statusButton = screen.getByRole('button', { name: 'Выберите статус' });
        fireEvent.click(statusButton);
        const statusOption = screen.getByRole('option', { name: 'Одобрен' });
        fireEvent.click(statusOption);
        const issuerButton = screen.getByRole('button', { name: 'Выберите эмитента' });
        fireEvent.click(issuerButton);
        const issuerOption = screen.getByRole('option', { name: issuersMock['ISS001'] || 'ISS001' });
        fireEvent.click(issuerOption);

        const dateInput = screen.getByLabelText('Начало даты:');
        fireEvent.change(dateInput, { target: { value: '2026-01-01' } });
        const searchButton = screen.getByRole('button', { name: 'Найти' });
        fireEvent.click(searchButton);
        expect(onSearchMock).toHaveBeenCalledTimes(1);
        expect(onSearchMock).toHaveBeenCalledWith({
            status: 'APPROVED',
            issuerId: 'ISS001',
            dateFrom: '2026-01-01'
        });
    });

    it('clear form and call search with empty filter', async() => {
        const { container } = render(
            <Filters issuers={issuersMock} mccNames={mccMock} onSearch={onSearchMock} />
        );
        const statusSelect = container.querySelector('input[name="status"]') || container.querySelector('select');
        if (statusSelect) {
            fireEvent.change(statusSelect, { target: { value: 'DECLINED' } });
        }
        const resetButton = screen.getByRole('button', { name: 'Сбросить' });
        fireEvent.click(resetButton);
        expect(onSearchMock).toHaveBeenCalledWith({});
    });
})
