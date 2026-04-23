import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../api/game-api.service';
import { GameState } from '../api/types';
import { PokemonCanvasComponent } from './pokemon-canvas.component';
import { VoiceRecorderComponent } from '../voice/voice-recorder.component';
import { QuizRunnerComponent } from './quiz-runner.component';

@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    PokemonCanvasComponent,
    VoiceRecorderComponent,
    QuizRunnerComponent,
  ],
  template: `
    @if (state(); as s) {
      <div class="container mx-auto max-w-2xl p-4 space-y-4">
        <app-pokemon-canvas
          [imageUrl]="s.imageUrl"
          [blurLevel]="s.blurLevel"
          [revealed]="s.nameSolved || s.nameSurrendered"
        />

        @if (!s.nameSolved && !s.nameSurrendered) {
          <div class="flex justify-between items-center">
            <div class="badge badge-neutral badge-lg">
              {{ 'game.attempts_remaining' | translate: { n: s.attemptsLeft } }}
            </div>
            <button class="btn btn-outline btn-sm" (click)="surrender()">
              {{ 'game.surrender' | translate }}
            </button>
          </div>
          <app-voice-recorder (transcript)="onAttempt($event)" />
        } @else {
          <div class="alert alert-success">
            {{ 'game.revealed' | translate: { name: s.revealedNameEs } }}
          </div>

          @if (s.quizReady) {
            <app-quiz-runner (completed)="onQuizComplete($event)" />
          }
        }

        @if (lastFeedback(); as f) {
          <div class="toast toast-top toast-center">
            <div
              class="alert"
              [class.alert-success]="f.ok"
              [class.alert-error]="!f.ok"
            >
              {{ (f.ok ? 'game.correct' : 'game.incorrect') | translate }}
            </div>
          </div>
        }
      </div>
    } @else {
      <div class="flex justify-center items-center min-h-[50vh]">
        <span class="loading loading-spinner loading-lg"></span>
      </div>
    }
  `,
})
export class GamePageComponent implements OnInit {
  private readonly api = inject(GameApi);

  protected readonly state = signal<GameState | null>(null);
  protected readonly lastFeedback = signal<{ ok: boolean } | null>(null);
  protected readonly finalScore = signal<number | null>(null);

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    try {
      this.state.set(await this.api.today());
    } catch (err) {
      console.error('Failed to load today state', err);
    }
  }

  async onAttempt(transcript: string): Promise<void> {
    try {
      const r = await this.api.attempt(transcript);
      this.state.set(r.state);
      this.lastFeedback.set({ ok: r.correct });
      setTimeout(() => this.lastFeedback.set(null), 2000);
    } catch (err) {
      console.error(err);
    }
  }

  async surrender(): Promise<void> {
    try {
      this.state.set(await this.api.surrender());
    } catch (err) {
      console.error(err);
    }
  }

  onQuizComplete(totalScore: number): void {
    this.finalScore.set(totalScore);
  }
}
