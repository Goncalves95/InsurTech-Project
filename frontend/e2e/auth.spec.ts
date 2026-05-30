import { test, expect } from '@playwright/test';

/**
 * Smoke tests — verify the Angular app serves correctly and
 * that the auth guard redirects unauthenticated users to Keycloak.
 *
 * These tests require the full Docker stack running:
 *   docker-compose up postgres kafka keycloak backend
 *   npm start   (or docker-compose up frontend)
 *
 * In CI the webServer block in playwright.config.ts starts ng serve automatically.
 * Keycloak must be reachable at http://localhost:8180.
 */

test.describe('Authentication guard', () => {
  test('redirects unauthenticated users to the Keycloak login page', async ({ page }) => {
    await page.goto('/portal/claims');

    // Keycloak redirects happen synchronously — wait for the URL to change
    await page.waitForURL(/localhost:8180\/realms\/insurtech/, { timeout: 15_000 });

    await expect(page).toHaveURL(/realms\/insurtech/);
    // The Keycloak login form is rendered
    await expect(page.locator('#kc-form-login, #kc-page-title')).toBeVisible();
  });

  test('root path also triggers the auth redirect', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL(/localhost:8180\/realms\/insurtech/, { timeout: 15_000 });
    await expect(page).toHaveURL(/realms\/insurtech/);
  });
});

/**
 * Post-authentication tests.
 * These require a valid Keycloak user and a running backend — run locally only.
 */
test.describe('Authenticated portal', () => {
  test.skip(!process.env['E2E_USERNAME'], 'Set E2E_USERNAME and E2E_PASSWORD to run authenticated tests');

  test.beforeEach(async ({ page }) => {
    await page.goto('/portal/claims');
    await page.waitForURL(/localhost:8180/);

    await page.fill('#username', process.env['E2E_USERNAME']!);
    await page.fill('#password', process.env['E2E_PASSWORD']!);
    await page.click('#kc-login');

    await page.waitForURL(/localhost:4200\/portal/);
  });

  test('My Claims page loads after login', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'My Claims' })).toBeVisible();
  });

  test('Submit New Claim button is visible', async ({ page }) => {
    await expect(page.getByRole('button', { name: /submit new claim/i })).toBeVisible();
  });
});
