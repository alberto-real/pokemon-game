import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { LanguageSwitcherComponent } from './i18n/components/language-switcher/language-switcher';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, TranslateModule, LanguageSwitcherComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
