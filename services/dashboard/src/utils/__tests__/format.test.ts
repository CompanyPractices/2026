import { describe, it, expect } from 'vitest';
import {hidePan, convertPenniesToRubles, formatTime, formatDateTime, formatDate} from '../format';

describe('formatAmount', () => {
    it('converts pennies to rubles with 2 decimal places', () => {
        expect(convertPenniesToRubles(250000)).toBe('2 500.00 ₽');
    });

    it('handles zero amount', () => {
        expect(convertPenniesToRubles(0)).toBe('0.00 ₽');
    });

    it('handles large amounts', () => {
        expect(convertPenniesToRubles(187500000)).toBe('1 875 000.00 ₽');
    });

    it('handles fractional pennies', () => {
        expect(convertPenniesToRubles(45000)).toBe('450.00 ₽');
    });
});

describe('maskPan', () => {
    it('masks middle digits of 16-digit PAN', () => {
        expect(hidePan('4000001234560001')).toBe('4000****0001');
    });
});

describe('formatTime', () => {
    it('formats ISO string to time', () => {
        expect(formatTime('2026-06-03T14:30:01Z')).toBe('14:30:01');
    });
});

describe('formatDateTime', () => {
    it('formats ISO string to Russian date and time', () => {
        expect(formatDateTime('2026-06-03T14:30:01Z')).toBe('03.06.2026, 14:30:01');
    });
});

describe('formatDate', () => {
    it('formats ISO string to Russian date', () => {
        expect(formatDate('2026-06-03T14:30:01Z')).toBe('03.06.2026');
    });
});
