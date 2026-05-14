-- Per-attempt state is now held in memory per session. The shared, per-day
-- Pokemon and quiz questions remain persisted; only the user's progress is
-- ephemeral so each device/session/refresh starts a fresh game.
DROP TABLE IF EXISTS user_daily_attempt;
