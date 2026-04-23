CREATE TABLE daily_pokemon (
  date DATE PRIMARY KEY,
  pokemon_id INT NOT NULL,
  pokemon_name TEXT NOT NULL,
  pokemon_name_es TEXT NOT NULL,
  image_url TEXT NOT NULL,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE daily_quiz (
  date DATE PRIMARY KEY REFERENCES daily_pokemon(date) ON DELETE CASCADE,
  status TEXT NOT NULL,
  status_message TEXT,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ready_at TIMESTAMPTZ
);

CREATE TABLE daily_quiz_question (
  id BIGSERIAL PRIMARY KEY,
  quiz_date DATE NOT NULL REFERENCES daily_quiz(date) ON DELETE CASCADE,
  position INT NOT NULL,
  question_text TEXT NOT NULL,
  options JSONB NOT NULL,
  correct_option_index INT NOT NULL,
  audio_question_path TEXT,
  audio_options_paths JSONB,
  CONSTRAINT uq_quiz_position UNIQUE (quiz_date, position)
);

CREATE TABLE user_daily_attempt (
  user_id TEXT NOT NULL,
  date DATE NOT NULL,
  name_attempts_used INT NOT NULL DEFAULT 0,
  name_solved BOOLEAN NOT NULL DEFAULT FALSE,
  name_surrendered BOOLEAN NOT NULL DEFAULT FALSE,
  name_score INT,
  quiz_answers JSONB,
  quiz_score INT,
  total_score INT,
  completed_at TIMESTAMPTZ,
  PRIMARY KEY (user_id, date)
);

CREATE INDEX idx_daily_quiz_status ON daily_quiz(status);
