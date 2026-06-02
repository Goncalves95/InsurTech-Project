import { Routes } from '@angular/router';

export const backofficeRoutes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./backoffice-dashboard/backoffice-dashboard').then(m => m.BackofficeDashboard),
  },
  {
    path: 'review-queue',
    loadComponent: () =>
      import('./review-queue/review-queue').then(m => m.ReviewQueue),
  },
];
