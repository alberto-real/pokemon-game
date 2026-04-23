import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

const STORAGE_KEY = 'pokemon-game.language';
const SUPPORTED = ['es', 'en'] as const;
type Lang = (typeof SUPPORTED)[number];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  readonly current = signal<Lang>('es');

  init(): void {
    this.translate.addLangs([...SUPPORTED]);
    this.translate.setDefaultLang('es');
    const saved = this.readSaved();
    const detected = saved ?? this.detectFromBrowser();
    this.use(detected);
  }

  use(lang: Lang): void {
    this.translate.use(lang);
    this.current.set(lang);
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      /* ignore storage errors */
    }
  }

  private readSaved(): Lang | null {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      return (SUPPORTED as readonly string[]).includes(v ?? '') ? (v as Lang) : null;
    } catch {
      return null;
    }
  }

  private detectFromBrowser(): Lang {
    const navLang = (typeof navigator !== 'undefined' && navigator.language) || 'es';
    return navLang.toLowerCase().startsWith('es') ? 'es' : 'en';
  }
}
