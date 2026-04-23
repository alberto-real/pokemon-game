import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { LanguageSwitcherComponent } from './i18n/language-switcher.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, TranslateModule, LanguageSwitcherComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
