import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { GameApi } from '../../../api/game-api.service';
import { AdminPageComponent } from './admin-page';

describe('AdminPageComponent', () => {
  const api = {
    adminGenerate: vi.fn(),
    adminRegenerate: vi.fn(),
    adminStatus: vi.fn(),
  };

  function setup() {
    TestBed.configureTestingModule({
      imports: [AdminPageComponent],
      providers: [provideTranslateService(), { provide: GameApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(AdminPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => Object.values(api).forEach((f) => f.mockReset()));

  it('calls generate with the selected date and prints the result', async () => {
    api.adminGenerate.mockResolvedValue({ ok: true });
    const fixture = setup();
    const date = fixture.componentInstance.date;
    await fixture.componentInstance.generate();
    fixture.detectChanges();
    expect(api.adminGenerate).toHaveBeenCalledWith(date);
    expect(fixture.nativeElement.querySelector('pre').textContent).toContain('"ok": true');
  });

  it('calls regenerate and status', async () => {
    api.adminRegenerate.mockResolvedValue({});
    api.adminStatus.mockResolvedValue({});
    const fixture = setup();
    await fixture.componentInstance.regenerate();
    await fixture.componentInstance.status();
    expect(api.adminRegenerate).toHaveBeenCalled();
    expect(api.adminStatus).toHaveBeenCalled();
  });

  it('shows the error when the call fails', async () => {
    api.adminStatus.mockRejectedValue('boom');
    const fixture = setup();
    await fixture.componentInstance.status();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('pre').textContent).toBe('boom');
  });
});
