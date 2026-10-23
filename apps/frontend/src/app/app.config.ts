import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
    provideTranslateService({
      fallbackLang: 'es',
    }),
    // Cache-busting query: translation files are not fingerprinted, and clients
    // that cached them before nginx sent no-cache would otherwise keep a stale copy.
    provideTranslateHttpLoader({ prefix: '/i18n/', suffix: `.json?v=${Date.now()}` }),
  ],
};
