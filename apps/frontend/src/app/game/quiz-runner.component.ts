import { Component, inject, signal, output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../api/game-api.service';
import { QuestionView, QuizAnswerResult } from '../api/types';
import { TtsPlayerService } from '../voice/tts-player.service';
import { VoiceRecorderComponent } from '../voice/voice-recorder.component';
import { ConfettiService } from './confetti.service';

@Component({
  selector: 'app-quiz-runner',
  standalone: true,
  imports: [CommonModule, TranslateModule, VoiceRecorderComponent],
  template: `
    @if (question(); as q) {
      <div class="card bg-base-200 p-4 space-y-3">
        <div class="text-sm opacity-70">
          {{ 'quiz.question_of' | translate: { n: q.position, total: total() } }}
        </div>
        <div class="text-xl font-semibold">{{ q.text }}</div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
          @for (opt of q.options; track $index) {
            <div class="alert gap-2">
              <span class="kbd kbd-sm">{{ letter($index) }}</span>
              <span>{{ opt }}</span>
            </div>
          }
        </div>
        <div class="flex gap-2 items-center">
          <button class="btn btn-sm btn-outline" (click)="playQuestion()">
            {{ 'quiz.play' | translate }}
          </button>
          <app-voice-recorder class="flex-1" (transcript)="onAnswer($event)" />
        </div>
        @if (lastResult(); as r) {
          <div
            class="alert"
            [class.alert-success]="r.correct"
            [class.alert-error]="!r.correct"
          >
            @if (r.correct) {
              {{ 'game.correct' | translate }}
            } @else {
              {{
                'quiz.wrong_answer'
                  | translate: { answer: q.options[r.correctIndex] }
              }}
            }
          </div>
        }
      </div>
    } @else if (finalScore() !== null) {
      <div class="alert alert-info text-lg">
        {{ 'quiz.final_score' | translate: { score: finalScore() } }}
      </div>
    } @else {
      <div class="flex justify-center">
        <span class="loading loading-dots"></span>
      </div>
    }
  `,
})
export class QuizRunnerComponent implements OnInit {
  private readonly api = inject(GameApi);
  private readonly tts = inject(TtsPlayerService);
  private readonly confetti = inject(ConfettiService);

  readonly completed = output<number>();

  protected readonly questions = signal<QuestionView[]>([]);
  protected readonly index = signal(0);
  protected readonly lastResult = signal<QuizAnswerResult | null>(null);
  protected readonly finalScore = signal<number | null>(null);

  protected readonly question = signal<QuestionView | null>(null);
  protected readonly total = signal(0);

  async ngOnInit(): Promise<void> {
    try {
      const quiz = await this.api.quiz();
      this.questions.set(quiz.questions);
      this.total.set(quiz.questions.length);
      this.showCurrent();
    } catch (err) {
      console.error('Failed to fetch quiz', err);
    }
  }

  private showCurrent(): void {
    const list = this.questions();
    const i = this.index();
    if (i < list.length) {
      this.question.set(list[i]);
      this.playQuestion();
    } else {
      this.question.set(null);
    }
  }

  async playQuestion(): Promise<void> {
    const q = this.question();
    if (!q) return;
    try {
      const text =
        q.text +
        '. ' +
        q.options.map((o, i) => `${this.letter(i)}: ${o}`).join('. ');
      await this.tts.speak(text, 'es-ES');
    } catch (err) {
      console.error('TTS failed', err);
    }
  }

  async onAnswer(transcript: string): Promise<void> {
    const q = this.question();
    if (!q) return;
    try {
      const r = await this.api.answer(q.id, transcript);
      this.lastResult.set(r);
      if (r.correct) this.confetti.burst();
      // Show the feedback longer when wrong so the user can read the
      // correct-answer hint.
      const delay = r.correct ? 1500 : 3500;
      if (r.quizComplete) {
        this.finalScore.set(r.totalScore ?? 0);
        this.question.set(null);
        if (r.quizScore === (this.total() ?? 0) && (this.total() ?? 0) > 0) {
          this.confetti.celebrate();
        }
        setTimeout(() => this.completed.emit(r.totalScore ?? 0), delay);
      } else {
        setTimeout(() => {
          this.lastResult.set(null);
          this.index.update((n) => n + 1);
          this.showCurrent();
        }, delay);
      }
    } catch (err) {
      console.error(err);
    }
  }

  protected letter(i: number): string {
    return ['A', 'B', 'C', 'D'][i] ?? '?';
  }
}
