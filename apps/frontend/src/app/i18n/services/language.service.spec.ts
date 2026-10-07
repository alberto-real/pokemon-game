import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';

import { LanguageService } from './language.service';

describe('LanguageService', () => {
  const KEY = 'pokemon-game.language';

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
  });

  it('uses the saved language when present', () => {
    localStorage.setItem(KEY, 'en');
    const service = TestBed.inject(LanguageService);
    service.init();
    expect(service.current()).toBe('en');
  });

  it('falls back to the browser language', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('en-US');
    const service = TestBed.inject(LanguageService);
    service.init();
    expect(service.current()).toBe('en');
  });

  it('defaults to Spanish for Spanish browsers', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('es-ES');
    const service = TestBed.inject(LanguageService);
    service.init();
    expect(service.current()).toBe('es');
  });

  it('ignores unsupported saved values', () => {
    localStorage.setItem(KEY, 'fr');
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('es-ES');
    const service = TestBed.inject(LanguageService);
    service.init();
    expect(service.current()).toBe('es');
  });

  it('use() switches the translate service and persists the choice', () => {
    const service = TestBed.inject(LanguageService);
    const translate = TestBed.inject(TranslateService);
    const spy = vi.spyOn(translate, 'use');
    service.use('en');
    expect(spy).toHaveBeenCalledWith('en');
    expect(service.current()).toBe('en');
    expect(localStorage.getItem(KEY)).toBe('en');
  });
});
