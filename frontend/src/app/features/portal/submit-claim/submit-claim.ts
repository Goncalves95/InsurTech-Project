import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClaimsApiService } from '../../../core/api/claims-api.service';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-submit-claim',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink,
  ],
  templateUrl: './submit-claim.html',
  styleUrl: './submit-claim.scss',
})
export class SubmitClaim {
  private readonly fb = inject(FormBuilder);
  private readonly claimsApi = inject(ClaimsApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.group({
    document: [null as File | null, Validators.required],
  });

  readonly selectedFile = signal<File | null>(null);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
    this.form.patchValue({ document: file });
  }

  onSubmit(): void {
    const file = this.selectedFile();
    if (!file || this.form.invalid) return;

    this.submitting.set(true);
    this.submitError.set(null);

    this.claimsApi.submit(this.authService.policyHolderId, file).subscribe({
      next: () => this.router.navigate(['/portal/claims']),
      error: () => {
        this.submitError.set('Failed to submit claim. Please try again.');
        this.submitting.set(false);
      },
    });
  }
}
