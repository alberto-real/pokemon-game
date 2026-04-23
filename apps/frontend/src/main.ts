import { bootstrapApplication } from '@angular/platform-browser';
import { EnvironmentInjector } from '@angular/core';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import { LanguageService } from './app/i18n/language.service';

bootstrapApplication(App, appConfig)
  .then((ref) => {
    const injector = ref.injector.get(EnvironmentInjector);
    injector.get(LanguageService).init();
  })
  .catch((err) => console.error(err));
