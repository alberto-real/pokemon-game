import { Component, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { JsonPipe, NgOptimizedImage } from '@angular/common';

interface Birthday {
  name: string;
  day: number;
  month: number;
}

interface BirthdaysData {
  birthdays: Birthday[];
}

const BIRTHDAY_GIFS = [
  '/assets/hb_1.gif',
  '/assets/hb_2.gif',
  '/assets/hb_3.gif',
  '/assets/hb_4.gif',
] as const;

@Component({
  selector: 'app-birthday-panel',
  imports: [TranslateModule, NgOptimizedImage, JsonPipe],
  templateUrl: './birthday-panel.html',
  styleUrl: './birthday-panel.css',
})
export class BirthdayPanelComponent {
  private http = inject(HttpClient);

  readonly birthdays = signal<Birthday[]>([]);

  readonly randomGif = signal<string>(
    BIRTHDAY_GIFS[Math.floor(Math.random() * BIRTHDAY_GIFS.length)],
  );

  readonly todayBirthdays = computed(() => {
    const today = new Date();
    const currentDay = today.getDate();
    const currentMonth = today.getMonth() + 1;

    return this.birthdays().filter((b) => b.day === currentDay && b.month === currentMonth);
  });

  readonly hasBirthdaysToday = computed(() => this.todayBirthdays().length > 0);

  ngOnInit(): void {
    this.http.get<BirthdaysData>('/assets/birthdays.json').subscribe({
      next: (data) => this.birthdays.set(data.birthdays),
      error: () => this.birthdays.set([]),
    });
  }
}
