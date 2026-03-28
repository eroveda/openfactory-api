ALTER TABLE workpacks ADD COLUMN IF NOT EXISTS pipeline_log jsonb DEFAULT '[]'::jsonb;
