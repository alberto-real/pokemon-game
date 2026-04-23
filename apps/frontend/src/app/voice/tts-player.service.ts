import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TtsPlayerService {
  readonly speaking = signal<boolean>(false);
  readonly available = signal<boolean>(
    typeof window !== 'undefined' && !!window.speechSynthesis,
  );

  speak(text: string, lang = 'es-ES'): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.available()) {
        reject(new Error('speechSynthesis not supported'));
        return;
      }
      window.speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(text);
      u.lang = lang;
      u.onend = () => {
        this.speaking.set(false);
        resolve();
      };
      u.onerror = (err) => {
        this.speaking.set(false);
        reject(err);
      };
      this.speaking.set(true);
      window.speechSynthesis.speak(u);
    });
  }

  stop(): void {
    if (this.available()) window.speechSynthesis.cancel();
    this.speaking.set(false);
  }
}
