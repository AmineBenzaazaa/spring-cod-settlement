import { vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api, ApiError } from '@/lib/api';
import { dispute } from '@/test/fixtures';
import { renderWithProviders, signIn } from '@/test/utils';
import { DisputeDrawer } from './DisputeDrawer';

afterEach(() => vi.restoreAllMocks());

describe('DisputeDrawer', () => {
  it('hides the resolution controls from merchants', async () => {
    signIn({ role: 'MERCHANT' });
    vi.spyOn(api, 'dispute').mockResolvedValue(dispute);
    renderWithProviders(<DisputeDrawer disputeId={9} onClose={() => {}} />);

    expect(await screen.findByTestId('dispute-reason')).toHaveTextContent('Courier short-paid 50 MAD');
    expect(screen.getByTestId('awaiting-staff')).toBeInTheDocument();
    expect(screen.queryByTestId('resolve-panel')).not.toBeInTheDocument();
  });

  it('lets staff resolve with a note', async () => {
    signIn({ role: 'STAFF', username: 'ops', merchantId: null });
    vi.spyOn(api, 'dispute').mockResolvedValue(dispute);
    const resolve = vi.spyOn(api, 'resolveDispute').mockResolvedValue({ ...dispute, status: 'RESOLVED', resolutionNote: 'Paid' });
    renderWithProviders(<DisputeDrawer disputeId={9} onClose={() => {}} />);

    await userEvent.type(await screen.findByLabelText('Resolution note'), 'Paid');
    await userEvent.click(screen.getByTestId('resolve-dispute'));

    expect(resolve).toHaveBeenCalledWith(9, { decision: 'RESOLVED', resolutionNote: 'Paid' });
    expect(await screen.findByTestId('badge-status')).toHaveAttribute('data-value', 'RESOLVED');
    expect(screen.queryByTestId('resolve-panel')).not.toBeInTheDocument();
  });

  it('shows a 409 ProblemDetail nicely', async () => {
    signIn({ role: 'STAFF', username: 'ops', merchantId: null });
    vi.spyOn(api, 'dispute').mockResolvedValue(dispute);
    vi.spyOn(api, 'resolveDispute').mockRejectedValue(
      new ApiError(409, 'Concurrent update', 'The resource was modified by someone else. Reload it and try again.'));
    renderWithProviders(<DisputeDrawer disputeId={9} onClose={() => {}} />);

    await userEvent.click(await screen.findByTestId('reject-dispute'));

    const alert = await screen.findByTestId('problem');
    expect(alert).toHaveTextContent('Concurrent update');
    expect(alert).toHaveTextContent('409');
    expect(alert).toHaveTextContent('modified by someone else');
  });
});
