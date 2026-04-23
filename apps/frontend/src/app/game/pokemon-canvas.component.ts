import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-pokemon-canvas',
  standalone: true,
  template: `
    <div class="flex items-center justify-center bg-base-200 rounded-box p-6">
      <img
        [src]="imageUrl()"
        alt="Pokemon"
        class="w-64 h-64 object-contain transition-all duration-500"
        [style.filter]="filter()"
      />
    </div>
  `,
})
export class PokemonCanvasComponent {
  readonly imageUrl = input.required<string>();
  readonly blurLevel = input.required<number>();
  readonly revealed = input.required<boolean>();

  protected readonly filter = computed(() => {
    if (this.revealed()) return 'none';
    switch (this.blurLevel()) {
      case 0:
        return 'brightness(0)';
      case 1:
        return 'blur(40px)';
      case 2:
        return 'blur(20px)';
      case 3:
        return 'blur(10px)';
      default:
        return 'none';
    }
  });
}
