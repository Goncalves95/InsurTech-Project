import { Routes } from '@angular/router';

export const backofficeRoutes: Routes = [
  {
    path: '',
    redirectTo: 'review-queue',
    pathMatch: 'full',
  },
  {
    path: 'review-queue',
    loadComponent: () =>
      import('./review-queue/review-queue').then(m => m.ReviewQueue),
  },
];
