import { TestBed } from '@angular/core/testing';

import { TtsPlayerService } from './tts-player.service';

describe('TtsPlayerService', () => {
  let utterance: { onend?: () => void; onerror?: (e: unknown) => void; lang?: string };
  const synth = { cancel: vi.fn(), speak: vi.fn() };

  beforeEach(() => {
    synth.cancel.mockReset();
    synth.speak.mockReset();
    vi.stubGlobal('speechSynthesis', synth);
    vi.stubGlobal(
      'SpeechSynthesisUtterance',
      class {
        constructor(public text: string) {
          utterance = this as never;
        }
      },
    );
  });

  afterEach(() => vi.unstubAllGlobals());

  it('speaks and resolves when the utterance ends', async () => {
    const service = TestBed.inject(TtsPlayerService);
    const p = service.speak('hola', 'es-ES');
    expect(service.speaking()).toBe(true);
    expect(utterance.lang).toBe('es-ES');
    utterance.onend!();
    await p;
    expect(service.speaking()).toBe(false);
  });

  it('resolves silently when the utterance is interrupted', async () => {
    const service = TestBed.inject(TtsPlayerService);
    const p = service.speak('hola');
    utterance.onerror!({ error: 'interrupted' });
    await expect(p).resolves.toBeUndefined();
  });

  it('rejects on real errors', async () => {
    const service = TestBed.inject(TtsPlayerService);
    const p = service.speak('hola');
    utterance.onerror!({ error: 'synthesis-failed' });
    await expect(p).rejects.toBeTruthy();
  });

  it('stop cancels the synthesis', () => {
    const service = TestBed.inject(TtsPlayerService);
    service.stop();
    expect(synth.cancel).toHaveBeenCalled();
    expect(service.speaking()).toBe(false);
  });
});
