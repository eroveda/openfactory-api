-- Hibernate @Enumerated(EnumType.STRING) expects varchar(255), not PostgreSQL native ENUMs.
-- Convert all ENUM columns to varchar(255) and fix pins.confidence type.

-- workpacks
ALTER TABLE workpacks
    ALTER COLUMN stage              TYPE varchar(255) USING stage::text,
    ALTER COLUMN processing_status  TYPE varchar(255) USING processing_status::text;

-- pins
ALTER TABLE pins
    ALTER COLUMN type               TYPE varchar(255) USING type::text,
    ALTER COLUMN confidence         TYPE float8       USING confidence::float8;

-- briefs
ALTER TABLE briefs
    ALTER COLUMN status             TYPE varchar(255) USING status::text;

-- boxes
ALTER TABLE boxes
    ALTER COLUMN status             TYPE varchar(255) USING status::text;

-- execution_plans
ALTER TABLE execution_plans
    ALTER COLUMN status             TYPE varchar(255) USING status::text;

-- handoffs
ALTER TABLE handoffs
    ALTER COLUMN approval_status    TYPE varchar(255) USING approval_status::text;

-- inbox
ALTER TABLE inbox
    ALTER COLUMN type               TYPE varchar(255) USING type::text;

-- workpack_members
ALTER TABLE workpack_members
    ALTER COLUMN role               TYPE varchar(255) USING role::text;

-- Drop old ENUM types (no longer needed)
DROP TYPE IF EXISTS workpack_stage;
DROP TYPE IF EXISTS processing_status;
DROP TYPE IF EXISTS pin_type;
DROP TYPE IF EXISTS brief_status;
DROP TYPE IF EXISTS box_status;
DROP TYPE IF EXISTS plan_status;
DROP TYPE IF EXISTS approval_status;
DROP TYPE IF EXISTS inbox_type;
DROP TYPE IF EXISTS member_role;
