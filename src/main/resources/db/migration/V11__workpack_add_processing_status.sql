CREATE TYPE processing_status AS ENUM ('PENDING', 'PROCESSING', 'DONE', 'FAILED');

ALTER TABLE workpacks
    ADD COLUMN IF NOT EXISTS processing_status processing_status DEFAULT 'DONE',
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;
