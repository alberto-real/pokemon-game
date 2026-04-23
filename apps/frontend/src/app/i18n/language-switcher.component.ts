import { Component, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from './language.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <label class="form-control">
      <select
        class="select select-sm select-bordered"
        [value]="lang.current()"
        (change)="onChange($event)"
      >
        <option value="es">Español</option>
        <option value="en">English</option>
      </select>
    </label>
  `,
})
export class LanguageSwitcherComponent {
  protected readonly lang = inject(LanguageService);

  onChange(ev: Event): void {
    const v = (ev.target as HTMLSelectElement).value;
    if (v === 'es' || v === 'en') this.lang.use(v);
  }
}
