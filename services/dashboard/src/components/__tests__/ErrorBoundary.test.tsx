import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorBoundary } from '../ErrorBoundary';
import { ReactNode } from 'react';

function BrokenChild({ message = 'Сломался!' }: { message?: string }) {
    throw new Error(message);
}

function HealthyChild() {
    return <div data-testid="healthy">Работаю</div>;
}

const TEST_NAMES = {
    widget: 'Тестовый виджет',
    test: 'Тест',
    chart: 'График',
} as const;

const ERROR_MESSAGES = {
    default: 'Сломался!',
    temporary: 'Временный сбой',
    chart: 'Ошибка графика',
} as const;

const renderWithBrokenChild = (props: { name?: string; fallback?: ReactNode; onError?: (error: Error, info: any, name: string) => void; children?: ReactNode } = {}) => {
    const { children = <BrokenChild />, ...rest } = props;
    return render(
        <ErrorBoundary name={TEST_NAMES.widget} {...rest}>
            {children}
        </ErrorBoundary>
    );
};

const renderWithHealthyChild = () => {
    return render(
        <ErrorBoundary name={TEST_NAMES.test}>
            <HealthyChild />
        </ErrorBoundary>
    );
};

const expectFallbackVisible = () => {
    expect(screen.getByRole('alert')).toBeInTheDocument();
};

const expectFallbackHidden = () => {
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
};

const expectRetryButtonVisible = () => {
    expect(screen.getByRole('button', { name: /Повторить/i })).toBeInTheDocument();
};

const getRetryButton = () => screen.getByRole('button', { name: /Повторить/i });

describe('ErrorBoundary', () => {
    const originalError = console.error;
    beforeEach(() => {
        console.error = vi.fn();
    });
    afterEach(() => {
        console.error = originalError;
    });

    it('renders fallback when child component crashes', () => {
        renderWithBrokenChild();

        expectFallbackVisible();
        expect(screen.getByText(/Не удалось загрузить: Тестовый виджет/)).toBeInTheDocument();
        expect(screen.getByText(ERROR_MESSAGES.default)).toBeInTheDocument();
        expectRetryButtonVisible();
    });

    it('does not render child component when it crashes', () => {
        renderWithBrokenChild({ name: TEST_NAMES.test });

        expect(screen.queryByTestId('healthy')).not.toBeInTheDocument();
    });

    it('recovers child component after clicking Retry', async () => {
        const user = userEvent.setup();

        let shouldThrow = true;
        function FlakyChild() {
            if (shouldThrow) throw new Error(ERROR_MESSAGES.temporary);
            return <div data-testid="recovered">Восстановлен</div>;
        }

        render(
            <ErrorBoundary name={TEST_NAMES.test}>
                <FlakyChild />
            </ErrorBoundary>
        );

        expectFallbackVisible();

        shouldThrow = false;

        await user.click(getRetryButton());

        expectFallbackHidden();
        expect(screen.getByTestId('recovered')).toBeInTheDocument();
    });

    it('calls onError with widget name on error', () => {
        const onError = vi.fn();

        renderWithBrokenChild({
            name: TEST_NAMES.chart,
            onError,
            children: <BrokenChild message={ERROR_MESSAGES.chart} />
        });

        expect(onError).toHaveBeenCalledTimes(1);
        expect(onError).toHaveBeenCalledWith(
            expect.any(Error),
            expect.objectContaining({ componentStack: expect.any(String) }),
            TEST_NAMES.chart
        );
        expect(onError.mock.calls[0][0].message).toBe(ERROR_MESSAGES.chart);
    });

    it('uses custom fallback when provided', () => {
        const customFallback = <div>Мой fallback</div>;
        renderWithBrokenChild({
            name: TEST_NAMES.test,
            fallback: customFallback
        });

        expect(screen.getByText('Мой fallback')).toBeInTheDocument();
    });

    it('renders children when there is no error', () => {
        renderWithHealthyChild();

        expect(screen.getByTestId('healthy')).toBeInTheDocument();
        expectFallbackHidden();
    });
});
