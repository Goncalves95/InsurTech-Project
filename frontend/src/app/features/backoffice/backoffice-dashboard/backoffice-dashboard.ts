import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { ClaimStatus } from '../../../core/api/models/claim.model';

type StatusCounts = Record<ClaimStatus, number>;

const ALL_STATUSES: ClaimStatus[] = [
  'PENDING_OCR',
  'OCR_PROCESSING',
  'PENDING_VALIDATION',
  'APPROVED',
  'REJECTED',
  'MANUAL_REVIEW_REQUIRED',
];

const EMPTY_COUNTS: StatusCounts = {
  PENDING_OCR: 0, OCR_PROCESSING: 0, PENDING_VALIDATION: 0,
  APPROVED: 0, REJECTED: 0, MANUAL_REVIEW_REQUIRED: 0,
};

@Component({
  selector: 'app-backoffice-dashboard',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './backoffice-dashboard.html',
  styleUrl: './backoffice-dashboard.scss',
})
export class BackofficeDashboard implements OnInit {
  private readonly claimsApi = inject(ClaimsApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly counts = signal<StatusCounts>({ ...EMPTY_COUNTS });

  readonly total      = computed(() => Object.values(this.counts()).reduce((s, n) => s + n, 0));
  readonly approved   = computed(() => this.counts().APPROVED);
  readonly pending    = computed(() => this.counts().MANUAL_REVIEW_REQUIRED);
  readonly rejected   = computed(() => this.counts().REJECTED);
  readonly inPipeline = computed(() =>
    this.counts().PENDING_OCR + this.counts().OCR_PROCESSING + this.counts().PENDING_VALIDATION
  );

  readonly bars = computed(() => {
    const c = this.counts();
    const t = this.total() || 1;
    return [
      { label: 'Approved',           count: c.APPROVED,               pct: (c.APPROVED / t) * 100,               cls: 'approved'   },
      { label: 'Manual Review',      count: c.MANUAL_REVIEW_REQUIRED, pct: (c.MANUAL_REVIEW_REQUIRED / t) * 100, cls: 'manual'     },
      { label: 'Pending OCR',        count: c.PENDING_OCR,            pct: (c.PENDING_OCR / t) * 100,            cls: 'default'    },
      { label: 'OCR Processing',     count: c.OCR_PROCESSING,         pct: (c.OCR_PROCESSING / t) * 100,         cls: 'processing' },
      { label: 'Pending Validation', count: c.PENDING_VALIDATION,     pct: (c.PENDING_VALIDATION / t) * 100,     cls: 'validation' },
      { label: 'Rejected',           count: c.REJECTED,               pct: (c.REJECTED / t) * 100,               cls: 'rejected'   },
    ];
  });

  ngOnInit(): void {
    forkJoin(ALL_STATUSES.map(s => this.claimsApi.getByStatus(s))).subscribe({
      next: results => {
        const c = { ...EMPTY_COUNTS };
        ALL_STATUSES.forEach((s, i) => (c[s] = results[i].length));
        this.counts.set(c);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load dashboard data. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
