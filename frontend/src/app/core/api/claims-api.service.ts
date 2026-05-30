import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Claim, ClaimStatus } from './models/claim.model';

@Injectable({ providedIn: 'root' })
export class ClaimsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/claims`;

  submit(policyHolderId: string, document: File): Observable<Claim> {
    const form = new FormData();
    form.append('policyHolderId', policyHolderId);
    form.append('document', document);
    return this.http.post<Claim>(this.baseUrl, form);
  }

  getById(claimId: string): Observable<Claim> {
    return this.http.get<Claim>(`${this.baseUrl}/${claimId}`);
  }

  getByPolicyHolder(policyHolderId: string): Observable<Claim[]> {
    return this.http.get<Claim[]>(this.baseUrl, { params: { policyHolderId } });
  }

  getByStatus(status: ClaimStatus): Observable<Claim[]> {
    return this.http.get<Claim[]>(`${this.baseUrl}/status/${status}`);
  }
}
