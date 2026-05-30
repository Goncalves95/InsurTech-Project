import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { Claim } from '../../../core/api/models/claim.model';

@Component({
  selector: 'app-my-claims',
  imports: [
    StatusChip,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './my-claims.html',
  styleUrl: './my-claims.scss',
})
export class MyClaims implements OnInit {
  private readonly claimsApi = inject(ClaimsApiService);
  private readonly authService = inject(AuthService);

  readonly claims = signal<Claim[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly displayedColumns = ['submittedAt', 'status', 'totalAmount', 'reimbursableAmount'];

  ngOnInit(): void {
    const policyHolderId = this.authService.policyHolderId;
    if (!policyHolderId) {
      this.error.set('Session not ready. Please refresh the page.');
      this.loading.set(false);
      return;
    }

    this.claimsApi.getByPolicyHolder(policyHolderId).subscribe({
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
}
