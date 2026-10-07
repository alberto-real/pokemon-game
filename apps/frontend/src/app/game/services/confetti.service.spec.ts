import { TestBed } from '@angular/core/testing';

import { ConfettiService } from './confetti.service';

const confettiMock = vi.hoisted(() => vi.fn());
vi.mock('canvas-confetti', () => ({ default: confettiMock }));

describe('ConfettiService', () => {
  beforeEach(() => confettiMock.mockClear());

  it('burst fires a small confetti shot', () => {
    TestBed.inject(ConfettiService).burst();
    expect(confettiMock).toHaveBeenCalledWith(expect.objectContaining({ particleCount: 80 }));
  });

  it('celebrate fires a bigger shot', () => {
    TestBed.inject(ConfettiService).celebrate();
    expect(confettiMock).toHaveBeenCalledWith(expect.objectContaining({ particleCount: 200 }));
  });
});
