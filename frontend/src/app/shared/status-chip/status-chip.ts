import { Component, input } from '@angular/core';
import { ClaimStatus } from '../../core/api/models/claim.model';

@Component({
  selector: 'app-status-chip',
  imports: [],
  templateUrl: './status-chip.html',
  styleUrl: './status-chip.scss',
})
export class StatusChip {
  readonly status = input.required<ClaimStatus>();

  get cssClass(): string {
    switch (this.status()) {
      case 'APPROVED':
        return 'chip chip--approved';
      case 'REJECTED':
        return 'chip chip--rejected';
      case 'MANUAL_REVIEW_REQUIRED':
        return 'chip chip--manual';
      case 'PENDING_VALIDATION':
        return 'chip chip--pending-validation';
      case 'OCR_PROCESSING':
        return 'chip chip--processing';
      default:
        return 'chip chip--default';
    }
  }

  get label(): string {
    return this.status().replace(/_/g, ' ');
  }
}
