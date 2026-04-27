ALTER TABLE user_daily_attempt
  ADD COLUMN name_attempts JSONB NOT NULL DEFAULT '[]'::jsonb;
