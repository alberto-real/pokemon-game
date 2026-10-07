import { TestBed } from '@angular/core/testing';

import { AttemptResult, GameState } from '../../api/types';
import { GameStateService } from './game-state.service';

describe('GameStateService', () => {
  let service: GameStateService;
  const bootstrap: GameState = {
    imageUrl: 'x',
    nameLength: 5,
    quizReady: true,
    quizStatus: 'READY',
    maxAttempts: 3,
  };
  const wrong = (over: Partial<AttemptResult> = {}): AttemptResult => ({
    correct: false,
    guess: 'abcde',
    feedback: 'MMMMM',
    hints: null,
    revealedName: null,
    revealedNameEs: null,
    nameScore: null,
    ...over,
  });

  beforeEach(() => {
    service = TestBed.inject(GameStateService);
    service.initialize(bootstrap);
  });

  it('starts with no attempts and full attempts left', () => {
    expect(service.attempts()).toEqual([]);
    expect(service.attemptsLeft()).toBe(3);
    expect(service.nameRevealed()).toBe(false);
  });

  it('records wrong attempts, stores hints and increases blur level', () => {
    service.applyAttempt(wrong({ hints: 'a____' }));
    expect(service.attempts()).toEqual([{ guess: 'abcde', feedback: 'MMMMM' }]);
    expect(service.hints()).toBe('a____');
    expect(service.attemptsLeft()).toBe(2);
    expect(service.blurLevel()).toBe(1);
  });

  it('caps the blur level at 4', () => {
    for (let i = 0; i < 6; i++) service.applyAttempt(wrong());
    expect(service.blurLevel()).toBe(4);
    expect(service.attemptsLeft()).toBe(0);
  });

  it('does not record empty guesses', () => {
    service.applyAttempt(wrong({ guess: '' }));
    expect(service.attempts()).toEqual([]);
  });

  it('marks the name as solved on a correct attempt and counts it as used', () => {
    service.applyAttempt(
      wrong({ correct: true, guess: 'pikac', nameScore: 4, revealedName: 'Pikachu', revealedNameEs: 'Pikachú' }),
    );
    expect(service.nameSolved()).toBe(true);
    expect(service.nameRevealed()).toBe(true);
    expect(service.nameScore()).toBe(4);
    expect(service.attempts()).toEqual([]);
    expect(service.attemptsUsed()).toBe(1);
  });

  it('reveals the name with score 0 when attempts run out', () => {
    service.applyAttempt(wrong({ revealedName: 'Pikachu', revealedNameEs: 'Pikachú', nameScore: null }));
    expect(service.nameRevealed()).toBe(true);
    expect(service.nameScore()).toBe(0);
    expect(service.hints()).toBeNull();
  });

  it('surrender reveals the name, scores 0 and does not use an attempt', () => {
    service.applySurrender({ revealedName: 'Pikachu', revealedNameEs: 'Pikachú' });
    expect(service.nameSurrendered()).toBe(true);
    expect(service.nameScore()).toBe(0);
    expect(service.revealedNameEs()).toBe('Pikachú');
    expect(service.attemptsUsed()).toBe(0);
  });

  it('initialize resets previous progress', () => {
    service.applyAttempt(wrong());
    service.initialize(bootstrap);
    expect(service.attempts()).toEqual([]);
    expect(service.nameRevealed()).toBe(false);
  });
});
