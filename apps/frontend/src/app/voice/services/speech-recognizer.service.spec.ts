import { TestBed } from '@angular/core/testing';

import { SpeechRecognizerService } from './speech-recognizer.service';

describe('SpeechRecognizerService', () => {
  let instance: any;

  class FakeRecognition {
    constructor() {
      instance = this;
    }
    start = vi.fn();
    stop = vi.fn();
  }

  afterEach(() => {
    delete (window as any).SpeechRecognition;
    delete (window as any).webkitSpeechRecognition;
  });

  it('is unavailable without browser support and rejects start()', async () => {
    const service = TestBed.inject(SpeechRecognizerService);
    expect(service.available()).toBe(false);
    await expect(service.start()).rejects.toThrow();
  });

  describe('with browser support', () => {
    let service: SpeechRecognizerService;
    beforeEach(() => {
      (window as any).SpeechRecognition = FakeRecognition;
      service = TestBed.inject(SpeechRecognizerService);
    });

    it('resolves with the transcript', async () => {
      const p = service.start('es-ES');
      expect(service.listening()).toBe(true);
      expect(instance.lang).toBe('es-ES');
      instance.onresult({ results: { 0: [{ transcript: 'pikachu' }] } });
      instance.onend();
      await expect(p).resolves.toBe('pikachu');
      expect(service.listening()).toBe(false);
    });

    it('rejects with no-speech when it ends without a result', async () => {
      const p = service.start();
      instance.onend();
      await expect(p).rejects.toThrow('no-speech');
    });

    it('stop() stops the recognition', () => {
      void service.start().catch(() => {});
      service.stop();
      expect(instance.stop).toHaveBeenCalled();
      expect(service.listening()).toBe(false);
    });
  });
});
