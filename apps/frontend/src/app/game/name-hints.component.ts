import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-name-hints',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (showLength()) {
      <div class="flex justify-center gap-1 mb-4">
        @for (char of displayChars(); track $index) {
          <div
            class="w-8 h-10 border-b-2 flex items-center justify-center text-xl font-bold uppercase"
            [class.border-base-content]="char === '_'"
            [class.border-transparent]="char !== '_'"
          >
            @if (char !== '_') {
              <span class="text-base-content opacity-40">{{ char }}</span>
            }
          </div>
        }
      </div>
    }
  `,
})
export class NameHintsComponent {
  readonly length = input.required<number>();
  readonly hints = input<string | null>(null);
  readonly showLength = input<boolean>(false);

  protected readonly displayChars = computed(() => {
    const h = this.hints();
    if (h) return [...h];
    return Array(this.length()).fill('_');
  });
}
