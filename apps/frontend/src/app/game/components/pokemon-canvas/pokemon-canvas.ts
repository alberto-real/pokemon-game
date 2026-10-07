import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-pokemon-canvas',
  templateUrl: './pokemon-canvas.html',
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
        return 'blur(12px)';
      case 2:
        return 'blur(8px)';
      case 3:
        return 'blur(4px)';
      case 4:
        return 'none';
      default:
        return 'none';
    }
  });
}
