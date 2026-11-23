import { screen, within } from '@testing-library/react';
import { overview } from '@/test/fixtures';
import { renderWithProviders } from '@/test/utils';
import { CourierBalanceTable } from './CourierBalances';

describe('CourierBalanceTable', () => {
  it('tells the merchant what each courier still owes', () => {
    renderWithProviders(<CourierBalanceTable couriers={overview.couriers} />);
    const speedex = screen.getAllByTestId('balance-row').find((r) => r.dataset.code === 'speedex')!;
    // the amount is shown once, with a small status chip instead of a repeated sentence
    expect(within(speedex).getByText('530.50')).toBeInTheDocument();
    expect(within(speedex).getByTestId('balance-status')).toHaveTextContent('On track');
    expect(within(speedex).queryByText(/still owes/)).toBeNull();
    expect(within(speedex).getByText('88.4%')).toBeInTheDocument();
    const rapido = screen.getAllByTestId('balance-row').find((r) => r.dataset.code === 'rapido')!;
    expect(within(rapido).getByText('Settled')).toBeInTheDocument();
  });

  it('flags an overpaying courier', () => {
    const over = { ...overview.couriers[1]!, balanceOwed: '-20.00' };
    renderWithProviders(<CourierBalanceTable couriers={[over]} />);
    expect(screen.getByText('Overpaid 20.00 MAD')).toBeInTheDocument();
  });

  it('marks a courier that reported nothing as the most urgent', () => {
    const none = { ...overview.couriers[1]!, reportedCollections: '0.00', balanceOwed: '100.00' };
    renderWithProviders(<CourierBalanceTable couriers={[none]} />);
    expect(screen.getByTestId('balance-status')).toHaveAttribute('data-health', 'none');
    expect(screen.getByText('Nothing reported')).toBeInTheDocument();
  });

  it('has an empty state', () => {
    renderWithProviders(<CourierBalanceTable couriers={[]} />);
    expect(screen.getByText('No couriers yet')).toBeInTheDocument();
  });
});
