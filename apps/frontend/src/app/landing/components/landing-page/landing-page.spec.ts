import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { LandingPageComponent } from './landing-page';

describe('LandingPageComponent', () => {
  it('renders a call to action linking to the game', async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [provideRouter([]), provideTranslateService()],
    }).compileComponents();
    const fixture = TestBed.createComponent(LandingPageComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/game');
  });
});
