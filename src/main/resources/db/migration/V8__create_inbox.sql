CREATE TYPE inbox_type AS ENUM (
    'WORKPACK_SHARED', 'APPROVAL_REQUESTED',
    'APPROVED', 'CHANGES_REQUESTED', 'MENTION'
);

CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workpack_id UUID REFERENCES workpacks(id) ON DELETE CASCADE,
    type inbox_type NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
