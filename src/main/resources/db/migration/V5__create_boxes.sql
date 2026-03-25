CREATE TYPE box_status AS ENUM ('DRAFT', 'READY', 'ACCEPTED');

CREATE TABLE boxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    node_id VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    purpose TEXT,
    scope JSONB DEFAULT '{}',
    input_context TEXT,
    instructions JSONB DEFAULT '[]',
    constraints JSONB DEFAULT '[]',
    dependencies JSONB DEFAULT '[]',
    expected_output TEXT,
    acceptance_criteria JSONB DEFAULT '[]',
    handoff TEXT,
    execution_hints JSONB DEFAULT '{}',
    status box_status DEFAULT 'DRAFT',
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
