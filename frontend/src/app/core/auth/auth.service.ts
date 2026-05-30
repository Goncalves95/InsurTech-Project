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

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly isBackoffice = computed(() => this._roles().includes('backoffice'));

  async init(): Promise<void> {
    const authenticated = await this.kc.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
    });

    this._authenticated.set(authenticated);
    if (authenticated) {
      this._username.set(this.kc.tokenParsed?.['preferred_username']);
      this._roles.set(this.kc.realmAccess?.roles ?? []);
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
    return this.kc.tokenParsed?.['sub'] ?? '';
  }
}
