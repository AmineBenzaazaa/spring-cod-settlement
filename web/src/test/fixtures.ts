import type { Dispute, Overview, ReconciliationResult, ReconciliationSummary } from '@/lib/types';

export const overview: Overview = {
  merchantId: 1, currency: 'MAD',
  expectedCollections: '4588.00', reportedCollections: '4057.50', fees: '225.00', netRemitted: '3832.50', balanceOwed: '530.50',
  openDisputes: 1, openDisputedAmount: '50.00', discrepancies: 3, discrepancyAmount: '1009.50',
  couriers: [
    { courierId: 2, name: 'Rapido Express', code: 'rapido', active: true, deliveredOrders: 0, expectedCollections: '0.00',
      reportedCollections: '0.00', fees: '0.00', netRemitted: '0.00', balanceOwed: '0.00' },
    { courierId: 1, name: 'Speedex Colis', code: 'speedex', active: true, deliveredOrders: 8, expectedCollections: '4588.00',
      reportedCollections: '4057.50', fees: '225.00', netRemitted: '3832.50', balanceOwed: '530.50' },
  ],
};

const result = (id: number, outcome: ReconciliationResult['outcome'], tracking: string, expected: string, collected: string,
  difference: string, hasDispute = false): ReconciliationResult => ({
  id, outcome, trackingNumber: tracking, orderId: outcome === 'UNKNOWN_TRACKING' ? null : id,
  line: outcome === 'MISSING_FROM_STATEMENT' ? null : { rowNumber: id, trackingNumber: tracking, amountCollected: collected, fee: '25.00' },
  expectedAmount: expected, collectedAmount: collected, difference, hasDispute,
});

export const results: ReconciliationResult[] = [
  result(1, 'MATCHED', 'SPX-100007', '817.50', '817.50', '0.00'),
  result(2, 'MATCHED', 'SPX-100009', '528.50', '528.50', '0.00'),
  result(3, 'AMOUNT_MISMATCH', 'SPX-100013', '445.50', '395.50', '-50.00'),
  result(4, 'UNKNOWN_TRACKING', 'SPX-UNKNOWN01', '0.00', '199.00', '199.00', true),
  result(5, 'MISSING_FROM_STATEMENT', 'SPX-100001', '760.50', '0.00', '-760.50'),
];

export const summary: ReconciliationSummary = {
  counts: { MATCHED: 2, AMOUNT_MISMATCH: 1, UNKNOWN_TRACKING: 1, MISSING_FROM_STATEMENT: 1 },
  expectedTotal: '2552.00', collectedTotal: '1940.50', feesTotal: '100.00', netRemitted: '1840.50', differenceTotal: '-611.50',
};

export const dispute: Dispute = {
  id: 9, resultId: 3, statementId: 1, trackingNumber: 'SPX-100013', outcome: 'AMOUNT_MISMATCH', merchantId: 1,
  reason: 'Courier short-paid 50 MAD', claimedAmount: '50.00', status: 'OPEN', resolutionNote: null,
  createdAt: '2026-11-01T10:00:00Z', resolvedAt: null,
};
