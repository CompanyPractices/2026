import { describe, it, expect } from 'vitest';
import {
    getTransactionLocation,
    ISSUER_LOCATIONS,
    DEFAULT_LOCATION,
} from '../geoGenerator';

describe('getTransactionLocation', () => {
    describe('when issuerId is known', () => {
        it.each(Object.entries(ISSUER_LOCATIONS))(
            'should return correct location for issuer %s',
            (issuerId, expectedLocation) => {
                const result = getTransactionLocation(issuerId);
                expect(result).toEqual(expectedLocation);
            }
        );
    });

    describe('when issuerId is unknown or missing', () => {
        it('should return DEFAULT_LOCATION reference for unknown issuerId', () => {
            const result = getTransactionLocation('UNKNOWN_ISSUER');
            expect(result).toBe(DEFAULT_LOCATION);
        });

        it('should return DEFAULT_LOCATION reference when issuerId is undefined', () => {
            const result = getTransactionLocation(undefined);
            expect(result).toBe(DEFAULT_LOCATION);
        });

        it('should return DEFAULT_LOCATION reference when issuerId is not provided', () => {
            const result = getTransactionLocation();
            expect(result).toBe(DEFAULT_LOCATION);
        });

        it('should return DEFAULT_LOCATION reference when issuerId is empty string', () => {
            const result = getTransactionLocation('');
            expect(result).toBe(DEFAULT_LOCATION);
        });
    });
});
