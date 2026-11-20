import { vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { api, ApiError } from '@/lib/api';
import { sessionStore } from '@/lib/session';
import { fakeJwt, renderWithProviders } from '@/test/utils';
import { LoginPage } from './LoginPage';

afterEach(() => vi.restoreAllMocks());

const app = (
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/" element={<p>home</p>} />
  </Routes>
);

describe('LoginPage', () => {
  it('is prefilled with a working demo account and the chips switch user', async () => {
    renderWithProviders(app, { path: '/login' });
    expect(screen.getByLabelText('Username')).toHaveValue('atlas');
    expect(screen.getByLabelText('Password')).toHaveValue('demo-pass-123');

    await userEvent.click(screen.getByTestId('demo-ops'));
    expect(screen.getByLabelText('Username')).toHaveValue('ops');
    expect(screen.getByLabelText('Password')).toHaveValue('demo-pass-123');
  });

  it('signs in and stores the session from the token claims', async () => {
    const login = vi.spyOn(api, 'login').mockResolvedValue({
      accessToken: fakeJwt({ sub: 'medina', role: 'MERCHANT', mid: 2, exp: 2_000_000_000 }),
      refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900,
    });
    renderWithProviders(app, { path: '/login' });
    await userEvent.click(screen.getByTestId('demo-medina'));
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(login).toHaveBeenCalledWith('medina', 'demo-pass-123');
    expect(await screen.findByText('home')).toBeInTheDocument();
    expect(sessionStore.get()).toMatchObject({ username: 'medina', merchantId: 2 });
  });

  it('shows a friendly message on bad credentials', async () => {
    vi.spyOn(api, 'login').mockRejectedValue(new ApiError(401, 'Unauthorized', 'Bad credentials'));
    renderWithProviders(app, { path: '/login' });
    await userEvent.clear(screen.getByLabelText('Password'));
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password.');
  });
});
