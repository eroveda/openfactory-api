CREATE TYPE pin_type AS ENUM (
    'INTENT', 'ACTOR', 'SCOPE_CONSTRAINT', 'OUT_OF_SCOPE',
    'DOMAIN_FACT', 'ATTACHMENT', 'UNKNOWN'
);

CREATE TABLE pins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    type pin_type DEFAULT 'UNKNOWN',
    confidence DECIMAL(4,2),
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
