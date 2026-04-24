import { Component, computed, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from './language.service';

type Lang = 'es' | 'en';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <details class="dropdown dropdown-end">
      <summary class="btn btn-ghost btn-sm gap-1">
        <span>{{ currentLabel() }}</span>
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12"
             viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" clip-rule="evenodd"
            d="M5.23 7.21a.75.75 0 011.06.02L10 11.06l3.71-3.83a.75.75 0 111.08 1.04l-4.25 4.39a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z" />
        </svg>
      </summary>
      <ul class="menu dropdown-content bg-base-100 rounded-box z-10 w-40 p-2 shadow mt-2">
        <li>
          <button
            [class.menu-active]="lang.current() === 'es'"
            (click)="select('es')"
          >
            Español
          </button>
        </li>
        <li>
          <button
            [class.menu-active]="lang.current() === 'en'"
            (click)="select('en')"
          >
            English
          </button>
        </li>
      </ul>
    </details>
  `,
})
export class LanguageSwitcherComponent {
  protected readonly lang = inject(LanguageService);

  protected readonly currentLabel = computed(() =>
    this.lang.current() === 'es' ? 'Español' : 'English',
  );

  select(value: Lang): void {
    this.lang.use(value);
    // Close the <details> after picking
    const el = document.activeElement?.closest('details');
    (el as HTMLDetailsElement | null)?.removeAttribute('open');
  }
}
