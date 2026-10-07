import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { GameApi } from '../../../api/game-api.service';
import { GameState } from '../../../api/types';
import { LanguageService } from '../../../i18n/services/language.service';
import { GameStateService } from '../../services/game-state.service';
import { GamePageComponent } from './game-page';

describe('GamePageComponent', () => {
  const bootstrap: GameState = {
    imageUrl: 'img.png',
    nameLength: 7,
    quizReady: false,
    quizStatus: 'PENDING',
    maxAttempts: 5,
  };
  const api = { today: vi.fn(), attempt: vi.fn(), surrender: vi.fn() };
  const lang = { current: signal<'es' | 'en'>('en') };

  async function setup() {
    await TestBed.configureTestingModule({
      imports: [GamePageComponent],
      providers: [
        provideTranslateService(),
        { provide: GameApi, useValue: api },
        { provide: LanguageService, useValue: lang },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(GamePageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    api.today.mockReset().mockResolvedValue(bootstrap);
    api.attempt.mockReset();
    api.surrender.mockReset();
    lang.current.set('en');
  });

  it('shows a spinner until the state is loaded, then the game', async () => {
    const fixture = await setup();
    expect(fixture.nativeElement.querySelector('app-pokemon-canvas')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeNull();
  });

  it('keeps the spinner when loading fails', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    api.today.mockRejectedValue(new Error('down'));
    const fixture = await setup();
    expect(fixture.nativeElement.querySelector('.loading-spinner')).not.toBeNull();
  });

  it('applies an attempt result to the game state', async () => {
    api.attempt.mockResolvedValue({
      correct: false,
      guess: 'rattata',
      feedback: 'MMMMMMM',
      hints: null,
      revealedName: null,
      revealedNameEs: null,
      nameScore: null,
    });
    const fixture = await setup();
    await fixture.componentInstance.onAttempt('rattata');
    expect(TestBed.inject(GameStateService).attempts().length).toBe(1);
  });

  it('ignores attempts while another one is processing', async () => {
    let resolve!: (v: unknown) => void;
    api.attempt.mockReturnValue(new Promise((r) => (resolve = r)));
    const fixture = await setup();
    const first = fixture.componentInstance.onAttempt('a');
    await fixture.componentInstance.onAttempt('b');
    expect(api.attempt).toHaveBeenCalledTimes(1);
    resolve({ correct: false, guess: 'a', feedback: 'M', hints: null, revealedName: null, revealedNameEs: null, nameScore: null });
    await first;
  });

  it('reveals the name after surrendering, in the selected language', async () => {
    api.surrender.mockResolvedValue({ revealedName: 'Pikachu', revealedNameEs: 'Pikachú' });
    const fixture = await setup();
    await fixture.componentInstance.surrender();
    fixture.detectChanges();
    expect(TestBed.inject(GameStateService).nameRevealed()).toBe(true);
    expect(fixture.nativeElement.querySelector('.alert-success')).not.toBeNull();
  });
});
