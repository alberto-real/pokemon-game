import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../api/game-api.service';
import { GameState } from '../api/types';
import { PokemonCanvasComponent } from './pokemon-canvas.component';
import { VoiceRecorderComponent } from '../voice/voice-recorder.component';
import { QuizRunnerComponent } from './quiz-runner.component';
import { AttemptsHistoryComponent } from './attempts-history.component';

const MAX_NAME_ATTEMPTS = 5;

@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    PokemonCanvasComponent,
    VoiceRecorderComponent,
    QuizRunnerComponent,
    AttemptsHistoryComponent,
  ],
  template: `
    @if (state(); as s) {
      <div class="container mx-auto max-w-4xl p-4 space-y-4">
        <div class="grid gap-4 md:grid-cols-2">
          <app-pokemon-canvas
            [imageUrl]="s.imageUrl"
            [blurLevel]="s.blurLevel"
            [revealed]="s.nameSolved || s.nameSurrendered"
          />
          <div class="flex flex-col gap-4 h-full">
            <app-attempts-history
              [attempts]="s.nameAttempts"
              [max]="maxAttempts"
            />
            @if (s.nameSolved || s.nameSurrendered) {
              <div class="alert alert-success mt-auto">
                {{ 'game.revealed' | translate: { name: s.revealedNameEs } }}
              </div>
            }
          </div>
        </div>

        @if (!s.nameSolved && !s.nameSurrendered) {
          <div class="flex justify-between items-center">
            <div class="badge badge-neutral badge-lg">
              {{ 'game.attempts_remaining' | translate: { n: s.attemptsLeft } }}
            </div>
            <button class="btn btn-outline btn-sm" (click)="surrender()">
              {{ 'game.surrender' | translate }}
            </button>
          </div>
          <app-voice-recorder
            [disabled]="processing()"
            (transcript)="onAttempt($event)"
          />
        } @else if (s.quizReady) {
          <app-quiz-runner (completed)="onQuizComplete($event)" />
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

  protected readonly maxAttempts = MAX_NAME_ATTEMPTS;
  protected readonly state = signal<GameState | null>(null);
  protected readonly lastFeedback = signal<{ ok: boolean } | null>(null);
  protected readonly processing = signal(false);
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
    if (this.processing()) return;
    this.processing.set(true);
    try {
      const r = await this.api.attempt(transcript);
      this.state.set(r.state);
      this.lastFeedback.set({ ok: r.correct });
      setTimeout(() => this.lastFeedback.set(null), 2000);
    } catch (err) {
      console.error(err);
    } finally {
      this.processing.set(false);
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
