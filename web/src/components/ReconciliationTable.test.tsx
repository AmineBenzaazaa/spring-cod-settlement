import { vi } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { results, summary } from '@/test/fixtures';
import { renderWithProviders } from '@/test/utils';
import { filterResults, ReconciliationTable } from './ReconciliationTable';

describe('ReconciliationTable', () => {
  it('colour-codes every line with its outcome and signed difference', () => {
    renderWithProviders(<ReconciliationTable results={results} summary={summary} onOpenDispute={() => {}} />);
    const rows = screen.getAllByTestId('recon-row');
    expect(rows.map((r) => r.dataset.outcome)).toEqual(['MATCHED', 'MATCHED', 'AMOUNT_MISMATCH', 'UNKNOWN_TRACKING', 'MISSING_FROM_STATEMENT']);
    const mismatch = rows[2]!;
    expect(mismatch.className).toContain('border-l-amber-500');
    expect(within(mismatch).getByText('Amount mismatch')).toBeInTheDocument();
    expect(within(mismatch).getByText('445.50')).toBeInTheDocument();
    expect(within(mismatch).getByText('−50.00')).toHaveClass('text-rose-600');
    expect(within(rows[3]!).getByText('+199.00')).toHaveClass('text-amber-600');
  });

  it('offers "Open dispute" only on non-matched lines without a dispute', async () => {
    const onOpen = vi.fn();
    renderWithProviders(<ReconciliationTable results={results} summary={summary} onOpenDispute={onOpen} />);
    const buttons = screen.getAllByTestId('open-dispute');
    expect(buttons).toHaveLength(2); // mismatch + missing; the unknown line is already disputed
    expect(screen.getByTestId('disputed')).toBeInTheDocument();
    await userEvent.click(buttons[0]!);
    expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: 3, outcome: 'AMOUNT_MISMATCH' }));
  });

  it('filters by outcome chip and by tracking search', async () => {
    renderWithProviders(<ReconciliationTable results={results} summary={summary} onOpenDispute={() => {}} />);
    expect(screen.getByTestId('count-MATCHED')).toHaveTextContent('2');
    expect(screen.getByTestId('count-ISSUES')).toHaveTextContent('3');

    await userEvent.click(screen.getByTestId('filter-ISSUES'));
    expect(screen.getAllByTestId('recon-row')).toHaveLength(3);

    await userEvent.click(screen.getByTestId('filter-MISSING_FROM_STATEMENT'));
    expect(screen.getAllByTestId('recon-row').map((r) => r.dataset.tracking)).toEqual(['SPX-100001']);

    await userEvent.click(screen.getByTestId('filter-ALL'));
    await userEvent.type(screen.getByLabelText('Search tracking number'), 'zzz');
    expect(screen.getByText('No line matches these filters')).toBeInTheDocument();
  });

  it('filterResults is case-insensitive', () => {
    expect(filterResults(results, 'ALL', 'unknown').map((r) => r.id)).toEqual([4]);
  });
});
