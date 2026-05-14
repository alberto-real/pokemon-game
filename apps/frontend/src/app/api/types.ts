export interface NameAttemptView {
  guess: string;
  feedback: string;
}

/** Immutable per-day info served on bootstrap. */
export interface GameState {
  imageUrl: string;
  nameLength: number;
  quizReady: boolean;
  quizStatus: string;
  maxAttempts: number;
}

/** Result of a single name guess. */
export interface AttemptResult {
  correct: boolean;
  guess: string;
  feedback: string;
  hints: string | null;
  revealedName: string | null;
  revealedNameEs: string | null;
  nameScore: number | null;
}

export interface SurrenderResult {
  revealedName: string;
  revealedNameEs: string;
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
}
