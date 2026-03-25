CREATE TYPE approval_status AS ENUM ('PENDING', 'APPROVED', 'CHANGES_REQUESTED');

CREATE TABLE handoffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    owner_id UUID REFERENCES users(id),
    intended_executor VARCHAR(255),
    assumptions JSONB DEFAULT '[]',
    approval_status approval_status DEFAULT 'PENDING',
    handoff_notes TEXT,
    review_notes TEXT,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
