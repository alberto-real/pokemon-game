import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, TranslateModule],
  templateUrl: './landing-page.html',
})
export class LandingPageComponent {}
