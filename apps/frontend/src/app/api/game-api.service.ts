import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import {
  AttemptResult,
  GameState,
  QuizAnswerResult,
  QuizView,
} from './types';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);

  today(): Promise<GameState> {
    return firstValueFrom(this.http.get<GameState>('/api/game/today'));
  }

  attempt(transcript: string): Promise<AttemptResult> {
    return firstValueFrom(
      this.http.post<AttemptResult>('/api/game/today/attempt', { transcript }),
    );
  }

  surrender(): Promise<GameState> {
    return firstValueFrom(
      this.http.post<GameState>('/api/game/today/surrender', {}),
    );
  }

  quiz(): Promise<QuizView> {
    return firstValueFrom(this.http.get<QuizView>('/api/game/today/quiz'));
  }

  answer(questionId: number, transcript: string): Promise<QuizAnswerResult> {
    return firstValueFrom(
      this.http.post<QuizAnswerResult>(
        `/api/game/today/quiz/answer/${questionId}`,
        { transcript },
      ),
    );
  }

  adminGenerate(date?: string): Promise<unknown> {
    const q = date ? `?date=${date}` : '';
    return firstValueFrom(this.http.post(`/api/admin/quiz/generate${q}`, {}));
  }

  adminRegenerate(date: string): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`/api/admin/quiz/regenerate?date=${date}`, {}),
    );
  }

  adminStatus(date: string): Promise<unknown> {
    return firstValueFrom(
      this.http.get(`/api/admin/quiz/status?date=${date}`),
    );
  }
}
