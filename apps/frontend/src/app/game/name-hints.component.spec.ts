import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NameHintsComponent } from './name-hints.component';
import { Component, signal } from '@angular/core';

describe('NameHintsComponent', () => {
  let component: NameHintsComponent;
  let fixture: ComponentFixture<NameHintsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NameHintsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(NameHintsComponent);
    component = fixture.componentInstance;
  });

  it('should not show anything if showLength is false', () => {
    fixture.componentRef.setInput('length', 5);
    fixture.componentRef.setInput('showLength', false);
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('div');
    expect(el).toBeNull();
  });

  it('should show dashes when showLength is true and no hints', () => {
    fixture.componentRef.setInput('length', 5);
    fixture.componentRef.setInput('showLength', true);
    fixture.detectChanges();
    const dots = fixture.nativeElement.querySelectorAll('.border-base-content');
    expect(dots.length).toBe(5);
  });

  it('should show hint letters when provided', () => {
    fixture.componentRef.setInput('length', 5);
    fixture.componentRef.setInput('showLength', true);
    fixture.componentRef.setInput('hints', 'P_K__');
    fixture.detectChanges();
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('P');
    expect(content).toContain('K');
    const dots = fixture.nativeElement.querySelectorAll('.border-base-content');
    expect(dots.length).toBe(3); // 3 underscores remaining
  });
});
