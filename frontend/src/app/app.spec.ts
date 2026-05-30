import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';

const authServiceMock = {
  authenticated: signal(false),
  username: signal<string | undefined>(undefined),
  isBackoffice: signal(false),
  login: vi.fn(),
  logout: vi.fn(),
};

describe('App shell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('creates the root component', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the brand name in the toolbar', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('InsurTech Platform');
  });

  it('shows a Login button when the user is not authenticated', async () => {
    authServiceMock.authenticated.set(false);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Login');
  });

  it('shows My Claims and Logout when the user is authenticated', async () => {
    authServiceMock.authenticated.set(true);
    authServiceMock.username.set('joao');
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('My Claims');
    expect(compiled.textContent).toContain('joao');
  });
});
