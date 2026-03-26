ALTER TABLE workpacks
    ADD COLUMN IF NOT EXISTS execution_plan TEXT,
    ADD COLUMN IF NOT EXISTS step_count     INTEGER;
