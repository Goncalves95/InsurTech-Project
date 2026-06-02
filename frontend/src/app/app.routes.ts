import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { backofficeGuard } from './core/auth/backoffice.guard';
import { AuthService } from './core/auth/auth.service';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const auth = inject(AuthService);
      return auth.isBackoffice() ? '/backoffice/dashboard' : '/portal';
    },
  },
  {
    path: 'portal',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/portal/portal.routes').then(m => m.portalRoutes),
  },
  {
    path: 'backoffice',
    canActivate: [authGuard, backofficeGuard],
    loadChildren: () =>
      import('./features/backoffice/backoffice.routes').then(m => m.backofficeRoutes),
  },
];
