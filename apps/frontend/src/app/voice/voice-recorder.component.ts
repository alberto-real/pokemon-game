import { Component, inject, output, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { SpeechRecognizerService } from './speech-recognizer.service';

@Component({
  selector: 'app-voice-recorder',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <div class="flex flex-col items-center gap-2 w-full">
      @if (recognizer.available()) {
        <button
          class="btn btn-primary btn-lg gap-2 w-full"
          [class.btn-error]="recognizer.listening()"
          [disabled]="busy()"
          (click)="toggle()"
        >
          @if (recognizer.listening()) {
            {{ 'game.stop' | translate }}
          } @else {
            {{ 'game.record' | translate }}
          }
        </button>
      } @else {
        <input
          type="text"
          class="input input-bordered w-full"
          [placeholder]="'game.record' | translate"
          (keyup.enter)="submitText($event)"
        />
      }
      @if (errorKey(); as key) {
        <div class="alert alert-warning text-xs">
          {{ key | translate }}
        </div>
      }
    </div>
  `,
})
export class VoiceRecorderComponent {
  protected readonly recognizer = inject(SpeechRecognizerService);
  readonly transcript = output<string>();

  protected readonly busy = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  async toggle(): Promise<void> {
    if (this.recognizer.listening()) {
      this.recognizer.stop();
      return;
    }
    this.busy.set(true);
    this.errorKey.set(null);
    try {
      const text = await this.recognizer.start('es-ES');
      if (text.trim()) this.transcript.emit(text);
    } catch (err) {
      console.warn('Speech recognition failed', err);
      this.errorKey.set('game.voice_not_detected');
    } finally {
      this.busy.set(false);
    }
  }

  submitText(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    if (input.value.trim()) {
      this.transcript.emit(input.value.trim());
      input.value = '';
    }
  }
}
