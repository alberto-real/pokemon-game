import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AdminAuthService } from './admin-auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);

  if (auth.authenticated()) return true;

  const pass = window.prompt('Password');
  if (pass === null) {
    return router.createUrlTree(['/']);
  }
  if (auth.authenticate(pass)) {
    return true;
  }
  window.alert('Password incorrecto');
  return router.createUrlTree(['/']);
};
