import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../../../api/game-api.service';
import { PokemonCanvasComponent } from '../pokemon-canvas/pokemon-canvas';
import { VoiceRecorderComponent } from '../../../voice/components/voice-recorder/voice-recorder';
import { QuizRunnerComponent } from '../quiz-runner/quiz-runner';
import { AttemptsHistoryComponent } from '../attempts-history/attempts-history';
import { GameStateService } from '../../services/game-state.service';
import { LanguageService } from '../../../i18n/services/language.service';

@Component({
  selector: 'app-game-page',
  templateUrl: './game-page.html',
  imports: [
    CommonModule,
    TranslateModule,
    PokemonCanvasComponent,
    VoiceRecorderComponent,
    QuizRunnerComponent,
    AttemptsHistoryComponent,
  ],
})
export class GamePageComponent implements OnInit {
  private readonly api = inject(GameApi);
  protected readonly state = inject(GameStateService);
  private readonly lang = inject(LanguageService);

  protected readonly lastFeedback = signal<{ ok: boolean } | null>(null);
  protected readonly processing = signal(false);
  protected readonly finalScore = signal<number | null>(null);

  protected readonly revealedName = computed(() =>
    this.lang.current() === 'es' ? this.state.revealedNameEs() : this.state.revealedName(),
  );

  async ngOnInit(): Promise<void> {
    try {
      this.state.initialize(await this.api.today());
    } catch (err) {
      console.error('Failed to load today state', err);
    }
  }

  async onAttempt(transcript: string): Promise<void> {
    if (this.processing()) return;
    this.processing.set(true);
    try {
      const r = await this.api.attempt(transcript, this.state.attempts());
      this.state.applyAttempt(r);
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
      this.state.applySurrender(await this.api.surrender());
    } catch (err) {
      console.error(err);
    }
  }

  onQuizComplete(totalScore: number): void {
    this.finalScore.set(totalScore);
  }
}
