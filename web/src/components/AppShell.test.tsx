import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { sessionStore } from '@/lib/session';
import { renderWithProviders, signIn } from '@/test/utils';
import { AppShell } from './AppShell';

const app = (
  <Routes>
    <Route element={<AppShell />}><Route path="/disputes" element={<p>disputes page</p>} /></Route>
  </Routes>
);

describe('AppShell', () => {
  afterEach(() => sessionStore.set(null));

  it('switches between the suite products from the brand lockup', async () => {
    signIn();
    renderWithProviders(app, { path: '/disputes' });
    expect(document.title).toBe('Disputes · MSE Settle');

    await userEvent.click(screen.getByTestId('product-switcher'));
    expect(screen.getByTestId('product-switcher')).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByTestId('product-dispatch')).toHaveAttribute('href', 'http://localhost:3100');
    expect(screen.getByTestId('product-copilot')).toHaveAttribute('href', 'http://localhost:3300');
    expect(screen.getByTestId('product-settle')).toHaveAttribute('aria-current', 'page');

    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('signs out from the user menu', async () => {
    signIn({ username: 'ops', role: 'STAFF', merchantId: null });
    renderWithProviders(app, { path: '/disputes' });
    expect(screen.getByTestId('user-chip')).toHaveTextContent('ops');
    await userEvent.click(screen.getByTestId('user-menu'));
    await userEvent.click(screen.getByTestId('how-it-works-open'));
    const dialog = await screen.findByTestId('how-it-works');
    expect(dialog).toHaveTextContent('How MSE Transport works');
    expect(dialog.querySelectorAll('li')).toHaveLength(6);
    await userEvent.keyboard('{Escape}');

    await userEvent.click(screen.getByTestId('user-menu'));
    await userEvent.click(screen.getByTestId('logout'));
    expect(sessionStore.get()).toBeNull();
  });
});
