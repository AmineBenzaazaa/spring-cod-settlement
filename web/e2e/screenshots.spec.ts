import { expect, test, type Page } from '@playwright/test';

/**
 * Product screenshots for the README (docs/screenshots, 1440x900). Opt-in, run after the main e2e
 * so the sample statement is reconciled and disputed:  SCREENSHOTS=1 npx playwright test screenshots
 */
test.skip(!process.env.SCREENSHOTS, 'set SCREENSHOTS=1 to refresh the README screenshots');
test.use({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });

const out = (name: string) => `../docs/screenshots/${name}.png`;

async function signIn(page: Page, user: 'atlas' | 'ops', theme: 'light' | 'dark' = 'light') {
  await page.addInitScript((t) => { localStorage.setItem('settle.theme', t); localStorage.setItem('settle.lang', 'en'); }, theme);
  await page.goto('/login');
  await page.getByTestId(`demo-${user}`).click();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByTestId('user-chip')).toContainText(user);
}

test('README screenshots', async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem('settle.theme', 'light'); localStorage.setItem('settle.lang', 'en'); });
  await page.goto('/login');
  await expect(page.getByTestId('login-form')).toBeVisible();
  await page.screenshot({ path: out('login') });

  await signIn(page, 'atlas');
  await expect(page.getByTestId('balance-chart')).toBeVisible();
  await page.mouse.move(5, 890);
  await page.waitForTimeout(800);
  await page.screenshot({ path: out('overview') });

  await page.getByTestId('user-menu').click();
  await page.getByTestId('how-it-works-open').click();
  await expect(page.getByTestId('how-it-works')).toBeVisible();
  await page.waitForTimeout(400);
  await page.screenshot({ path: out('how-it-works') });
  await page.keyboard.press('Escape');

  await page.getByRole('link', { name: 'Statements' }).click();
  await page.getByLabel('Courier').selectOption({ label: 'Speedex Colis · speedex' });
  await page.getByLabel('Period start').fill('2026-10-01');
  await page.getByLabel('Period end').fill('2026-10-31');
  await expect(page.getByTestId('statement-row').first()).toBeVisible();
  await page.screenshot({ path: out('statements') });

  await page.getByTestId('statement-row').first().click();
  await expect(page.getByTestId('recon-summary')).toBeVisible();
  await page.waitForTimeout(400);
  await page.screenshot({ path: out('reconciliation') });

  await page.getByTestId('user-menu').click();
  await page.getByTestId('logout').click();
  await signIn(page, 'ops', 'dark');
  await page.getByRole('link', { name: 'Disputes' }).click();
  await page.getByTestId('dispute-row').first().click();
  await expect(page.getByTestId('dispute-drawer')).toBeVisible();
  await page.waitForTimeout(500);
  await page.screenshot({ path: out('disputes-dark') });
});
