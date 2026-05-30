import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { Claim, ClaimStatus } from '../../../core/api/models/claim.model';

@Component({
  selector: 'app-review-queue',
  imports: [
    StatusChip,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    FormsModule,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './review-queue.html',
  styleUrl: './review-queue.scss',
})
export class ReviewQueue implements OnInit {
  private readonly claimsApi = inject(ClaimsApiService);

  readonly claims = signal<Claim[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  selectedStatus: ClaimStatus = 'PENDING_VALIDATION';
  readonly statuses: ClaimStatus[] = [
    'PENDING_OCR',
    'OCR_PROCESSING',
    'PENDING_VALIDATION',
    'APPROVED',
    'REJECTED',
    'MANUAL_REVIEW',
  ];
  readonly displayedColumns = [
    'id',
    'policyHolderId',
    'submittedAt',
    'status',
    'totalAmount',
    'reimbursableAmount',
  ];

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading.set(true);
    this.error.set(null);
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

  formatStatus(status: ClaimStatus): string {
    return status.replace(/_/g, ' ');
  }
}
