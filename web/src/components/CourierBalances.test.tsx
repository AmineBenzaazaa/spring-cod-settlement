import { screen, within } from '@testing-library/react';
import { overview } from '@/test/fixtures';
import { renderWithProviders } from '@/test/utils';
import { CourierBalanceTable } from './CourierBalances';

describe('CourierBalanceTable', () => {
  it('tells the merchant what each courier still owes', () => {
    renderWithProviders(<CourierBalanceTable couriers={overview.couriers} />);
    const speedex = screen.getAllByTestId('balance-row').find((r) => r.dataset.code === 'speedex')!;
    expect(within(speedex).getByText('Speedex Colis still owes 530.50 MAD')).toBeInTheDocument();
    expect(within(speedex).getByText('88.4%')).toBeInTheDocument();
    const rapido = screen.getAllByTestId('balance-row').find((r) => r.dataset.code === 'rapido')!;
    expect(within(rapido).getByText('Settled')).toBeInTheDocument();
  });

  it('flags an overpaying courier', () => {
    const over = { ...overview.couriers[1]!, balanceOwed: '-20.00' };
    renderWithProviders(<CourierBalanceTable couriers={[over]} />);
    expect(screen.getByText('Overpaid 20.00 MAD')).toBeInTheDocument();
  });

  it('has an empty state', () => {
    renderWithProviders(<CourierBalanceTable couriers={[]} />);
    expect(screen.getByText('No couriers yet')).toBeInTheDocument();
  });
});
