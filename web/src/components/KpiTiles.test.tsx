import { render, screen, within } from '@testing-library/react';
import { I18nProvider } from '@/lib/i18n';
import { overview } from '@/test/fixtures';
import { KpiTilesView } from './KpiTiles';

describe('KpiTilesView', () => {
  it('renders the five KPIs from money strings', () => {
    render(<I18nProvider initial="en"><KpiTilesView overview={overview} /></I18nProvider>);
    expect(within(screen.getByTestId('kpi-expected')).getByText('4,588.00')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-expected')).getByText('8 delivered parcels')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-reported')).getByText('4,057.50')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-reported')).getByText(/88\.4 % of expected/)).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-net')).getByText('3,832.50')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-disputes')).getByText('1')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-disputes')).getByText('50.00 MAD claimed')).toBeInTheDocument();
    expect(within(screen.getByTestId('kpi-discrepancy')).getByText('1,009.50')).toBeInTheDocument();
  });

  it('shows a dash instead of a percentage before anything is delivered', () => {
    const empty = { ...overview, expectedCollections: '0.00', reportedCollections: '0.00', couriers: [] };
    render(<I18nProvider initial="en"><KpiTilesView overview={empty} /></I18nProvider>);
    expect(within(screen.getByTestId('kpi-reported')).getByText(/— of expected/)).toBeInTheDocument();
  });

  it('speaks French', () => {
    render(<I18nProvider initial="fr"><KpiTilesView overview={overview} /></I18nProvider>);
    expect(screen.getByText('COD attendu')).toBeInTheDocument();
    expect(screen.getByText('Litiges ouverts')).toBeInTheDocument();
  });
});
