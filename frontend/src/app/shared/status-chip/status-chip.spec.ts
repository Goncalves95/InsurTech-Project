import { render, screen } from '@testing-library/angular';
import { StatusChip } from './status-chip';
import { ClaimStatus } from '../../core/api/models/claim.model';

describe('StatusChip', () => {
  async function renderChip(status: ClaimStatus) {
    return render(StatusChip, { componentInputs: { status } });
  }

  it('replaces underscores with spaces in the visible label', async () => {
    await renderChip('PENDING_OCR');
    expect(screen.getByText('PENDING OCR')).toBeInTheDocument();
  });

  it('applies the approved modifier class for APPROVED', async () => {
    await renderChip('APPROVED');
    expect(screen.getByText('APPROVED')).toHaveClass('chip--approved');
  });

  it('applies the rejected modifier class for REJECTED', async () => {
    await renderChip('REJECTED');
    expect(screen.getByText('REJECTED')).toHaveClass('chip--rejected');
  });

  it('applies the manual modifier class for MANUAL_REVIEW_REQUIRED', async () => {
    await renderChip('MANUAL_REVIEW_REQUIRED');
    expect(screen.getByText('MANUAL REVIEW REQUIRED')).toHaveClass('chip--manual');
  });

  it('applies the pending-validation modifier class for PENDING_VALIDATION', async () => {
    await renderChip('PENDING_VALIDATION');
    expect(screen.getByText('PENDING VALIDATION')).toHaveClass('chip--pending-validation');
  });

  it('applies the processing modifier class for OCR_PROCESSING', async () => {
    await renderChip('OCR_PROCESSING');
    expect(screen.getByText('OCR PROCESSING')).toHaveClass('chip--processing');
  });

  it('falls back to the default chip class for unknown statuses', async () => {
    await renderChip('PENDING_OCR');
    expect(screen.getByText('PENDING OCR')).toHaveClass('chip--default');
  });
});
