import { Injectable } from '@angular/core';
import confetti from 'canvas-confetti';

@Injectable({ providedIn: 'root' })
export class ConfettiService {
  /** Quick burst for small wins (correct quiz answer). */
  burst(): void {
    confetti({
      particleCount: 80,
      spread: 65,
      origin: { y: 0.7 },
    });
  }

  /** Bigger celebration for big wins (perfect quiz, game solved fast). */
  celebrate(): void {
    confetti({
      particleCount: 200,
      spread: 90,
      origin: { y: 0.6 },
      startVelocity: 45,
    });
  }
}
