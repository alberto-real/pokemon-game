import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { SpeechRecognizerService } from './speech-recognizer.service';
import { TtsPlayerService } from './tts-player.service';

@Component({
  selector: 'app-voice-recorder',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  template: `
    <div class="flex flex-col items-stretch gap-2 w-full">
      @if (recognizer.available()) {
        <button
          class="btn btn-primary btn-lg gap-2 w-full"
          [class.btn-error]="recognizer.listening()"
          [disabled]="busy() || disabled()"
          (click)="toggle()"
        >
          @if (recognizer.listening()) {
            {{ 'game.stop' | translate }}
          } @else {
            {{ 'game.record' | translate }}
          }
        </button>
      }

      <form class="join w-full" (submit)="submit($event)">
        <input
          type="text"
          class="input input-bordered join-item flex-1"
          [placeholder]="'game.type_placeholder' | translate"
          [disabled]="recognizer.listening() || disabled()"
          [(ngModel)]="text"
          name="text"
          autocomplete="off"
        />
        <button
          type="submit"
          class="btn btn-primary join-item"
          [disabled]="!text().trim() || recognizer.listening() || disabled()"
        >
          {{ 'game.send' | translate }}
        </button>
      </form>

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
  private readonly tts = inject(TtsPlayerService);
  readonly disabled = input(false);
  readonly transcript = output<string>();

  protected readonly busy = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly text = signal('');

  async toggle(): Promise<void> {
    if (this.recognizer.listening()) {
      this.recognizer.stop();
      return;
    }
    this.tts.stop();
    this.busy.set(true);
    this.errorKey.set(null);
    try {
      const text = await this.recognizer.start('es-ES');
      if (text.trim()) {
        this.text.set(text);
      }
    } catch (err) {
      console.warn('Speech recognition failed', err);
      this.errorKey.set('game.voice_not_detected');
    } finally {
      this.busy.set(false);
    }
  }

  submit(ev: Event): void {
    ev.preventDefault();
    const value = this.text().trim();
    if (!value) return;
    this.transcript.emit(value);
    this.text.set('');
  }
}
