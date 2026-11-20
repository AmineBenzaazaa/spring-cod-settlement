import { absMoney, formatMoney, fromCents, isValidMoneyInput, moneySign, normalizeMoneyInput, ratioPercent, sumMoney, toCents } from './money';

describe('money (decimal strings, never floats)', () => {
  it('parses API strings into integer cents', () => {
    expect(toCents('349.90')).toBe(34990n);
    expect(toCents('-50.00')).toBe(-5000n);
    expect(toCents('7')).toBe(700n);
    expect(toCents('0.5')).toBe(50n);
    expect(toCents('10.005')).toBe(1001n); // half-up, like the backend
    expect(() => toCents('12,50')).toThrow();
    expect(() => toCents('abc')).toThrow();
  });

  it('round-trips without float drift', () => {
    expect(fromCents(toCents('0.10') + toCents('0.20'))).toBe('0.30'); // 0.1 + 0.2 !== 0.3 in floats
    expect(sumMoney(['0.10', '0.20', '-0.05'])).toBe('0.25');
    expect(fromCents(-5n)).toBe('-0.05');
  });

  it('keeps amounts beyond Number.MAX_SAFE_INTEGER exact', () => {
    expect(formatMoney('90071992547409931.07', 'en')).toBe('90,071,992,547,409,931.07 MAD');
  });

  it('formats per locale with grouping and two decimals', () => {
    expect(formatMoney('4588', 'en')).toBe('4,588.00 MAD');
    expect(formatMoney('1234567.5', 'en', { currency: false })).toBe('1,234,567.50');
    const fr = formatMoney('4588.5', 'fr');
    expect(fr).toMatch(/^4.588,50 MAD$/); // fr-MA grouping symbol varies by ICU version
    expect(fr).toContain(',50');
  });

  it('shows signs for differences', () => {
    expect(formatMoney('-50.00', 'en', { signed: true })).toBe('−50.00 MAD');
    expect(formatMoney('199.00', 'en', { signed: true, currency: false })).toBe('+199.00');
    expect(formatMoney('0.00', 'en', { signed: true, currency: false })).toBe('0.00');
  });

  it('renders a dash for missing or invalid values', () => {
    expect(formatMoney(null)).toBe('—');
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney('NaN')).toBe('—');
  });

  it('computes sign, absolute value and ratios on cents', () => {
    expect(moneySign('-0.01')).toBe(-1);
    expect(moneySign('0.00')).toBe(0);
    expect(absMoney('-530.50')).toBe('530.50');
    expect(ratioPercent('4057.50', '4588.00')).toBe('88.4');
    expect(ratioPercent('1', '3')).toBe('33.3');
    expect(ratioPercent('5', '0')).toBeNull();
  });

  it('validates and normalizes what users type', () => {
    expect(isValidMoneyInput('50')).toBe(true);
    expect(isValidMoneyInput('50,5')).toBe(true);
    expect(isValidMoneyInput('50.555')).toBe(false);
    expect(isValidMoneyInput('-1')).toBe(false);
    expect(isValidMoneyInput('1e3')).toBe(false);
    expect(normalizeMoneyInput('12,5')).toBe('12.50');
    expect(normalizeMoneyInput(' 7 ')).toBe('7.00');
  });
});
