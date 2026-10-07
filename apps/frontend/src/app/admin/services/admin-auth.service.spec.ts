import { TestBed } from '@angular/core/testing';

import { AdminAuthService } from './admin-auth.service';

describe('AdminAuthService', () => {
  beforeEach(() => sessionStorage.clear());

  it('starts unauthenticated', () => {
    expect(TestBed.inject(AdminAuthService).authenticated()).toBe(false);
  });

  it('rejects a wrong password', () => {
    const service = TestBed.inject(AdminAuthService);
    expect(service.authenticate('nope')).toBe(false);
    expect(service.authenticated()).toBe(false);
  });

  it('accepts the right password and persists it for the session', () => {
    const service = TestBed.inject(AdminAuthService);
    expect(service.authenticate('admin')).toBe(true);
    expect(service.authenticated()).toBe(true);
    expect(sessionStorage.getItem('pokemon-game.admin.authenticated')).toBe('1');
  });

  it('restores the flag from sessionStorage', () => {
    sessionStorage.setItem('pokemon-game.admin.authenticated', '1');
    expect(TestBed.inject(AdminAuthService).authenticated()).toBe(true);
  });

  it('logout clears the flag', () => {
    const service = TestBed.inject(AdminAuthService);
    service.authenticate('admin');
    service.logout();
    expect(service.authenticated()).toBe(false);
    expect(sessionStorage.getItem('pokemon-game.admin.authenticated')).toBeNull();
  });
});
