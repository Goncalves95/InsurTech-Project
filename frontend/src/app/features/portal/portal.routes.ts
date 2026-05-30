import { Routes } from '@angular/router';

export const portalRoutes: Routes = [
  {
    path: '',
    redirectTo: 'claims',
    pathMatch: 'full',
  },
  {
    path: 'claims',
    loadComponent: () => import('./my-claims/my-claims').then(m => m.MyClaims),
  },
  {
    path: 'submit',
    loadComponent: () => import('./submit-claim/submit-claim').then(m => m.SubmitClaim),
  },
];
