import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { SpeechRecognizerService } from '../../services/speech-recognizer.service';
import { TtsPlayerService } from '../../services/tts-player.service';

@Component({
  selector: 'app-voice-recorder',
  imports: [TranslateModule, FormsModule],
  templateUrl: './voice-recorder.html',
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
