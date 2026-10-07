import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { LanguageService } from '../../services/language.service';
import { LanguageSwitcherComponent } from './language-switcher';

describe('LanguageSwitcherComponent', () => {
  const current = signal<'es' | 'en'>('es');
  const use = vi.fn((l: 'es' | 'en') => current.set(l));

  beforeEach(async () => {
    current.set('es');
    use.mockClear();
    await TestBed.configureTestingModule({
      imports: [LanguageSwitcherComponent],
      providers: [provideTranslateService(), { provide: LanguageService, useValue: { current, use } }],
    }).compileComponents();
  });

  it('shows the current language label', () => {
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('summary').textContent).toContain('Español');
  });

  it('switches language when an option is clicked', () => {
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('li button') as NodeListOf<HTMLButtonElement>;
    buttons[1].click();
    fixture.detectChanges();
    expect(use).toHaveBeenCalledWith('en');
    expect(fixture.nativeElement.querySelector('summary').textContent).toContain('English');
  });
});
