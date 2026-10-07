import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../../../api/game-api.service';

@Component({
  selector: 'app-admin-page',
  imports: [FormsModule, TranslateModule],
  templateUrl: './admin-page.html',
})
export class AdminPageComponent {
  private readonly api = inject(GameApi);

  date = new Date().toISOString().slice(0, 10);
  protected readonly result = signal<string | null>(null);

  async generate(): Promise<void> {
    await this.run(() => this.api.adminGenerate(this.date));
  }
  async regenerate(): Promise<void> {
    await this.run(() => this.api.adminRegenerate(this.date));
  }
  async status(): Promise<void> {
    await this.run(() => this.api.adminStatus(this.date));
  }

  private async run(fn: () => Promise<unknown>): Promise<void> {
    try {
      const r = await fn();
      this.result.set(JSON.stringify(r, null, 2));
    } catch (e) {
      this.result.set(String(e));
    }
  }
}
