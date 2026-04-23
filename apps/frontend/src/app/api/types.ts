export interface GameState {
  attemptsLeft: number;
  blurLevel: number;
  nameSolved: boolean;
  nameSurrendered: boolean;
  revealedName: string | null;
  revealedNameEs: string | null;
  imageUrl: string;
  nameScore: number | null;
  quizReady: boolean;
  quizStatus: string;
}

export interface AttemptResult {
  correct: boolean;
  state: GameState;
}

export interface QuestionView {
  id: number;
  position: number;
  text: string;
  options: string[];
}

export interface QuizView {
  status: string;
  questions: QuestionView[];
}

export interface QuizAnswerResult {
  correct: boolean;
  selectedIndex: number;
  correctIndex: number;
  quizComplete: boolean;
  quizScore: number | null;
  totalScore: number | null;
}
