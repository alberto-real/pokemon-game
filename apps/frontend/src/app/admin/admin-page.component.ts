import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { GameApi } from '../api/game-api.service';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  template: `
    <div class="container mx-auto max-w-xl p-4 space-y-4">
      <h2 class="text-2xl font-bold">{{ 'admin.title' | translate }}</h2>
      <div class="card bg-base-200 p-4 space-y-3">
        <div class="flex gap-2">
          <input
            type="date"
            [(ngModel)]="date"
            class="input input-bordered flex-1"
          />
        </div>
        <div class="flex gap-2 flex-wrap">
          <button class="btn btn-primary" (click)="generate()">
            {{ 'admin.generate' | translate }}
          </button>
          <button class="btn btn-warning" (click)="regenerate()">
            {{ 'admin.regenerate' | translate }}
          </button>
          <button class="btn btn-outline" (click)="status()">
            {{ 'admin.status' | translate }}
          </button>
        </div>
        @if (result(); as r) {
          <pre class="bg-base-300 rounded p-2 text-xs overflow-x-auto">{{ r }}</pre>
        }
      </div>
    </div>
  `,
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
