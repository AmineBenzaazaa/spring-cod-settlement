import { translate } from './i18n';

describe('translate', () => {
  it('picks singular or plural forms, with French counting 0 and 1 as singular', () => {
    expect(translate('en', 'kpi.discrepancyHint', { n: 1 })).toBe('1 line needs attention');
    expect(translate('en', 'kpi.discrepancyHint', { n: '3' })).toBe('3 lines need attention');
    expect(translate('en', 'kpi.expectedHint', { n: '1,204' })).toBe('Cash due on 1,204 delivered parcels');
    expect(translate('fr', 'kpi.discrepancyHint', { n: 0 })).toBe('0 ligne à traiter');
    expect(translate('fr', 'kpi.discrepancyHint', { n: 2 })).toBe('2 lignes à traiter');
  });
});
