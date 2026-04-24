import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'pokemon-game.admin.authenticated';
const EXPECTED_PASSWORD = 'admin';

/**
 * Stub auth for the admin panel. Stores the "authenticated" flag in
 * sessionStorage so the user isn't asked again in the same browser tab.
 * Will be replaced with real Keycloak OIDC when migrating to OCI.
 */
@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  readonly authenticated = signal<boolean>(this.readStoredFlag());

  authenticate(password: string | null): boolean {
    if (password === EXPECTED_PASSWORD) {
      this.authenticated.set(true);
      try {
        sessionStorage.setItem(STORAGE_KEY, '1');
      } catch {
        /* ignore */
      }
      return true;
    }
    return false;
  }

  logout(): void {
    this.authenticated.set(false);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  }

  private readStoredFlag(): boolean {
    try {
      return sessionStorage.getItem(STORAGE_KEY) === '1';
    } catch {
      return false;
    }
  }
}
