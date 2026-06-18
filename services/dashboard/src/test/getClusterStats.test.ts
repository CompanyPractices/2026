import { describe, it, expect } from 'vitest';
import { Transaction } from '../types';
import {getClusterStats} from "../components/TransactionsMap";

const createMockTx = (status: Transaction['status'], amount: number): Transaction =>
    ({ status, amount } as Transaction);

describe('getClusterStats', () => {
    describe('with empty array', () => {
        it('should return all zeros', () => {
            const result = getClusterStats([]);

            expect(result).toEqual({
                approved: 0,
                declined: 0,
                totalAmount: 0,
                approvalRate: 0,
            });
        });
    });

    describe('with all approved transactions', () => {
        it('should return 100% approval rate', () => {
            const transactions = [
                createMockTx('APPROVED', 10000),
                createMockTx('APPROVED', 20000),
                createMockTx('APPROVED', 30000),
            ];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 3,
                declined: 0,
                totalAmount: 60000,
                approvalRate: 100,
            });
        });
    });

    describe('with all declined transactions', () => {
        it('should return 0% approval rate', () => {
            const transactions = [
                createMockTx('DECLINED', 10000),
                createMockTx('DECLINED', 20000),
            ];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 0,
                declined: 2,
                totalAmount: 30000,
                approvalRate: 0,
            });
        });
    });

    describe('with mixed transactions', () => {
        it('should calculate correct statistics', () => {
            const transactions = [
                createMockTx('APPROVED', 10000),
                createMockTx('DECLINED', 20000),
                createMockTx('APPROVED', 30000),
                createMockTx('DECLINED', 40000),
            ];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 2,
                declined: 2,
                totalAmount: 100000,
                approvalRate: 50,
            });
        });

        it('should handle fractional approval rate', () => {
            const transactions = [
                createMockTx('APPROVED', 10000),
                createMockTx('DECLINED', 20000),
                createMockTx('DECLINED', 30000),
            ];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 1,
                declined: 2,
                totalAmount: 60000,
                approvalRate: 33.33333333333333,
            });
        });
    });

    describe('with single transaction', () => {
        it('should handle single approved transaction', () => {
            const transactions = [createMockTx('APPROVED', 50000)];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 1,
                declined: 0,
                totalAmount: 50000,
                approvalRate: 100,
            });
        });

        it('should handle single declined transaction', () => {
            const transactions = [createMockTx('DECLINED', 50000)];

            const result = getClusterStats(transactions);

            expect(result).toEqual({
                approved: 0,
                declined: 1,
                totalAmount: 50000,
                approvalRate: 0,
            });
        });
    });
});
