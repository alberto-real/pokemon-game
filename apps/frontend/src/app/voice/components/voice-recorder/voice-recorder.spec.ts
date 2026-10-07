import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { SpeechRecognizerService } from '../../services/speech-recognizer.service';
import { TtsPlayerService } from '../../services/tts-player.service';
import { VoiceRecorderComponent } from './voice-recorder';

describe('VoiceRecorderComponent', () => {
  let fixture: ComponentFixture<VoiceRecorderComponent>;
  const recognizer = {
    available: signal(true),
    listening: signal(false),
    start: vi.fn(),
    stop: vi.fn(),
  };
  const tts = { stop: vi.fn() };

  const el = () => fixture.nativeElement as HTMLElement;
  const input = () => el().querySelector('input') as HTMLInputElement;
  const submit = () => el().querySelector('button[type=submit]') as HTMLButtonElement;

  async function type(value: string) {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    recognizer.available.set(true);
    recognizer.listening.set(false);
    recognizer.start.mockReset();
    recognizer.stop.mockReset();
    tts.stop.mockReset();
    await TestBed.configureTestingModule({
      imports: [VoiceRecorderComponent],
      providers: [
        provideTranslateService(),
        { provide: SpeechRecognizerService, useValue: recognizer },
        { provide: TtsPlayerService, useValue: tts },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VoiceRecorderComponent);
    fixture.detectChanges();
  });

  it('disables submit while the text is empty', () => {
    expect(submit().disabled).toBe(true);
  });

  it('emits the trimmed text on submit and clears the input', async () => {
    const emitted: string[] = [];
    fixture.componentInstance.transcript.subscribe((t) => emitted.push(t));
    await type('  pikachu ');
    el().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(emitted).toEqual(['pikachu']);
    expect(input().value).toBe('');
  });

  it('hides the mic button when speech recognition is unavailable', () => {
    recognizer.available.set(false);
    fixture.detectChanges();
    expect(el().querySelector('button[type=button]')).toBeNull();
  });

  it('fills the input with the recognised speech', async () => {
    recognizer.start.mockResolvedValue('charmander');
    (el().querySelector('button[type=button]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(tts.stop).toHaveBeenCalled();
    expect(input().value).toBe('charmander');
  });

  it('shows a warning when recognition fails', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    recognizer.start.mockRejectedValue(new Error('no-speech'));
    (el().querySelector('button[type=button]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el().querySelector('.alert-warning')).not.toBeNull();
  });

  it('stops recognition when already listening', () => {
    recognizer.listening.set(true);
    fixture.detectChanges();
    (el().querySelector('button[type=button]') as HTMLButtonElement).click();
    expect(recognizer.stop).toHaveBeenCalled();
    expect(recognizer.start).not.toHaveBeenCalled();
  });
});
