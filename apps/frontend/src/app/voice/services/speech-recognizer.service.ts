import { Injectable, signal } from '@angular/core';

interface ISpeechRecognition {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: (e: SpeechRecognitionEvent) => void;
  onend: () => void;
  onerror: (e: unknown) => void;
  start(): void;
  stop(): void;
}

interface SpeechRecognitionEvent {
  results: { [index: number]: { transcript: string }[] };
}

declare global {
  interface Window {
    SpeechRecognition?: new () => ISpeechRecognition;
    webkitSpeechRecognition?: new () => ISpeechRecognition;
  }
}

@Injectable({ providedIn: 'root' })
export class SpeechRecognizerService {
  readonly available = signal<boolean>(this.detectSupport());
  readonly listening = signal<boolean>(false);

  private recog: ISpeechRecognition | null = null;

  private detectSupport(): boolean {
    if (typeof window === 'undefined') return false;
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  }

  start(lang = 'es-ES'): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!this.available()) {
        reject(new Error('SpeechRecognition not supported in this browser'));
        return;
      }
      const Ctor = window.SpeechRecognition || window.webkitSpeechRecognition!;
      const r = new Ctor();
      r.lang = lang;
      r.continuous = false;
      r.interimResults = false;
      let resolved = false;
      r.onresult = (e) => {
        const transcript = e.results?.[0]?.[0]?.transcript ?? '';
        resolved = true;
        resolve(transcript);
      };
      r.onerror = (err) => {
        if (!resolved) reject(err);
      };
      r.onend = () => {
        this.listening.set(false);
        if (!resolved) reject(new Error('no-speech'));
      };
      this.recog = r;
      this.listening.set(true);
      r.start();
    });
  }

  stop(): void {
    this.recog?.stop();
    this.listening.set(false);
  }
}
