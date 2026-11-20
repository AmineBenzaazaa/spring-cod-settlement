import type { Money } from './money';

export type Role = 'MERCHANT' | 'STAFF';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type StatementStatus = 'UPLOADED' | 'PROCESSING' | 'RECONCILED' | 'FAILED';
export type Outcome = 'MATCHED' | 'AMOUNT_MISMATCH' | 'UNKNOWN_TRACKING' | 'MISSING_FROM_STATEMENT';
export type DisputeStatus = 'OPEN' | 'RESOLVED' | 'REJECTED';
export type DisputeDecision = 'RESOLVED' | 'REJECTED';
export type OrderStatus = 'PENDING' | 'SHIPPED' | 'DELIVERED' | 'RETURNED' | 'CANCELLED';
export type AttemptOutcome = 'DELIVERED' | 'NO_ANSWER' | 'REFUSED' | 'RESCHEDULED' | 'RETURNED';

export const OUTCOMES: Outcome[] = ['MATCHED', 'AMOUNT_MISMATCH', 'UNKNOWN_TRACKING', 'MISSING_FROM_STATEMENT'];

export interface CourierPosition {
  courierId: number;
  name: string;
  code: string;
  active: boolean;
  deliveredOrders: number;
  expectedCollections: Money;
  reportedCollections: Money;
  fees: Money;
  netRemitted: Money;
  balanceOwed: Money;
}

export interface Overview {
  merchantId: number | null;
  currency: string;
  expectedCollections: Money;
  reportedCollections: Money;
  fees: Money;
  netRemitted: Money;
  balanceOwed: Money;
  openDisputes: number;
  openDisputedAmount: Money;
  discrepancies: number;
  discrepancyAmount: Money;
  couriers: CourierPosition[];
}

export interface Courier {
  id: number;
  name: string;
  code: string;
  contactPhone: string | null;
  active: boolean;
  createdAt: string;
}

export interface DeliveryAttempt {
  id: number;
  attemptedAt: string;
  outcome: AttemptOutcome;
  note: string | null;
}

export interface Order {
  id: number;
  merchantId: number;
  courierId: number;
  courierName: string;
  trackingNumber: string;
  customerName: string;
  customerPhone: string | null;
  city: string | null;
  amountDue: Money;
  currency: string;
  status: OrderStatus;
  deliveredAt: string | null;
  attempts: DeliveryAttempt[];
  createdAt: string;
  updatedAt: string;
}

export interface Statement {
  id: number;
  merchantId: number;
  courierId: number;
  periodStart: string;
  periodEnd: string;
  originalFilename: string;
  fileSha256: string;
  currency: string;
  status: StatementStatus;
  errorMessage: string | null;
  lineCount: number;
  uploadedAt: string;
  reconciledAt: string | null;
}

export interface StatementUploadResponse {
  duplicate: boolean;
  statement: Statement;
}

export interface RemittanceLine {
  rowNumber: number;
  trackingNumber: string;
  amountCollected: Money;
  fee: Money;
}

export interface ReconciliationResult {
  id: number;
  outcome: Outcome;
  trackingNumber: string;
  orderId: number | null;
  line: RemittanceLine | null;
  expectedAmount: Money;
  collectedAmount: Money;
  difference: Money;
  hasDispute: boolean;
}

export interface ReconciliationSummary {
  counts: Record<Outcome, number>;
  expectedTotal: Money;
  collectedTotal: Money;
  feesTotal: Money;
  netRemitted: Money;
  differenceTotal: Money;
}

export interface ReconciliationReport {
  statement: Statement;
  summary: ReconciliationSummary | null;
  results: ReconciliationResult[];
}

export interface Dispute {
  id: number;
  resultId: number;
  statementId: number;
  trackingNumber: string;
  outcome: Outcome;
  merchantId: number;
  reason: string;
  claimedAmount: Money;
  status: DisputeStatus;
  resolutionNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}
