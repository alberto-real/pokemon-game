import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  template: `
    <div class="hero min-h-[60vh]">
      <div class="hero-content text-center">
        <div>
          <h1 class="text-5xl font-bold">{{ 'app.title' | translate }}</h1>
          <a routerLink="/game" class="btn btn-primary btn-lg mt-6">
            {{ 'landing.cta' | translate }}
          </a>
        </div>
      </div>
    </div>
  `,
})
export class LandingPageComponent {}
