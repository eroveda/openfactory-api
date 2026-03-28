ALTER TABLE workpacks ADD COLUMN IF NOT EXISTS chat_history jsonb DEFAULT '[]'::jsonb;
