import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { backofficeGuard } from './core/auth/backoffice.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'portal',
    pathMatch: 'full',
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
