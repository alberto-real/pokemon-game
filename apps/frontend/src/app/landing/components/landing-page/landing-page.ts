import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BirthdayPanelComponent } from '../../../shared/components/birthday-panel/birthday-panel';

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, TranslateModule, BirthdayPanelComponent],
  templateUrl: './landing-page.html',
})
export class LandingPageComponent {}
