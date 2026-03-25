CREATE TYPE brief_status AS ENUM ('DRAFT', 'READY', 'INCOMPLETE');

CREATE TABLE briefs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workpack_id UUID NOT NULL REFERENCES workpacks(id) ON DELETE CASCADE,
    title VARCHAR(500),
    main_idea TEXT,
    objective TEXT,
    actors JSONB DEFAULT '[]',
    scope_includes JSONB DEFAULT '[]',
    scope_excludes JSONB DEFAULT '[]',
    constraints JSONB DEFAULT '[]',
    success_criteria JSONB DEFAULT '[]',
    domain_facts JSONB DEFAULT '[]',
    readiness_signals JSONB DEFAULT '{}',
    status brief_status DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
