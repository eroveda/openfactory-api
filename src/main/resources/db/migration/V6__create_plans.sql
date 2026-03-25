CREATE TYPE plan_status AS ENUM ('VALID', 'HAS_WARNINGS', 'INVALID');

CREATE TABLE execution_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    version VARCHAR(50) DEFAULT '1.0',
    status plan_status DEFAULT 'VALID',
    steps JSONB DEFAULT '[]',
    weak_dependencies JSONB DEFAULT '[]',
    findings JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW()
);
