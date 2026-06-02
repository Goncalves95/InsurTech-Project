import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AuthService } from './auth.service';

// vi.hoisted runs before any imports and before mock hoisting — the only safe
// place to define values that are referenced inside a vi.mock factory.
const mockKcInstance = vi.hoisted(() => ({
  init: vi.fn().mockResolvedValue(false),
  subject: undefined as string | undefined,
  tokenParsed: null as Record<string, unknown> | null,
  idTokenParsed: null as Record<string, unknown> | null,
  realmAccess: undefined as { roles: string[] } | undefined,
  token: undefined as string | undefined,
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  updateToken: vi.fn().mockResolvedValue(true),
  loadUserProfile: vi.fn().mockResolvedValue({ firstName: undefined as string | undefined, username: undefined as string | undefined }),
}));

// The factory must use a regular function so it can be called with `new`.
// Returning an object from a constructor causes `new Keycloak()` to evaluate
// to that object — this gives us a stable reference we can mutate per-test.
vi.mock('keycloak-js', () => ({
  default: vi.fn(function MockKeycloak() {
    return mockKcInstance;
  }),
}));

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    mockKcInstance.init.mockResolvedValue(false);
    mockKcInstance.subject = undefined;
    mockKcInstance.tokenParsed = null;
    mockKcInstance.idTokenParsed = null;
    mockKcInstance.realmAccess = undefined;
    mockKcInstance.token = undefined;
    mockKcInstance.loadUserProfile.mockResolvedValue({ firstName: undefined, username: undefined });

    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  describe('initial state (before init)', () => {
    it('is not authenticated', () => {
      expect(service.authenticated()).toBe(false);
    });

    it('has no username', () => {
      expect(service.username()).toBeUndefined();
    });

    it('is not a backoffice user', () => {
      expect(service.isBackoffice()).toBe(false);
    });

    it('returns an empty policyHolderId', () => {
      expect(service.policyHolderId).toBe('');
    });
  });

  describe('after a failed / unauthenticated init', () => {
    it('keeps authenticated false when Keycloak returns false', async () => {
      mockKcInstance.init.mockResolvedValue(false);
      await service.init();
      expect(service.authenticated()).toBe(false);
    });
  });

  describe('after a successful init', () => {
    beforeEach(async () => {
      mockKcInstance.init.mockResolvedValue(true);
      mockKcInstance.idTokenParsed = { sub: 'user-uuid-001' };
      mockKcInstance.realmAccess = { roles: ['user', 'backoffice'] };
      mockKcInstance.subject = 'user-uuid-001';
      mockKcInstance.loadUserProfile.mockResolvedValue({ firstName: 'joao', username: 'joao' });
      await service.init();
    });

    it('sets authenticated to true', () => {
      expect(service.authenticated()).toBe(true);
    });

    it('sets username from the user profile', () => {
      expect(service.username()).toBe('joao');
    });

    it('derives isBackoffice from realm roles', () => {
      expect(service.isBackoffice()).toBe(true);
    });
  });

  it('isBackoffice remains false when the backoffice role is absent', async () => {
    mockKcInstance.init.mockResolvedValue(true);
    mockKcInstance.tokenParsed = { preferred_username: 'joao' };
    mockKcInstance.realmAccess = { roles: ['user'] };
    await service.init();
    expect(service.isBackoffice()).toBe(false);
  });
});
