import { Routes } from '@angular/router';
import { adminGuard } from './admin/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./landing/landing-page.component').then(
        (m) => m.LandingPageComponent,
      ),
  },
  {
    path: 'game',
    loadComponent: () =>
      import('./game/game-page.component').then((m) => m.GamePageComponent),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./admin/admin-page.component').then((m) => m.AdminPageComponent),
  },
  { path: '**', redirectTo: '' },
];
