DROP INDEX uk_reservations_rrn;
CREATE UNIQUE INDEX uk_reservations_rrn_pan ON reservations (rrn, pan);
