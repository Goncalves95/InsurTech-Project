import { render, screen } from '@testing-library/angular';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { NEVER, of, throwError } from 'rxjs';
import { MyClaims } from './my-claims';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Claim } from '../../../core/api/models/claim.model';

// Prevent the real Keycloak constructor from running inside AuthService
vi.mock('keycloak-js', () => ({
  default: vi.fn().mockImplementation(() => ({
    init: vi.fn().mockResolvedValue(false),
    tokenParsed: null,
    idTokenParsed: null,
    realmAccess: undefined,
    subject: undefined,
  })),
}));

const MOCK_CLAIM: Claim = {
  id: 'claim-001',
  policyHolderId: 'user-001',
  status: 'APPROVED',
  reviewerNote: null,
  documentReference: 'invoice.pdf',
  totalAmount: 250,
  deductible: 50,
  reimbursableAmount: 200,
  submittedAt: '2024-05-01T10:00:00Z',
  processedAt: '2024-05-02T10:00:00Z',
};

interface RenderOptions {
  policyHolderId?: string;
  claims?: Claim[];
  apiError?: boolean;
  pending?: boolean;
}

function renderMyClaims({ policyHolderId = 'user-001', claims = [], apiError = false, pending = false }: RenderOptions = {}) {
  const claimsApi$ = pending
    ? NEVER
    : apiError
      ? throwError(() => new Error('API error'))
      : of(claims);

  return render(MyClaims, {
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { policyHolderId, username: () => 'Jose' } },
      { provide: ClaimsApiService, useValue: { getByPolicyHolder: vi.fn().mockReturnValue(claimsApi$) } },
    ],
  });
}

describe('MyClaims', () => {
  it('shows a loading spinner while the request is in flight', async () => {
    await renderMyClaims({ pending: true });
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows the empty state message when the user has no claims', async () => {
    await renderMyClaims({ claims: [] });
    expect(screen.getByText('No claims submitted yet.')).toBeInTheDocument();
  });

  it('shows a session error when policyHolderId is empty', async () => {
    await renderMyClaims({ policyHolderId: '' });
    expect(screen.getByText(/session not ready/i)).toBeInTheDocument();
  });

  it('shows an error message when the API call fails', async () => {
    await renderMyClaims({ apiError: true });
    expect(screen.getByText(/failed to load claims/i)).toBeInTheDocument();
  });

  it('renders one row per claim returned by the API', async () => {
    await renderMyClaims({ claims: [MOCK_CLAIM] });
    // StatusChip renders the status label — presence confirms the row was rendered
    expect(screen.getByText('APPROVED')).toBeInTheDocument();
  });

  it('does not show the spinner once data is loaded', async () => {
    await renderMyClaims({ claims: [MOCK_CLAIM] });
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });
});
