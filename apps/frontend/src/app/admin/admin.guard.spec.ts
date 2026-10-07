import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';

import { adminGuard } from './admin.guard';
import { AdminAuthService } from './services/admin-auth.service';

describe('adminGuard', () => {
  const run = () => TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });

  it('lets an authenticated admin through without prompting', () => {
    TestBed.inject(AdminAuthService).authenticate('admin');
    const prompt = vi.spyOn(window, 'prompt');
    expect(run()).toBe(true);
    expect(prompt).not.toHaveBeenCalled();
  });

  it('allows access after the correct password', () => {
    vi.spyOn(window, 'prompt').mockReturnValue('admin');
    expect(run()).toBe(true);
  });

  it('redirects home on a wrong password', () => {
    vi.spyOn(window, 'prompt').mockReturnValue('wrong');
    const result = run() as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/');
  });

  it('redirects home when the prompt is cancelled', () => {
    vi.spyOn(window, 'prompt').mockReturnValue(null);
    expect(run()).toBeInstanceOf(UrlTree);
  });
});
