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

      // Keycloak 26 omits profile claims from the ID token by default when access
      // tokens are opaque. loadUserProfile() hits /account directly — most reliable.
      const nameFromToken = (idToken?.['given_name'] ?? idToken?.['name'] ?? idToken?.['preferred_username']) as string | undefined;
      if (nameFromToken) {
        this._username.set(nameFromToken);
      } else {
        const profile = await this.kc.loadUserProfile().catch(() => null);
        this._username.set(profile?.firstName ?? profile?.username);
      }

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
    // If the id_token_hint is stale (e.g. after a Keycloak realm reset), the
    // OIDC logout endpoint rejects it. Fall back to a hard redirect so the
    // app re-enters the login-required flow cleanly.
    this.kc.logout({ redirectUri: window.location.origin })
      .catch(() => { window.location.assign(window.location.origin); });
  }

  async getToken(): Promise<string> {
    await this.kc.updateToken(30);
    return this.kc.token!;
  }

  get policyHolderId(): string {
    return this._policyHolderId();
  }
}
