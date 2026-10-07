import { Component, computed, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';

type Lang = 'es' | 'en';

@Component({
  selector: 'app-language-switcher',
  imports: [TranslateModule],
  templateUrl: './language-switcher.html',
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
