import { Injectable, computed, signal } from '@angular/core';

import {
  AttemptResult,
  GameState,
  NameAttemptView,
  SurrenderResult,
} from '../api/types';

/**
 * Per-session game state. Held in memory only — a page refresh creates a
 * fresh service instance, which is exactly what we want: each session
 * starts a new game.
 */
@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly _bootstrap = signal<GameState | null>(null);
  private readonly _attempts = signal<NameAttemptView[]>([]);
  private readonly _solvingGuess = signal<NameAttemptView | null>(null);
  private readonly _nameSolved = signal(false);
  private readonly _nameSurrendered = signal(false);
  private readonly _nameScore = signal<number | null>(null);
  private readonly _revealedName = signal<string | null>(null);
  private readonly _revealedNameEs = signal<string | null>(null);
  private readonly _hints = signal<string | null>(null);

  readonly bootstrap = this._bootstrap.asReadonly();
  readonly attempts = this._attempts.asReadonly();
  readonly nameSolved = this._nameSolved.asReadonly();
  readonly nameSurrendered = this._nameSurrendered.asReadonly();
  readonly nameScore = this._nameScore.asReadonly();
  readonly revealedName = this._revealedName.asReadonly();
  readonly revealedNameEs = this._revealedNameEs.asReadonly();
  readonly hints = this._hints.asReadonly();

  /**
   * Number of attempts the player has used. Counts failed attempts plus the
   * solving one if the name was guessed; surrender does not count as an
   * attempt.
   */
  readonly attemptsUsed = computed(
    () => this._attempts().length + (this._nameSolved() ? 1 : 0),
  );
  readonly attemptsLeft = computed(() =>
    Math.max(0, (this._bootstrap()?.maxAttempts ?? 0) - this.attemptsUsed()),
  );
  readonly blurLevel = computed(() => Math.min(this.attemptsUsed(), 4));
  readonly nameRevealed = computed(
    () =>
      this._nameSolved() ||
      this._nameSurrendered() ||
      this._revealedName() !== null,
  );

  initialize(state: GameState): void {
    this._bootstrap.set(state);
    this._attempts.set([]);
    this._solvingGuess.set(null);
    this._nameSolved.set(false);
    this._nameSurrendered.set(false);
    this._nameScore.set(null);
    this._revealedName.set(null);
    this._revealedNameEs.set(null);
    this._hints.set(null);
  }

  applyAttempt(r: AttemptResult): void {
    if (r.correct) {
      this._solvingGuess.set({ guess: r.guess, feedback: r.feedback });
      this._nameSolved.set(true);
      this._nameScore.set(r.nameScore);
      this._revealedName.set(r.revealedName);
      this._revealedNameEs.set(r.revealedNameEs);
      this._hints.set(null);
      return;
    }

    if (r.guess) {
      this._attempts.update((prev) => [
        ...prev,
        { guess: r.guess, feedback: r.feedback },
      ]);
    }

    if (r.revealedName) {
      // Game over after the last attempt.
      this._nameScore.set(r.nameScore ?? 0);
      this._revealedName.set(r.revealedName);
      this._revealedNameEs.set(r.revealedNameEs);
      this._hints.set(null);
    } else {
      this._hints.set(r.hints);
    }
  }

  applySurrender(r: SurrenderResult): void {
    this._nameSurrendered.set(true);
    this._nameScore.set(0);
    this._revealedName.set(r.revealedName);
    this._revealedNameEs.set(r.revealedNameEs);
    this._hints.set(null);
  }
}
