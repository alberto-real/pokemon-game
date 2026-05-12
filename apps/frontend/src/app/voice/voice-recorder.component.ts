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
      <form class="join w-full" (submit)="submit($event)">
        @if (recognizer.available()) {
          <button
            type="button"
            class="btn join-item"
            [class.btn-error]="recognizer.listening()"
            [disabled]="busy() || disabled()"
            (click)="toggle()"
            [title]="(recognizer.listening() ? 'game.stop' : 'game.record') | translate"
          >
            @if (recognizer.listening()) {
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/></svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/></svg>
            }
          </button>
        }
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
