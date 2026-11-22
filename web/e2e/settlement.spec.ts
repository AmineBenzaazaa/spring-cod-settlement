import { fileURLToPath } from 'node:url';
import { expect, test, type Page } from '@playwright/test';

const SAMPLE_CSV = fileURLToPath(new URL('../../fixtures/sample_remittance.csv', import.meta.url));

async function login(page: Page, user: 'atlas' | 'ops') {
  await page.goto('/login');
  await page.getByTestId(`demo-${user}`).click();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByTestId('user-chip')).toContainText(user);
}

/**
 * The demo story through nginx and the real API: a merchant uploads the courier's October CSV,
 * sees it reconciled in the background (11 matched + one of each discrepancy), disputes a line,
 * then finance ops resolves the dispute.
 *
 * Re-runnable on the same database: a second upload of the same bytes hits the SHA-256
 * idempotency check and the test follows the link to the existing statement. Each run disputes
 * one of the three discrepancies (`docker compose down -v` resets the demo data).
 */
test('merchant uploads, reconciles and disputes; ops resolves', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
  await login(page, 'atlas');

  // ---- upload -------------------------------------------------------------------------------
  await page.getByRole('link', { name: 'Statements' }).click();
  await page.getByLabel('Courier').selectOption({ label: 'Speedex Colis · speedex' });
  await page.getByLabel('Period start').fill('2026-10-01');
  await page.getByLabel('Period end').fill('2026-10-31');
  await page.getByTestId('file-input').setInputFiles(SAMPLE_CSV);
  await expect(page.getByTestId('picked-file')).toHaveText('sample_remittance.csv');

  const uploaded = page.waitForResponse((r) => r.url().endsWith('/api/statements') && r.request().method() === 'POST');
  await page.getByTestId('upload-submit').click();
  const response = await uploaded;
  const body = await response.json();
  if (body.duplicate) {
    expect(response.status()).toBe(200);
    await expect(page.getByTestId('duplicate-notice')).toContainText('already uploaded as statement');
    await page.getByTestId('open-existing').click();
  } else {
    expect(response.status()).toBe(202);
    await expect(page.getByTestId('statement-row').first()).toHaveAttribute('data-id', String(body.statement.id));
    await page.getByTestId('open-uploaded').click();
  }

  // ---- reconciliation (polled until the async processing is done) ----------------------------
  await expect(page).toHaveURL(new RegExp(`/statements/${body.statement.id}$`));
  await expect(page.getByTestId('recon-summary')).toBeVisible();
  await expect(page.getByTestId('count-MATCHED')).toHaveText('11');
  await expect(page.getByTestId('count-AMOUNT_MISMATCH')).toHaveText('1');
  await expect(page.getByTestId('count-UNKNOWN_TRACKING')).toHaveText('1');
  await expect(page.getByTestId('count-MISSING_FROM_STATEMENT')).toHaveText('1');
  await expect(page.locator('[data-testid="recon-row"][data-outcome="MATCHED"]')).toHaveCount(11);
  await expect(page.locator('[data-testid="recon-row"][data-outcome="AMOUNT_MISMATCH"]')).toContainText('−50.00');

  // ---- open a dispute on the first discrepancy that has none yet -----------------------------
  await page.getByTestId('filter-ISSUES').click();
  await expect(page.getByTestId('recon-row')).toHaveCount(3);
  const row = page.getByTestId('recon-row').filter({ has: page.getByTestId('open-dispute') }).first();
  const tracking = await row.getAttribute('data-tracking');
  await row.getByTestId('open-dispute').click();
  const dialog = page.getByTestId('dispute-dialog');
  await expect(dialog.getByLabel('Reason')).not.toHaveValue('');
  const opened = page.waitForResponse((r) => r.url().endsWith('/api/disputes') && r.request().method() === 'POST');
  await dialog.getByTestId('submit-dispute').click();
  const dispute = await (await opened).json();
  expect(dispute.status).toBe('OPEN');
  await expect(page.locator(`[data-testid="recon-row"][data-tracking="${tracking}"]`).getByTestId('disputed')).toBeVisible();

  // ---- finance ops resolves it --------------------------------------------------------------
  await page.getByTestId('user-menu').click();
  await page.getByTestId('logout').click();
  await login(page, 'ops');
  await page.getByRole('link', { name: 'Disputes' }).click();
  await page.getByTestId('dispute-filter-OPEN').click();
  await page.locator(`[data-testid="dispute-row"][data-id="${dispute.id}"]`).click();
  const drawer = page.getByTestId('dispute-drawer');
  await expect(drawer).toContainText(tracking!);
  await drawer.getByLabel('Resolution note').fill('Courier confirmed and will pay with the next remittance.');
  await drawer.getByTestId('resolve-dispute').click();
  await expect(drawer.getByTestId('badge-status')).toHaveAttribute('data-value', 'RESOLVED');
  await expect(drawer.getByTestId('resolve-panel')).toHaveCount(0);
});
