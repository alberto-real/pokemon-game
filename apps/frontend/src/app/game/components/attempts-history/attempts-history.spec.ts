import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { AttemptsHistoryComponent } from './attempts-history';

describe('AttemptsHistoryComponent', () => {
  let fixture: ComponentFixture<AttemptsHistoryComponent>;

  const render = (inputs: Record<string, unknown>) => {
    for (const [k, v] of Object.entries(inputs)) fixture.componentRef.setInput(k, v);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttemptsHistoryComponent],
      providers: [provideTranslateService()],
    }).compileComponents();
    fixture = TestBed.createComponent(AttemptsHistoryComponent);
  });

  it('shows the empty message when there are no attempts', () => {
    const el = render({ attempts: [], max: 5 });
    expect(el.querySelector('ul')).toBeNull();
    expect(el.textContent).toContain('0/5');
  });

  it('renders one row per attempt with the right tile colours', () => {
    const el = render({ attempts: [{ guess: 'abc', feedback: 'HPM' }], max: 5 });
    const tiles = el.querySelectorAll('li span.inline-flex');
    expect(tiles.length).toBe(3);
    expect(tiles[0].classList).toContain('bg-success');
    expect(tiles[1].classList).toContain('bg-warning');
    expect(tiles[2].classList).toContain('bg-error');
  });

  it('treats missing feedback characters as misses', () => {
    const el = render({ attempts: [{ guess: 'ab', feedback: 'H' }], max: 5 });
    const tiles = el.querySelectorAll('li span.inline-flex');
    expect(tiles[1].classList).toContain('bg-error');
  });

  it('shows placeholder slots for the next row when showNextRow is set', () => {
    const el = render({ attempts: [], max: 5, nameLength: 4, showNextRow: true });
    expect(el.querySelectorAll('li span.border-b-2').length).toBe(4);
  });

  it('shows revealed hint letters in the next row', () => {
    const el = render({ attempts: [], max: 5, hints: 'p_k', showNextRow: true });
    const slots = el.querySelectorAll('li span.border-b-2');
    expect(slots.length).toBe(3);
    expect(slots[0].textContent).toContain('p');
    expect(slots[1].textContent?.trim()).toBe('');
  });
});
