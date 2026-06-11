import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import TransactionLineChart from '../components/TransactionLineChart';
import TransactionPieChart from '../components/TransactionPieChart';

describe('TransactionLineChart', () => {
    it('should display loading state', () => {
        render(<TransactionLineChart transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка транзакций\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<TransactionLineChart transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки транзакций:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<TransactionLineChart transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Транзакций не найдено/i)).toBeInTheDocument();
    });
})

describe('TransactionPieChart', () => {
    it('should display loading state', () => {
        render(<TransactionPieChart transactions={[]} loading={true} error={null} />);
        expect(screen.getByText(/Загрузка транзакций\.\.\./i)).toBeInTheDocument();
    });

    it('should display error message', () => {
        render(<TransactionPieChart transactions={[]} loading={false} error="Network Error" />);
        expect(screen.getByText(/Ошибка загрузки транзакций:/i)).toBeInTheDocument();
    });

    it('should display empty state when no transactions exist', () => {
        render(<TransactionPieChart transactions={[]} loading={false} error={null} />);
        expect(screen.getByText(/Транзакций не найдено/i)).toBeInTheDocument();
    });
})
