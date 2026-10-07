import { Routes } from '@angular/router';
import { adminGuard } from './admin/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./landing/components/landing-page/landing-page').then((m) => m.LandingPageComponent),
  },
  {
    path: 'game',
    loadComponent: () =>
      import('./game/components/game-page/game-page').then((m) => m.GamePageComponent),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./admin/components/admin-page/admin-page').then((m) => m.AdminPageComponent),
  },
  { path: '**', redirectTo: '' },
];
