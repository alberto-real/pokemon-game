import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { GameApi } from '../../../api/game-api.service';
import { TtsPlayerService } from '../../../voice/services/tts-player.service';
import { SpeechRecognizerService } from '../../../voice/services/speech-recognizer.service';
import { signal } from '@angular/core';
import { ConfettiService } from '../../services/confetti.service';
import { QuizRunnerComponent } from './quiz-runner';

describe('QuizRunnerComponent', () => {
  let fixture: ComponentFixture<QuizRunnerComponent>;
  const api = { quiz: vi.fn(), answer: vi.fn() };
  const tts = { speak: vi.fn(), stop: vi.fn() };
  const confetti = { burst: vi.fn(), celebrate: vi.fn() };
  const recognizer = { available: signal(false), listening: signal(false) };

  const quiz = {
    status: 'READY',
    questions: [
      { id: 1, position: 1, text: 'Q1?', options: ['a', 'b', 'c', 'd'] },
      { id: 2, position: 2, text: 'Q2?', options: ['a', 'b', 'c', 'd'] },
    ],
  };

  const el = () => fixture.nativeElement as HTMLElement;
  const optionButtons = () => el().querySelectorAll<HTMLButtonElement>('.grid button');

  beforeEach(async () => {
    vi.useFakeTimers();
    api.quiz.mockReset().mockResolvedValue(quiz);
    api.answer.mockReset();
    tts.speak.mockReset().mockResolvedValue(undefined);
    confetti.burst.mockReset();
    confetti.celebrate.mockReset();
    await TestBed.configureTestingModule({
      imports: [QuizRunnerComponent],
      providers: [
        provideTranslateService(),
        { provide: GameApi, useValue: api },
        { provide: TtsPlayerService, useValue: tts },
        { provide: SpeechRecognizerService, useValue: recognizer },
        { provide: ConfettiService, useValue: confetti },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(QuizRunnerComponent);
    fixture.componentRef.setInput('nameScore', 3);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('loads the quiz and shows the first question with four options', () => {
    expect(el().textContent).toContain('Q1?');
    expect(optionButtons().length).toBe(4);
  });

  it('reads the question aloud when it is shown', () => {
    expect(tts.speak).toHaveBeenCalledWith(expect.stringContaining('A: a'), 'es-ES');
  });

  it('celebrates a correct answer and advances to the next question', async () => {
    api.answer.mockResolvedValue({ correct: true, selectedIndex: 0, correctIndex: 0 });
    optionButtons()[0].click();
    await vi.advanceTimersByTimeAsync(0);
    expect(api.answer).toHaveBeenCalledWith(1, 'A');
    expect(confetti.burst).toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1500);
    fixture.detectChanges();
    expect(el().textContent).toContain('Q2?');
  });

  it('emits name + quiz score after the last question', async () => {
    const completed: number[] = [];
    fixture.componentInstance.completed.subscribe((s) => completed.push(s));
    api.answer.mockResolvedValue({ correct: true, selectedIndex: 0, correctIndex: 0 });

    optionButtons()[0].click();
    await vi.advanceTimersByTimeAsync(1500);
    fixture.detectChanges();
    optionButtons()[0].click();
    await vi.advanceTimersByTimeAsync(1500);

    expect(completed).toEqual([5]);
    expect(confetti.celebrate).toHaveBeenCalled();
  });

  it('does not add to the score on a wrong answer', async () => {
    const completed: number[] = [];
    fixture.componentInstance.completed.subscribe((s) => completed.push(s));
    api.answer.mockResolvedValue({ correct: false, selectedIndex: 0, correctIndex: 1 });

    optionButtons()[0].click();
    await vi.advanceTimersByTimeAsync(3500);
    fixture.detectChanges();
    optionButtons()[0].click();
    await vi.advanceTimersByTimeAsync(3500);

    expect(completed).toEqual([3]);
    expect(confetti.burst).not.toHaveBeenCalled();
    expect(confetti.celebrate).not.toHaveBeenCalled();
  });
});
