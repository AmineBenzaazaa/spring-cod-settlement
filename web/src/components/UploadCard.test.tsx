import { vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api, ApiError } from '@/lib/api';
import type { Statement } from '@/lib/types';
import { renderWithProviders, signIn } from '@/test/utils';
import { UploadCard } from './UploadCard';

const statement: Statement = {
  id: 4, merchantId: 1, courierId: 1, periodStart: '2026-10-01', periodEnd: '2026-10-31', originalFilename: 'sample_remittance.csv',
  fileSha256: 'f'.repeat(64), currency: 'MAD', status: 'RECONCILED', errorMessage: null, lineCount: 13,
  uploadedAt: '2026-11-01T10:00:00Z', reconciledAt: '2026-11-01T10:00:01Z',
};

beforeEach(() => {
  signIn();
  vi.spyOn(api, 'couriers').mockResolvedValue({
    content: [{ id: 1, name: 'Speedex Colis', code: 'speedex', contactPhone: null, active: true, createdAt: '2026-01-01T00:00:00Z' }],
    page: 0, size: 100, totalElements: 1, totalPages: 1,
  });
});
afterEach(() => vi.restoreAllMocks());

const csv = () => new File(['tracking_number,amount_collected,fee\n'], 'sample_remittance.csv', { type: 'text/csv' });

async function fillAndDrop() {
  await screen.findByRole('option', { name: /Speedex/ });
  await userEvent.selectOptions(screen.getByLabelText('Courier'), '1');
  fireEvent.drop(screen.getByTestId('dropzone'), { dataTransfer: { files: [csv()] } });
  expect(screen.getByTestId('picked-file')).toHaveTextContent('sample_remittance.csv');
}

describe('UploadCard', () => {
  it('uploads a dropped CSV with courier and period', async () => {
    const upload = vi.spyOn(api, 'upload').mockResolvedValue({ duplicate: false, statement: { ...statement, status: 'UPLOADED' } });
    renderWithProviders(<UploadCard />);
    await fillAndDrop();
    fireEvent.change(screen.getByLabelText('Period start'), { target: { value: '2026-10-01' } });
    fireEvent.change(screen.getByLabelText('Period end'), { target: { value: '2026-10-31' } });
    await userEvent.click(screen.getByTestId('upload-submit'));

    expect(upload).toHaveBeenCalledWith(expect.objectContaining({ courierId: 1, periodStart: '2026-10-01', periodEnd: '2026-10-31' }));
    expect(await screen.findByTestId('upload-accepted')).toHaveTextContent('Statement #4 uploaded');
  });

  it('explains the SHA-256 duplicate check and links the existing statement', async () => {
    vi.spyOn(api, 'upload').mockResolvedValue({ duplicate: true, statement });
    renderWithProviders(<UploadCard />);
    await fillAndDrop();
    await userEvent.click(screen.getByTestId('upload-submit'));

    const notice = await screen.findByTestId('duplicate-notice');
    expect(notice).toHaveTextContent('Already uploaded');
    expect(notice).toHaveTextContent('same SHA-256');
    expect(screen.getByTestId('open-existing')).toHaveAttribute('href', '/statements/4');
  });

  it('lists every rejected row from the ProblemDetail', async () => {
    vi.spyOn(api, 'upload').mockRejectedValue(new ApiError(400, 'Invalid request', 'The request contains invalid data.', {
      file: ['Row 2: tracking_number is required.', 'Row 3: amount_collected is not a valid amount.'],
    }));
    renderWithProviders(<UploadCard />);
    await fillAndDrop();
    await userEvent.click(screen.getByTestId('upload-submit'));

    const alert = await screen.findByTestId('problem');
    expect(alert).toHaveTextContent('Row 2: tracking_number is required.');
    expect(alert).toHaveTextContent('Row 3: amount_collected is not a valid amount.');
  });

  it('refuses non-CSV files before calling the API', async () => {
    const upload = vi.spyOn(api, 'upload');
    renderWithProviders(<UploadCard />);
    fireEvent.drop(screen.getByTestId('dropzone'), { dataTransfer: { files: [new File(['x'], 'statement.xlsx')] } });
    expect(screen.getByRole('alert')).toHaveTextContent('Only .csv files are accepted');
    expect(upload).not.toHaveBeenCalled();
  });
});
