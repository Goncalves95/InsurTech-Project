import { Injectable, signal, computed } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly kc = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId,
  });

  private readonly _authenticated = signal(false);
  private readonly _username = signal<string | undefined>(undefined);
  private readonly _roles = signal<string[]>([]);
  private readonly _policyHolderId = signal<string>('');

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly isBackoffice = computed(() => this._roles().includes('backoffice'));

  async init(): Promise<void> {
    const authenticated = await this.kc.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
    });

    this._authenticated.set(authenticated);
    if (authenticated) {
      // Access tokens in this realm are opaque — all user claims come from the ID token.
      const idToken = this.kc.idTokenParsed as Record<string, unknown> | undefined;
      this._username.set(
        (idToken?.['preferred_username'] ?? this.kc.tokenParsed?.['preferred_username']) as string | undefined
      );
      const realmAccess = (idToken?.['realm_access'] ?? this.kc.realmAccess) as { roles?: string[] } | undefined;
      this._roles.set(realmAccess?.roles ?? []);
      this._policyHolderId.set(
        (this.kc.subject ?? idToken?.['sub'] ?? '') as string,
      );
    }
  }

  login(): void {
    this.kc.login();
  }

  logout(): void {
    this.kc.logout({ redirectUri: window.location.origin });
  }

  async getToken(): Promise<string> {
    await this.kc.updateToken(30);
    return this.kc.token!;
  }

  get policyHolderId(): string {
    return this._policyHolderId();
  }
}
