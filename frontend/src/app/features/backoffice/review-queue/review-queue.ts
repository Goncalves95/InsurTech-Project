import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { Claim, ClaimStatus, ReviewDecision } from '../../../core/api/models/claim.model';

@Component({
  selector: 'app-review-queue',
  imports: [
    StatusChip,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatChipsModule,
    FormsModule,
    DatePipe,
  ],
  templateUrl: './review-queue.html',
  styleUrl: './review-queue.scss',
})
export class ReviewQueue implements OnInit {
  private readonly claimsApi = inject(ClaimsApiService);

  readonly claims = signal<Claim[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedClaim = signal<Claim | null>(null);
  readonly reviewNotes = signal('');
  readonly reviewSubmitting = signal(false);
  readonly reviewError = signal<string | null>(null);

  selectedStatus: ClaimStatus = 'MANUAL_REVIEW_REQUIRED';
  readonly statuses: ClaimStatus[] = [
    'PENDING_OCR',
    'OCR_PROCESSING',
    'PENDING_VALIDATION',
    'APPROVED',
    'REJECTED',
    'MANUAL_REVIEW_REQUIRED',
  ];
  readonly displayedColumns = ['id', 'policyHolderId', 'submittedAt', 'status', 'actions'];
  readonly detailColumns = ['expandedDetail'];

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading.set(true);
    this.error.set(null);
    this.expandedClaim.set(null);
    this.claimsApi.getByStatus(this.selectedStatus).subscribe({
      next: claims => {
        this.claims.set(claims);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load claims. Please try again.');
        this.loading.set(false);
      },
    });
  }

  toggleExpand(claim: Claim): void {
    if (claim.status !== 'MANUAL_REVIEW_REQUIRED') return;
    const current = this.expandedClaim();
    this.expandedClaim.set(current?.id === claim.id ? null : claim);
    this.reviewNotes.set('');
    this.reviewError.set(null);
  }

  submitReview(decision: ReviewDecision): void {
    const claim = this.expandedClaim();
    if (!claim) return;

    this.reviewSubmitting.set(true);
    this.reviewError.set(null);

    this.claimsApi.reviewClaim(claim.id, { decision, notes: this.reviewNotes() }).subscribe({
      next: updated => {
        this.claims.update(list =>
          list.map(c => (c.id === updated.id ? updated : c))
        );
        this.expandedClaim.set(null);
        this.reviewSubmitting.set(false);
      },
      error: () => {
        this.reviewError.set('Failed to submit review. Please try again.');
        this.reviewSubmitting.set(false);
      },
    });
  }

  isExpanded(claim: Claim): boolean {
    return this.expandedClaim()?.id === claim.id;
  }

  formatStatus(status: ClaimStatus): string {
    return status.replace(/_/g, ' ');
  }
}
