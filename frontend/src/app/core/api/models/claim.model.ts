export type ClaimStatus =
  | 'PENDING_OCR'
  | 'OCR_PROCESSING'
  | 'PENDING_VALIDATION'
  | 'APPROVED'
  | 'REJECTED'
  | 'MANUAL_REVIEW_REQUIRED';

export type ReviewDecision = 'APPROVE' | 'REJECT';

export interface ReviewRequest {
  decision: ReviewDecision;
  notes: string;
}

export interface Claim {
  id: string;
  policyHolderId: string;
  status: ClaimStatus;
  reviewerNote: string | null;
  documentReference: string | null;
  totalAmount: number | null;
  deductible: number | null;
  reimbursableAmount: number | null;
  submittedAt: string;
  processedAt: string | null;
}
