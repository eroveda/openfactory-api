CREATE TYPE workpack_stage AS ENUM ('RAW', 'DEFINE', 'SHAPE', 'BOX');

CREATE TABLE workpacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    stage workpack_stage DEFAULT 'RAW',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE workpack_members (
    workpack_id UUID REFERENCES workpacks(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'editor',
    joined_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (workpack_id, user_id)
);
