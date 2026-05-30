import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (!authService.authenticated()) {
    return next(req);
  }

  return from(authService.getToken()).pipe(
    switchMap(token =>
      next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }))
    )
  );
};
