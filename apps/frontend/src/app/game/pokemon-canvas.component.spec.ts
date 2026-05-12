import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PokemonCanvasComponent } from './pokemon-canvas.component';

describe('PokemonCanvasComponent', () => {
  let fixture: ComponentFixture<PokemonCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PokemonCanvasComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PokemonCanvasComponent);
  });

  it('should apply brightness(0) filter for level 0', () => {
    fixture.componentRef.setInput('imageUrl', 'url');
    fixture.componentRef.setInput('blurLevel', 0);
    fixture.componentRef.setInput('revealed', false);
    fixture.detectChanges();
    const img = fixture.nativeElement.querySelector('img');
    expect(img.style.filter).toBe('brightness(0)');
  });

  it('should apply blur(12px) filter for level 1', () => {
    fixture.componentRef.setInput('imageUrl', 'url');
    fixture.componentRef.setInput('blurLevel', 1);
    fixture.componentRef.setInput('revealed', false);
    fixture.detectChanges();
    const img = fixture.nativeElement.querySelector('img');
    expect(img.style.filter).toBe('blur(12px)');
  });

  it('should apply none filter when revealed', () => {
    fixture.componentRef.setInput('imageUrl', 'url');
    fixture.componentRef.setInput('blurLevel', 0);
    fixture.componentRef.setInput('revealed', true);
    fixture.detectChanges();
    const img = fixture.nativeElement.querySelector('img');
    expect(img.style.filter).toBe('none');
  });
});
