export type ClaimStatus =
  | 'PENDING_OCR'
  | 'OCR_PROCESSING'
  | 'PENDING_VALIDATION'
  | 'APPROVED'
  | 'REJECTED'
  | 'MANUAL_REVIEW';

export interface Claim {
  id: string;
  policyHolderId: string;
  status: ClaimStatus;
  documentReference: string;
  totalAmount: number | null;
  deductible: number | null;
  reimbursableAmount: number | null;
  submittedAt: string;
  processedAt: string | null;
}
