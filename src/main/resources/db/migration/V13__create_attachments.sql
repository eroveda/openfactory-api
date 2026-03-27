CREATE TABLE attachments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    file_name   TEXT NOT NULL,
    file_type   VARCHAR(50) NOT NULL,   -- 'text', 'image', 'pdf', 'audio'
    storage_url TEXT NOT NULL,
    content_text TEXT,                  -- extracted text for text files; NULL for images/audio
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX attachments_workpack_idx ON attachments(workpack_id);
