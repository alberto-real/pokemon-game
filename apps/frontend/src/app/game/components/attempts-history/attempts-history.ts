import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { NameAttemptView } from '../../../api/types';

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
  imports: [CommonModule, TranslateModule],
  templateUrl: './attempts-history.html',
})
export class AttemptsHistoryComponent {
  readonly attempts = input.required<NameAttemptView[]>();
  readonly max = input.required<number>();
  readonly nameLength = input<number>(0);
  readonly hints = input<string | null>(null);
  readonly showNextRow = input<boolean>(false);

  protected readonly used = computed(() => this.attempts().length);

  protected readonly rows = computed<AttemptRow[]>(() =>
    this.attempts().map((a, i) => ({
      index: i + 1,
      tiles: this.toTiles(a),
    })),
  );

  protected readonly nextRowChars = computed<string[]>(() => {
    const h = this.hints();
    if (h) return [...h];
    return Array(this.nameLength()).fill('_');
  });

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
