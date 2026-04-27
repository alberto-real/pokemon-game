import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { NameAttemptView } from '../api/types';

interface LetterTile {
  letter: string;
  status: 'H' | 'P' | 'M';
}

interface AttemptRow {
  index: number;
  tiles: LetterTile[];
}

@Component({
  selector: 'app-attempts-history',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div class="card bg-base-200 shadow-sm">
      <div class="card-body p-4">
        <h3 class="card-title text-base">
          {{ 'game.attempts_history' | translate }}
          <span class="text-sm font-normal opacity-70">{{ used() }}/{{ max() }}</span>
        </h3>

        @if (rows().length === 0) {
          <p class="text-sm opacity-60">{{ 'game.no_attempts_yet' | translate }}</p>
        } @else {
          <ul class="space-y-2">
            @for (row of rows(); track row.index) {
              <li class="flex items-center gap-2">
                <span class="text-xs opacity-60 w-4 text-right">{{ row.index }}.</span>
                <div class="flex gap-1 flex-wrap">
                  @for (tile of row.tiles; track $index) {
                    <span
                      class="inline-flex items-center justify-center w-7 h-7 rounded text-sm font-bold uppercase text-white"
                      [class.bg-success]="tile.status === 'H'"
                      [class.bg-warning]="tile.status === 'P'"
                      [class.bg-error]="tile.status === 'M'"
                    >
                      {{ tile.letter }}
                    </span>
                  }
                </div>
              </li>
            }
          </ul>
        }
      </div>
    </div>
  `,
})
export class AttemptsHistoryComponent {
  readonly attempts = input.required<NameAttemptView[]>();
  readonly max = input.required<number>();

  protected readonly used = computed(() => this.attempts().length);

  protected readonly rows = computed<AttemptRow[]>(() =>
    this.attempts().map((a, i) => ({
      index: i + 1,
      tiles: this.toTiles(a),
    })),
  );

  private toTiles(a: NameAttemptView): LetterTile[] {
    const letters = [...a.guess];
    return letters.map((letter, i) => ({
      letter,
      status: this.statusAt(a.feedback, i),
    }));
  }

  private statusAt(feedback: string, i: number): 'H' | 'P' | 'M' {
    const c = feedback.charAt(i);
    return c === 'H' || c === 'P' ? c : 'M';
  }
}
