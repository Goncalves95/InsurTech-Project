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
      this._username.set(this.kc.tokenParsed?.['preferred_username']);
      this._roles.set(this.kc.realmAccess?.roles ?? []);

      // Access tokens in this realm are opaque, so kc.subject and kc.tokenParsed are null.
      // The subject is reliably available in the ID token (idTokenParsed.sub).
      const idToken = this.kc.idTokenParsed as Record<string, unknown> | undefined;
      const accessToken = this.kc.tokenParsed as Record<string, unknown> | undefined;
      this._policyHolderId.set(
        (this.kc.subject ?? idToken?.['sub'] ?? accessToken?.['sub'] ?? '') as string,
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
