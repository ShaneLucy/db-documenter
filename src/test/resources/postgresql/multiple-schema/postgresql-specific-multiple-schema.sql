-- =============================================================================
-- PostgreSQL Production-Ready Multi-Schema Database
-- =============================================================================
-- This schema demonstrates PostgreSQL-specific features for a SaaS project
-- management platform with enterprise capabilities.
--
-- Schemas:
--   - auth: Authentication, authorization, and session management
--   - core: Main business entities (organizations, projects, tasks)
--   - analytics: Reporting, aggregations, and materialized views
--   - audit: Change tracking and compliance logging
--
-- PostgreSQL-Specific Features Demonstrated:
--   - Custom ENUM types
--   - Custom COMPOSITE types
--   - Array types (text[], integer[], uuid[])
--   - JSONB with GIN indexes
--   - Full-text search with tsvector/tsquery
--   - Table partitioning (range partitioning by date)
--   - Generated columns (stored)
--   - Exclusion constraints (prevent overlapping time ranges)
--   - Partial indexes
--   - Cross-schema foreign keys
--   - Table inheritance
--   - Sequences with custom configurations
--   - Triggers and functions
--   - Materialized views with concurrent refresh
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Extensions
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- For gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "btree_gist";    -- Required for exclusion constraints with multiple types
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- Trigram similarity for fuzzy text search

-- -----------------------------------------------------------------------------
-- Create Schemas
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS audit;

-- =============================================================================
-- CUSTOM TYPES (must be created before tables that use them)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- ENUM Types
-- -----------------------------------------------------------------------------

-- User account status in auth schema
CREATE TYPE auth.account_status AS ENUM (
    'pending_verification',
    'active',
    'suspended',
    'deactivated',
    'locked'
);

-- MFA (Multi-Factor Authentication) method types
CREATE TYPE auth.mfa_method AS ENUM (
    'totp',           -- Time-based One-Time Password (Google Authenticator, etc.)
    'sms',            -- SMS-based verification
    'email',          -- Email-based verification
    'hardware_key',   -- FIDO2/WebAuthn hardware keys
    'backup_codes'    -- One-time backup codes
);

-- Project status in core schema
CREATE TYPE core.project_status AS ENUM (
    'planning',
    'active',
    'on_hold',
    'completed',
    'archived',
    'cancelled'
);

-- Task priority levels
CREATE TYPE core.priority_level AS ENUM (
    'critical',
    'high',
    'medium',
    'low',
    'none'
);

-- Task status with workflow states
CREATE TYPE core.task_status AS ENUM (
    'backlog',
    'todo',
    'in_progress',
    'in_review',
    'blocked',
    'done',
    'cancelled'
);

-- Subscription tiers for SaaS billing
CREATE TYPE core.subscription_tier AS ENUM (
    'free',
    'starter',
    'professional',
    'enterprise',
    'custom'
);

-- Audit action types
CREATE TYPE audit.action_type AS ENUM (
    'create',
    'read',
    'update',
    'delete',
    'login',
    'logout',
    'permission_change',
    'export',
    'bulk_operation'
);

-- -----------------------------------------------------------------------------
-- COMPOSITE Types (PostgreSQL-specific structured types)
-- -----------------------------------------------------------------------------

-- Address composite type - reusable across multiple tables
CREATE TYPE core.address_type AS (
    street_line1    varchar(200),
    street_line2    varchar(200),
    city            varchar(100),
    state_province  varchar(100),
    postal_code     varchar(20),
    country_code    char(2)         -- ISO 3166-1 alpha-2
);

-- Monetary amount with currency (for internationalization)
CREATE TYPE core.money_amount AS (
    amount          numeric(19, 4),
    currency_code   char(3)         -- ISO 4217 currency code
);

-- Time range for scheduling (used with exclusion constraints)
CREATE TYPE core.time_slot AS (
    start_time      timestamptz,
    end_time        timestamptz
);

-- =============================================================================
-- AUTH SCHEMA - Authentication, Authorization, Sessions
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Sequences with custom configuration
-- -----------------------------------------------------------------------------
CREATE SEQUENCE auth.permission_id_seq
    START WITH 1000
    INCREMENT BY 1
    MINVALUE 1000
    MAXVALUE 9999
    NO CYCLE
    CACHE 10;

-- -----------------------------------------------------------------------------
-- Users table - Core identity
-- -----------------------------------------------------------------------------
CREATE TABLE auth.users (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identity
    email               varchar(255) NOT NULL,
    email_verified      boolean NOT NULL DEFAULT false,
    email_verified_at   timestamptz,
    username            varchar(50),

    -- Authentication
    password_hash       varchar(255) NOT NULL,
    password_changed_at timestamptz NOT NULL DEFAULT now(),

    -- Profile (using JSONB for flexible schema)
    profile             jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- PostgreSQL Array type: store user's preferred languages
    preferred_languages text[] NOT NULL DEFAULT ARRAY['en']::text[],

    -- Account status using custom ENUM
    status              auth.account_status NOT NULL DEFAULT 'pending_verification',

    -- Full-text search vector (PostgreSQL tsvector type)
    -- Automatically maintained via trigger for searching users
    search_vector       tsvector,

    -- Audit columns
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    last_login_at       timestamptz,
    login_count         integer NOT NULL DEFAULT 0,
    failed_login_count  integer NOT NULL DEFAULT 0,
    locked_until        timestamptz,

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_username_format CHECK (username IS NULL OR username ~* '^[a-z][a-z0-9_]{2,49}$'),
    CONSTRAINT users_password_not_empty CHECK (char_length(password_hash) >= 60)
);

-- GIN index on JSONB profile for efficient querying
CREATE INDEX idx_users_profile ON auth.users USING GIN (profile jsonb_path_ops);

-- GIN index for full-text search
CREATE INDEX idx_users_search ON auth.users USING GIN (search_vector);

-- Partial index: only index active users for login queries (PostgreSQL-specific)
CREATE INDEX idx_users_active_email ON auth.users (email)
    WHERE status = 'active';

-- Index on array column for language preference queries
CREATE INDEX idx_users_languages ON auth.users USING GIN (preferred_languages);

-- -----------------------------------------------------------------------------
-- MFA Configuration - Multi-factor authentication settings per user
-- -----------------------------------------------------------------------------
CREATE TABLE auth.user_mfa (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- MFA method using custom ENUM
    method          auth.mfa_method NOT NULL,

    -- Encrypted secret (for TOTP) or identifier (for SMS/email)
    secret_encrypted bytea,

    -- Backup codes stored as text array (PostgreSQL-specific)
    backup_codes    text[],

    -- Status
    is_primary      boolean NOT NULL DEFAULT false,
    is_verified     boolean NOT NULL DEFAULT false,
    verified_at     timestamptz,

    -- Metadata as JSONB (device info, setup details, etc.)
    metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Audit
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_used_at    timestamptz,

    -- Only one primary MFA method per user
    CONSTRAINT user_mfa_one_primary UNIQUE (user_id, is_primary)
        -- PostgreSQL partial unique: only enforce when is_primary = true
        -- Note: This requires a partial unique index instead
);

-- Partial unique index: ensure only one primary MFA per user
CREATE UNIQUE INDEX idx_user_mfa_primary ON auth.user_mfa (user_id)
    WHERE is_primary = true;

-- -----------------------------------------------------------------------------
-- Roles - RBAC role definitions
-- -----------------------------------------------------------------------------
CREATE TABLE auth.roles (
    id              integer PRIMARY KEY DEFAULT nextval('auth.permission_id_seq'),

    code            varchar(50) NOT NULL UNIQUE,
    name            varchar(100) NOT NULL,
    description     text,

    -- Hierarchical roles: self-referential FK
    parent_role_id  integer REFERENCES auth.roles(id) ON DELETE SET NULL,

    -- Role level for hierarchy (generated column - PostgreSQL 12+)
    -- This would need a trigger for proper hierarchy calculation

    -- System roles cannot be deleted
    is_system_role  boolean NOT NULL DEFAULT false,

    -- JSONB for role-specific settings
    settings        jsonb NOT NULL DEFAULT '{}'::jsonb,

    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- Permissions - Granular permission definitions
-- -----------------------------------------------------------------------------
CREATE TABLE auth.permissions (
    id              integer PRIMARY KEY DEFAULT nextval('auth.permission_id_seq'),

    -- Permission identifier (e.g., 'projects.create', 'tasks.delete')
    code            varchar(100) NOT NULL UNIQUE,

    -- Human-readable
    name            varchar(200) NOT NULL,
    description     text,

    -- Categorization using array (can belong to multiple categories)
    categories      text[] NOT NULL DEFAULT ARRAY[]::text[],

    -- Resource and action breakdown
    resource        varchar(50) NOT NULL,
    action          varchar(50) NOT NULL,

    created_at      timestamptz NOT NULL DEFAULT now(),

    -- Unique constraint on resource + action combination
    CONSTRAINT permissions_resource_action_unique UNIQUE (resource, action)
);

-- -----------------------------------------------------------------------------
-- Role-Permission mapping (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE auth.role_permissions (
    role_id         integer NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    permission_id   integer NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,

    -- Permission can be conditional (JSONB conditions)
    conditions      jsonb,

    granted_at      timestamptz NOT NULL DEFAULT now(),
    granted_by      uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    PRIMARY KEY (role_id, permission_id)
);

-- -----------------------------------------------------------------------------
-- User-Role mapping (many-to-many with context)
-- -----------------------------------------------------------------------------
CREATE TABLE auth.user_roles (
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id         integer NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,

    -- Role can be scoped to an organization (cross-schema FK)
    -- NULL means global role
    organization_id uuid,  -- FK added after core.organizations is created

    -- Temporal validity (role assignment with expiration)
    valid_from      timestamptz NOT NULL DEFAULT now(),
    valid_until     timestamptz,

    assigned_at     timestamptz NOT NULL DEFAULT now(),
    assigned_by     uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    PRIMARY KEY (user_id, role_id, COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid)),

    -- Check that valid_until is after valid_from
    CONSTRAINT user_roles_valid_range CHECK (valid_until IS NULL OR valid_until > valid_from)
);

-- Partial index for currently valid roles
CREATE INDEX idx_user_roles_current ON auth.user_roles (user_id, role_id)
    WHERE valid_until IS NULL OR valid_until > now();

-- -----------------------------------------------------------------------------
-- Sessions - User session tracking
-- -----------------------------------------------------------------------------
CREATE TABLE auth.sessions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Session token (hashed)
    token_hash      varchar(64) NOT NULL UNIQUE,

    -- Session metadata as JSONB
    -- Contains: ip_address, user_agent, device_info, geo_location
    metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Session validity
    created_at      timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz NOT NULL,
    last_activity   timestamptz NOT NULL DEFAULT now(),

    -- Revocation
    revoked_at      timestamptz,
    revoked_reason  varchar(100),

    CONSTRAINT sessions_not_expired CHECK (expires_at > created_at)
);

-- Index for session lookup and cleanup
CREATE INDEX idx_sessions_user ON auth.sessions (user_id, expires_at);
CREATE INDEX idx_sessions_cleanup ON auth.sessions (expires_at) WHERE revoked_at IS NULL;

-- =============================================================================
-- CORE SCHEMA - Main Business Entities
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Organizations - Multi-tenant support
-- -----------------------------------------------------------------------------
CREATE TABLE core.organizations (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identity
    name                varchar(200) NOT NULL,
    slug                varchar(100) NOT NULL UNIQUE,

    -- Using composite type for billing address (PostgreSQL-specific)
    billing_address     core.address_type,

    -- Subscription using custom ENUM
    subscription_tier   core.subscription_tier NOT NULL DEFAULT 'free',
    subscription_data   jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Organization settings as JSONB
    settings            jsonb NOT NULL DEFAULT '{
        "features": {},
        "limits": {
            "max_projects": 5,
            "max_members": 10,
            "storage_gb": 1
        },
        "branding": {}
    }'::jsonb,

    -- Domain allowlist for SSO (array type)
    allowed_domains     text[] NOT NULL DEFAULT ARRAY[]::text[],

    -- Full-text search
    search_vector       tsvector,

    -- Audit columns
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    created_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    -- Soft delete
    deleted_at          timestamptz,

    CONSTRAINT organizations_slug_format CHECK (slug ~* '^[a-z][a-z0-9-]{2,99}$')
);

-- Add the cross-schema FK from user_roles to organizations
ALTER TABLE auth.user_roles
    ADD CONSTRAINT user_roles_organization_fk
    FOREIGN KEY (organization_id) REFERENCES core.organizations(id) ON DELETE CASCADE;

-- GIN index for settings queries
CREATE INDEX idx_organizations_settings ON core.organizations USING GIN (settings jsonb_path_ops);

-- Full-text search index
CREATE INDEX idx_organizations_search ON core.organizations USING GIN (search_vector);

-- Partial index for active organizations
CREATE INDEX idx_organizations_active ON core.organizations (slug) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Organization Members - User membership in organizations
-- -----------------------------------------------------------------------------
CREATE TABLE core.organization_members (
    organization_id     uuid NOT NULL REFERENCES core.organizations(id) ON DELETE CASCADE,
    user_id             uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Member-specific display name override
    display_name        varchar(100),

    -- Member role within organization
    role                varchar(50) NOT NULL DEFAULT 'member',

    -- Permissions override as JSONB
    permissions_override jsonb,

    -- Invitation tracking
    invited_at          timestamptz NOT NULL DEFAULT now(),
    invited_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    accepted_at         timestamptz,

    -- Status
    is_active           boolean NOT NULL DEFAULT true,

    PRIMARY KEY (organization_id, user_id)
);

-- Index for user's organizations lookup
CREATE INDEX idx_org_members_user ON core.organization_members (user_id) WHERE is_active = true;

-- -----------------------------------------------------------------------------
-- Projects - Core project entity
-- -----------------------------------------------------------------------------
CREATE TABLE core.projects (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     uuid NOT NULL REFERENCES core.organizations(id) ON DELETE CASCADE,

    -- Project identity
    key                 varchar(10) NOT NULL,  -- Short key like "PROJ", "DEV"
    name                varchar(200) NOT NULL,
    description         text,

    -- Status using custom ENUM
    status              core.project_status NOT NULL DEFAULT 'planning',

    -- Project lead (FK to auth.users - cross-schema)
    lead_id             uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    -- Project dates
    start_date          date,
    target_end_date     date,
    actual_end_date     date,

    -- Tags as array (PostgreSQL-specific)
    tags                text[] NOT NULL DEFAULT ARRAY[]::text[],

    -- Project color for UI (hex format)
    color               char(7) DEFAULT '#3B82F6',

    -- Custom fields as JSONB (flexible schema per organization)
    custom_fields       jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Settings
    settings            jsonb NOT NULL DEFAULT '{
        "workflow": "kanban",
        "visibility": "members",
        "notifications": true
    }'::jsonb,

    -- Full-text search vector
    search_vector       tsvector,

    -- Audit columns
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    created_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    -- Soft delete
    archived_at         timestamptz,

    -- Unique key within organization
    CONSTRAINT projects_org_key_unique UNIQUE (organization_id, key),

    -- Color format validation
    CONSTRAINT projects_color_format CHECK (color IS NULL OR color ~* '^#[0-9A-F]{6}$'),

    -- Date logic
    CONSTRAINT projects_dates_valid CHECK (
        (start_date IS NULL OR target_end_date IS NULL OR start_date <= target_end_date)
        AND (actual_end_date IS NULL OR start_date IS NULL OR actual_end_date >= start_date)
    )
);

-- GIN index for tags array
CREATE INDEX idx_projects_tags ON core.projects USING GIN (tags);

-- GIN index for custom fields
CREATE INDEX idx_projects_custom_fields ON core.projects USING GIN (custom_fields jsonb_path_ops);

-- Full-text search
CREATE INDEX idx_projects_search ON core.projects USING GIN (search_vector);

-- Composite index for organization's active projects
CREATE INDEX idx_projects_org_active ON core.projects (organization_id, status)
    WHERE archived_at IS NULL;

-- -----------------------------------------------------------------------------
-- Task Sequences - Per-project task numbering
-- -----------------------------------------------------------------------------
-- Each project gets its own sequence for task numbers
-- This is handled via a function and trigger

-- -----------------------------------------------------------------------------
-- Tasks - Main work items
-- -----------------------------------------------------------------------------
CREATE TABLE core.tasks (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          uuid NOT NULL REFERENCES core.projects(id) ON DELETE CASCADE,

    -- Task number within project (populated by trigger)
    task_number         integer NOT NULL,

    -- Generated column: Full task key combining project key and number
    -- PostgreSQL 12+ GENERATED ALWAYS AS (stored)
    -- Note: This would need the project key, so we use a trigger instead

    -- Task content
    title               varchar(500) NOT NULL,
    description         text,

    -- Status and priority using custom ENUMs
    status              core.task_status NOT NULL DEFAULT 'backlog',
    priority            core.priority_level NOT NULL DEFAULT 'medium',

    -- Assignment (can be assigned to multiple users via array)
    assignee_ids        uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],

    -- Reporter
    reporter_id         uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    -- Parent task for subtasks (self-referential)
    parent_task_id      uuid REFERENCES core.tasks(id) ON DELETE CASCADE,

    -- Task hierarchy path (for efficient tree queries - PostgreSQL ltree alternative using array)
    path                uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],
    depth               integer NOT NULL DEFAULT 0,

    -- Dates
    due_date            timestamptz,
    started_at          timestamptz,
    completed_at        timestamptz,

    -- Time tracking (in minutes)
    estimated_minutes   integer,
    logged_minutes      integer NOT NULL DEFAULT 0,

    -- Generated column: remaining time (PostgreSQL 12+ STORED generated column)
    remaining_minutes   integer GENERATED ALWAYS AS (
        CASE
            WHEN estimated_minutes IS NULL THEN NULL
            WHEN estimated_minutes - logged_minutes < 0 THEN 0
            ELSE estimated_minutes - logged_minutes
        END
    ) STORED,

    -- Story points for agile estimation
    story_points        smallint,

    -- Labels/tags as array
    labels              text[] NOT NULL DEFAULT ARRAY[]::text[],

    -- Custom fields
    custom_fields       jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Full-text search combining title and description
    search_vector       tsvector,

    -- Audit
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    created_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    -- Unique task number within project
    CONSTRAINT tasks_project_number_unique UNIQUE (project_id, task_number),

    -- Validate time estimates
    CONSTRAINT tasks_estimated_positive CHECK (estimated_minutes IS NULL OR estimated_minutes >= 0),
    CONSTRAINT tasks_logged_positive CHECK (logged_minutes >= 0),
    CONSTRAINT tasks_story_points_range CHECK (story_points IS NULL OR (story_points >= 0 AND story_points <= 100)),

    -- Completed tasks must have completed_at
    CONSTRAINT tasks_completion_consistency CHECK (
        (status != 'done' AND status != 'cancelled')
        OR completed_at IS NOT NULL
    )
);

-- GIN index for assignees array
CREATE INDEX idx_tasks_assignees ON core.tasks USING GIN (assignee_ids);

-- GIN index for labels
CREATE INDEX idx_tasks_labels ON core.tasks USING GIN (labels);

-- Full-text search
CREATE INDEX idx_tasks_search ON core.tasks USING GIN (search_vector);

-- Composite index for project task listing
CREATE INDEX idx_tasks_project_status ON core.tasks (project_id, status, priority);

-- Index for subtask queries
CREATE INDEX idx_tasks_parent ON core.tasks (parent_task_id) WHERE parent_task_id IS NOT NULL;

-- Index for due date queries
CREATE INDEX idx_tasks_due ON core.tasks (due_date) WHERE due_date IS NOT NULL AND status NOT IN ('done', 'cancelled');

-- -----------------------------------------------------------------------------
-- Task Dependencies - DAG of task relationships
-- -----------------------------------------------------------------------------
CREATE TABLE core.task_dependencies (
    predecessor_id      uuid NOT NULL REFERENCES core.tasks(id) ON DELETE CASCADE,
    successor_id        uuid NOT NULL REFERENCES core.tasks(id) ON DELETE CASCADE,

    -- Dependency type
    dependency_type     varchar(20) NOT NULL DEFAULT 'finish_to_start',

    -- Lag time in days (can be negative for lead time)
    lag_days            integer NOT NULL DEFAULT 0,

    created_at          timestamptz NOT NULL DEFAULT now(),
    created_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,

    PRIMARY KEY (predecessor_id, successor_id),

    -- Prevent self-dependency
    CONSTRAINT task_deps_no_self CHECK (predecessor_id != successor_id),

    -- Valid dependency types
    CONSTRAINT task_deps_valid_type CHECK (
        dependency_type IN ('finish_to_start', 'start_to_start', 'finish_to_finish', 'start_to_finish')
    )
);

-- -----------------------------------------------------------------------------
-- Time Entries - Time tracking records
-- -----------------------------------------------------------------------------
CREATE TABLE core.time_entries (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             uuid NOT NULL REFERENCES core.tasks(id) ON DELETE CASCADE,
    user_id             uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Time range
    started_at          timestamptz NOT NULL,
    ended_at            timestamptz,

    -- Calculated duration in minutes (generated column)
    duration_minutes    integer GENERATED ALWAYS AS (
        CASE
            WHEN ended_at IS NULL THEN NULL
            ELSE EXTRACT(EPOCH FROM (ended_at - started_at))::integer / 60
        END
    ) STORED,

    -- Description of work done
    description         text,

    -- Billable tracking
    is_billable         boolean NOT NULL DEFAULT true,
    billing_rate        core.money_amount,  -- Using composite type

    -- Audit
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT time_entries_valid_range CHECK (ended_at IS NULL OR ended_at > started_at),
    CONSTRAINT time_entries_reasonable_duration CHECK (
        ended_at IS NULL OR EXTRACT(EPOCH FROM (ended_at - started_at)) < 86400  -- Max 24 hours
    )
);

-- Index for user's time entries
CREATE INDEX idx_time_entries_user ON core.time_entries (user_id, started_at DESC);

-- Index for task's time entries
CREATE INDEX idx_time_entries_task ON core.time_entries (task_id, started_at DESC);

-- -----------------------------------------------------------------------------
-- Resource Bookings - With EXCLUSION constraint to prevent double-booking
-- -----------------------------------------------------------------------------
CREATE TABLE core.resource_bookings (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- What is being booked (meeting room, equipment, etc.)
    resource_type       varchar(50) NOT NULL,
    resource_id         uuid NOT NULL,

    -- Who booked it
    booked_by           uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    organization_id     uuid NOT NULL REFERENCES core.organizations(id) ON DELETE CASCADE,

    -- Booking time range
    booking_start       timestamptz NOT NULL,
    booking_end         timestamptz NOT NULL,

    -- tstzrange for exclusion constraint (PostgreSQL range type)
    booking_range       tstzrange GENERATED ALWAYS AS (
        tstzrange(booking_start, booking_end, '[)')
    ) STORED,

    title               varchar(200) NOT NULL,
    description         text,

    -- Recurrence pattern as JSONB
    recurrence          jsonb,

    created_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT bookings_valid_range CHECK (booking_end > booking_start),

    -- PostgreSQL EXCLUSION constraint: prevent overlapping bookings for same resource
    -- This is a powerful PostgreSQL-specific feature for temporal data
    CONSTRAINT bookings_no_overlap EXCLUDE USING GIST (
        resource_type WITH =,
        resource_id WITH =,
        booking_range WITH &&
    )
);

-- Index for finding user's bookings
CREATE INDEX idx_bookings_user ON core.resource_bookings (booked_by, booking_start);

-- Index for finding resource availability
CREATE INDEX idx_bookings_resource ON core.resource_bookings (resource_type, resource_id, booking_start);

-- -----------------------------------------------------------------------------
-- Comments - Polymorphic comments on various entities
-- -----------------------------------------------------------------------------
CREATE TABLE core.comments (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Polymorphic reference (entity type + id)
    entity_type         varchar(50) NOT NULL,
    entity_id           uuid NOT NULL,

    -- Comment content
    content             text NOT NULL,
    content_html        text,  -- Rendered HTML (for rich text)

    -- Full-text search
    search_vector       tsvector,

    -- Parent comment for threading
    parent_id           uuid REFERENCES core.comments(id) ON DELETE CASCADE,

    -- Mentions (array of user IDs)
    mentioned_user_ids  uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],

    -- Reactions stored as JSONB (e.g., {"thumbs_up": ["user1", "user2"], "heart": ["user3"]})
    reactions           jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Author
    author_id           uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Edit tracking
    is_edited           boolean NOT NULL DEFAULT false,
    edited_at           timestamptz,

    -- Audit
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    -- Soft delete
    deleted_at          timestamptz,

    CONSTRAINT comments_content_not_empty CHECK (char_length(trim(content)) > 0),
    CONSTRAINT comments_valid_entity_type CHECK (
        entity_type IN ('task', 'project', 'document', 'milestone')
    )
);

-- Composite index for entity comments
CREATE INDEX idx_comments_entity ON core.comments (entity_type, entity_id, created_at);

-- Index for user's comments
CREATE INDEX idx_comments_author ON core.comments (author_id, created_at DESC);

-- Full-text search
CREATE INDEX idx_comments_search ON core.comments USING GIN (search_vector);

-- GIN index for mentioned users
CREATE INDEX idx_comments_mentions ON core.comments USING GIN (mentioned_user_ids);

-- =============================================================================
-- ANALYTICS SCHEMA - Reporting and Aggregations
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Daily Project Stats - Partitioned table for time-series data
-- -----------------------------------------------------------------------------
-- This demonstrates PostgreSQL declarative partitioning (PostgreSQL 10+)

CREATE TABLE analytics.daily_project_stats (
    id                  bigserial,
    stat_date           date NOT NULL,
    project_id          uuid NOT NULL,
    organization_id     uuid NOT NULL,

    -- Task counts by status
    tasks_backlog       integer NOT NULL DEFAULT 0,
    tasks_todo          integer NOT NULL DEFAULT 0,
    tasks_in_progress   integer NOT NULL DEFAULT 0,
    tasks_in_review     integer NOT NULL DEFAULT 0,
    tasks_blocked       integer NOT NULL DEFAULT 0,
    tasks_done          integer NOT NULL DEFAULT 0,
    tasks_cancelled     integer NOT NULL DEFAULT 0,

    -- Derived metrics (could be generated columns)
    tasks_total         integer NOT NULL DEFAULT 0,
    tasks_completed_pct numeric(5, 2),

    -- Velocity metrics
    story_points_total      integer,
    story_points_completed  integer,

    -- Time tracking
    estimated_minutes   bigint,
    logged_minutes      bigint,

    -- Active team members
    active_member_count integer NOT NULL DEFAULT 0,
    active_member_ids   uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],

    -- Computed at
    computed_at         timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (stat_date, project_id)
) PARTITION BY RANGE (stat_date);

-- Create partitions for recent and upcoming months
-- In production, this would be automated via a scheduled job
CREATE TABLE analytics.daily_project_stats_2024_q4
    PARTITION OF analytics.daily_project_stats
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE analytics.daily_project_stats_2025_q1
    PARTITION OF analytics.daily_project_stats
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');

CREATE TABLE analytics.daily_project_stats_2025_q2
    PARTITION OF analytics.daily_project_stats
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');

CREATE TABLE analytics.daily_project_stats_default
    PARTITION OF analytics.daily_project_stats
    DEFAULT;

-- Index on partitioned table (will be created on each partition)
CREATE INDEX idx_daily_stats_project ON analytics.daily_project_stats (project_id, stat_date DESC);
CREATE INDEX idx_daily_stats_org ON analytics.daily_project_stats (organization_id, stat_date DESC);

-- -----------------------------------------------------------------------------
-- User Activity Summary - Aggregated user activity metrics
-- -----------------------------------------------------------------------------
CREATE TABLE analytics.user_activity_summary (
    user_id             uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    period_start        date NOT NULL,
    period_type         varchar(10) NOT NULL,  -- 'daily', 'weekly', 'monthly'

    -- Activity counts
    tasks_created       integer NOT NULL DEFAULT 0,
    tasks_completed     integer NOT NULL DEFAULT 0,
    comments_posted     integer NOT NULL DEFAULT 0,
    time_entries_count  integer NOT NULL DEFAULT 0,

    -- Time logged
    minutes_logged      integer NOT NULL DEFAULT 0,

    -- Projects active in (array)
    active_project_ids  uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],

    -- Detailed breakdown as JSONB
    activity_breakdown  jsonb NOT NULL DEFAULT '{}'::jsonb,

    computed_at         timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, period_start, period_type)
);

-- -----------------------------------------------------------------------------
-- Materialized View - Project Dashboard Summary
-- -----------------------------------------------------------------------------
-- Materialized views are PostgreSQL-specific and allow storing query results
-- for fast access with periodic refresh

CREATE MATERIALIZED VIEW analytics.project_dashboard_summary AS
SELECT
    p.id AS project_id,
    p.organization_id,
    p.name AS project_name,
    p.status AS project_status,
    p.lead_id,

    -- Task statistics
    COUNT(t.id) AS total_tasks,
    COUNT(t.id) FILTER (WHERE t.status = 'done') AS completed_tasks,
    COUNT(t.id) FILTER (WHERE t.status = 'in_progress') AS active_tasks,
    COUNT(t.id) FILTER (WHERE t.status = 'blocked') AS blocked_tasks,
    COUNT(t.id) FILTER (WHERE t.due_date < now() AND t.status NOT IN ('done', 'cancelled')) AS overdue_tasks,

    -- Progress percentage
    CASE
        WHEN COUNT(t.id) = 0 THEN 0
        ELSE ROUND(100.0 * COUNT(t.id) FILTER (WHERE t.status = 'done') / COUNT(t.id), 2)
    END AS completion_percentage,

    -- Story points
    COALESCE(SUM(t.story_points), 0) AS total_story_points,
    COALESCE(SUM(t.story_points) FILTER (WHERE t.status = 'done'), 0) AS completed_story_points,

    -- Time tracking
    COALESCE(SUM(t.estimated_minutes), 0) AS total_estimated_minutes,
    COALESCE(SUM(t.logged_minutes), 0) AS total_logged_minutes,

    -- Team members (distinct assignees)
    array_agg(DISTINCT unnest_assignee) FILTER (WHERE unnest_assignee IS NOT NULL) AS team_member_ids,

    -- Recent activity
    MAX(t.updated_at) AS last_task_update,

    -- Computed timestamp
    now() AS computed_at

FROM core.projects p
LEFT JOIN core.tasks t ON t.project_id = p.id
LEFT JOIN LATERAL unnest(t.assignee_ids) AS unnest_assignee ON true
WHERE p.archived_at IS NULL
GROUP BY p.id, p.organization_id, p.name, p.status, p.lead_id
WITH DATA;

-- Unique index required for CONCURRENTLY refresh
CREATE UNIQUE INDEX idx_project_dashboard_pk ON analytics.project_dashboard_summary (project_id);

-- Additional indexes for queries
CREATE INDEX idx_project_dashboard_org ON analytics.project_dashboard_summary (organization_id);
CREATE INDEX idx_project_dashboard_lead ON analytics.project_dashboard_summary (lead_id);

-- To refresh: REFRESH MATERIALIZED VIEW CONCURRENTLY analytics.project_dashboard_summary;

-- -----------------------------------------------------------------------------
-- Materialized View - Organization Usage Metrics
-- -----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW analytics.organization_usage_metrics AS
SELECT
    o.id AS organization_id,
    o.name AS organization_name,
    o.subscription_tier,

    -- Member counts
    COUNT(DISTINCT om.user_id) FILTER (WHERE om.is_active) AS active_members,
    COUNT(DISTINCT om.user_id) AS total_members,

    -- Project counts
    COUNT(DISTINCT p.id) AS total_projects,
    COUNT(DISTINCT p.id) FILTER (WHERE p.status = 'active') AS active_projects,

    -- Task counts
    COUNT(DISTINCT t.id) AS total_tasks,

    -- Storage and usage (would be calculated from actual storage in production)
    0::bigint AS storage_bytes_used,

    -- Activity in last 30 days
    COUNT(DISTINCT t.id) FILTER (WHERE t.created_at > now() - interval '30 days') AS tasks_created_30d,
    COUNT(DISTINCT t.id) FILTER (WHERE t.completed_at > now() - interval '30 days') AS tasks_completed_30d,

    -- Computed timestamp
    now() AS computed_at

FROM core.organizations o
LEFT JOIN core.organization_members om ON om.organization_id = o.id
LEFT JOIN core.projects p ON p.organization_id = o.id AND p.archived_at IS NULL
LEFT JOIN core.tasks t ON t.project_id = p.id
WHERE o.deleted_at IS NULL
GROUP BY o.id, o.name, o.subscription_tier
WITH DATA;

CREATE UNIQUE INDEX idx_org_usage_pk ON analytics.organization_usage_metrics (organization_id);

-- =============================================================================
-- AUDIT SCHEMA - Change Tracking and Compliance
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Audit Log - Main audit trail (partitioned by month)
-- -----------------------------------------------------------------------------
CREATE TABLE audit.audit_log (
    id                  bigserial,
    event_time          timestamptz NOT NULL DEFAULT now(),

    -- Actor information
    user_id             uuid,  -- Can be NULL for system actions
    session_id          uuid,
    ip_address          inet,  -- PostgreSQL inet type for IP addresses
    user_agent          text,

    -- Organization context
    organization_id     uuid,

    -- Action details using custom ENUM
    action              audit.action_type NOT NULL,

    -- Target entity (polymorphic)
    entity_type         varchar(100) NOT NULL,
    entity_id           text NOT NULL,

    -- Change details
    -- old_values and new_values store the before/after state
    old_values          jsonb,
    new_values          jsonb,

    -- Computed diff (for easier querying)
    changed_fields      text[] NOT NULL DEFAULT ARRAY[]::text[],

    -- Additional context
    context             jsonb NOT NULL DEFAULT '{}'::jsonb,

    -- Request metadata
    request_id          uuid,
    correlation_id      uuid,

    PRIMARY KEY (event_time, id)
) PARTITION BY RANGE (event_time);

-- Create partitions (in production, automate this)
CREATE TABLE audit.audit_log_2024_11
    PARTITION OF audit.audit_log
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE audit.audit_log_2024_12
    PARTITION OF audit.audit_log
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

CREATE TABLE audit.audit_log_2025_01
    PARTITION OF audit.audit_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit.audit_log_default
    PARTITION OF audit.audit_log
    DEFAULT;

-- Indexes on partitioned audit log
CREATE INDEX idx_audit_user ON audit.audit_log (user_id, event_time DESC);
CREATE INDEX idx_audit_entity ON audit.audit_log (entity_type, entity_id, event_time DESC);
CREATE INDEX idx_audit_org ON audit.audit_log (organization_id, event_time DESC);
CREATE INDEX idx_audit_action ON audit.audit_log (action, event_time DESC);

-- GIN index for searching within JSONB
CREATE INDEX idx_audit_context ON audit.audit_log USING GIN (context jsonb_path_ops);
CREATE INDEX idx_audit_changed_fields ON audit.audit_log USING GIN (changed_fields);

-- -----------------------------------------------------------------------------
-- Data Change History - Table inheritance example for type-specific audit
-- -----------------------------------------------------------------------------
-- Base table for all entity changes (using PostgreSQL table inheritance)
CREATE TABLE audit.entity_changes (
    id                  bigserial PRIMARY KEY,
    entity_type         varchar(100) NOT NULL,
    entity_id           uuid NOT NULL,
    version             integer NOT NULL DEFAULT 1,
    changed_at          timestamptz NOT NULL DEFAULT now(),
    changed_by          uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    change_type         varchar(20) NOT NULL,  -- 'create', 'update', 'delete'
    data_snapshot       jsonb NOT NULL
);

-- Child table for task changes (inherits from entity_changes)
CREATE TABLE audit.task_changes (
    project_id          uuid NOT NULL,
    task_number         integer NOT NULL,
    status_before       core.task_status,
    status_after        core.task_status,

    CONSTRAINT task_changes_type CHECK (entity_type = 'task')
) INHERITS (audit.entity_changes);

-- Child table for project changes
CREATE TABLE audit.project_changes (
    organization_id     uuid NOT NULL,
    status_before       core.project_status,
    status_after        core.project_status,

    CONSTRAINT project_changes_type CHECK (entity_type = 'project')
) INHERITS (audit.entity_changes);

-- Indexes for inherited tables
CREATE INDEX idx_task_changes_entity ON audit.task_changes (entity_id, version DESC);
CREATE INDEX idx_task_changes_project ON audit.task_changes (project_id, changed_at DESC);

CREATE INDEX idx_project_changes_entity ON audit.project_changes (entity_id, version DESC);
CREATE INDEX idx_project_changes_org ON audit.project_changes (organization_id, changed_at DESC);

-- -----------------------------------------------------------------------------
-- Access Log - Security-focused access tracking
-- -----------------------------------------------------------------------------
CREATE TABLE audit.access_log (
    id                  bigserial PRIMARY KEY,
    event_time          timestamptz NOT NULL DEFAULT now(),

    -- User information
    user_id             uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    session_id          uuid,

    -- Network information (PostgreSQL inet and cidr types)
    client_ip           inet NOT NULL,
    client_ip_network   cidr,  -- Network the IP belongs to

    -- Request details
    request_method      varchar(10) NOT NULL,
    request_path        text NOT NULL,
    request_query       jsonb,

    -- Response
    response_status     smallint NOT NULL,
    response_time_ms    integer NOT NULL,

    -- Geographic data (if available)
    geo_country         char(2),
    geo_region          varchar(100),
    geo_city            varchar(100),
    geo_coordinates     point,  -- PostgreSQL geometric point type

    -- Risk assessment
    risk_score          smallint,
    risk_factors        text[],

    -- Additional metadata
    metadata            jsonb NOT NULL DEFAULT '{}'::jsonb
);

-- Index for user access patterns
CREATE INDEX idx_access_user ON audit.access_log (user_id, event_time DESC);

-- Index for security analysis
CREATE INDEX idx_access_ip ON audit.access_log (client_ip, event_time DESC);

-- Index for geographic analysis
CREATE INDEX idx_access_geo ON audit.access_log (geo_country, geo_city)
    WHERE geo_country IS NOT NULL;

-- GIN index for risk factors
CREATE INDEX idx_access_risk ON audit.access_log USING GIN (risk_factors);

-- =============================================================================
-- FUNCTIONS AND TRIGGERS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Function: Update updated_at timestamp
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION core.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to relevant tables
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON core.organizations
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON core.projects
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

CREATE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON core.tasks
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

CREATE TRIGGER trg_time_entries_updated_at
    BEFORE UPDATE ON core.time_entries
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

CREATE TRIGGER trg_comments_updated_at
    BEFORE UPDATE ON core.comments
    FOR EACH ROW EXECUTE FUNCTION core.update_updated_at();

-- -----------------------------------------------------------------------------
-- Function: Generate task number within project
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION core.generate_task_number()
RETURNS TRIGGER AS $$
BEGIN
    -- Get the next task number for this project
    SELECT COALESCE(MAX(task_number), 0) + 1
    INTO NEW.task_number
    FROM core.tasks
    WHERE project_id = NEW.project_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_number
    BEFORE INSERT ON core.tasks
    FOR EACH ROW
    WHEN (NEW.task_number IS NULL)
    EXECUTE FUNCTION core.generate_task_number();

-- -----------------------------------------------------------------------------
-- Function: Update task path for hierarchy
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION core.update_task_path()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_task_id IS NULL THEN
        NEW.path = ARRAY[NEW.id];
        NEW.depth = 0;
    ELSE
        SELECT path || NEW.id, depth + 1
        INTO NEW.path, NEW.depth
        FROM core.tasks
        WHERE id = NEW.parent_task_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_path
    BEFORE INSERT OR UPDATE OF parent_task_id ON core.tasks
    FOR EACH ROW EXECUTE FUNCTION core.update_task_path();

-- -----------------------------------------------------------------------------
-- Function: Update full-text search vectors
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION auth.update_user_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.email, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.username, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.profile->>'first_name', '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.profile->>'last_name', '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.profile->>'bio', '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_search_vector
    BEFORE INSERT OR UPDATE OF email, username, profile ON auth.users
    FOR EACH ROW EXECUTE FUNCTION auth.update_user_search_vector();

CREATE OR REPLACE FUNCTION core.update_organization_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.slug, '')), 'A');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_organizations_search_vector
    BEFORE INSERT OR UPDATE OF name, slug ON core.organizations
    FOR EACH ROW EXECUTE FUNCTION core.update_organization_search_vector();

CREATE OR REPLACE FUNCTION core.update_project_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.key, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', array_to_string(NEW.tags, ' ')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_projects_search_vector
    BEFORE INSERT OR UPDATE OF key, name, description, tags ON core.projects
    FOR EACH ROW EXECUTE FUNCTION core.update_project_search_vector();

CREATE OR REPLACE FUNCTION core.update_task_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector =
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', array_to_string(NEW.labels, ' ')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_search_vector
    BEFORE INSERT OR UPDATE OF title, description, labels ON core.tasks
    FOR EACH ROW EXECUTE FUNCTION core.update_task_search_vector();

CREATE OR REPLACE FUNCTION core.update_comment_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector = to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comments_search_vector
    BEFORE INSERT OR UPDATE OF content ON core.comments
    FOR EACH ROW EXECUTE FUNCTION core.update_comment_search_vector();

-- -----------------------------------------------------------------------------
-- Function: Update task logged time when time entry changes
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION core.update_task_logged_time()
RETURNS TRIGGER AS $$
BEGIN
    -- Update the task's logged_minutes based on all time entries
    IF TG_OP = 'DELETE' THEN
        UPDATE core.tasks
        SET logged_minutes = COALESCE((
            SELECT SUM(duration_minutes)
            FROM core.time_entries
            WHERE task_id = OLD.task_id AND ended_at IS NOT NULL
        ), 0)
        WHERE id = OLD.task_id;
        RETURN OLD;
    ELSE
        UPDATE core.tasks
        SET logged_minutes = COALESCE((
            SELECT SUM(duration_minutes)
            FROM core.time_entries
            WHERE task_id = NEW.task_id AND ended_at IS NOT NULL
        ), 0)
        WHERE id = NEW.task_id;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_time_entries_update_task
    AFTER INSERT OR UPDATE OR DELETE ON core.time_entries
    FOR EACH ROW EXECUTE FUNCTION core.update_task_logged_time();

-- -----------------------------------------------------------------------------
-- Function: Audit logging for sensitive tables
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION audit.log_entity_change()
RETURNS TRIGGER AS $$
DECLARE
    v_old_values jsonb;
    v_new_values jsonb;
    v_changed_fields text[];
    v_action audit.action_type;
BEGIN
    -- Determine action type
    IF TG_OP = 'INSERT' THEN
        v_action = 'create';
        v_new_values = to_jsonb(NEW);
        v_changed_fields = ARRAY(SELECT jsonb_object_keys(v_new_values));
    ELSIF TG_OP = 'UPDATE' THEN
        v_action = 'update';
        v_old_values = to_jsonb(OLD);
        v_new_values = to_jsonb(NEW);
        -- Find changed fields
        SELECT array_agg(key)
        INTO v_changed_fields
        FROM jsonb_each(v_new_values) n
        LEFT JOIN jsonb_each(v_old_values) o USING (key)
        WHERE n.value IS DISTINCT FROM o.value;
    ELSIF TG_OP = 'DELETE' THEN
        v_action = 'delete';
        v_old_values = to_jsonb(OLD);
        v_changed_fields = ARRAY(SELECT jsonb_object_keys(v_old_values));
    END IF;

    -- Insert audit record
    INSERT INTO audit.audit_log (
        user_id,
        action,
        entity_type,
        entity_id,
        old_values,
        new_values,
        changed_fields
    ) VALUES (
        current_setting('app.current_user_id', true)::uuid,
        v_action,
        TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
        COALESCE(NEW.id::text, OLD.id::text),
        v_old_values,
        v_new_values,
        COALESCE(v_changed_fields, ARRAY[]::text[])
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply audit triggers to critical tables
CREATE TRIGGER trg_audit_users
    AFTER INSERT OR UPDATE OR DELETE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION audit.log_entity_change();

CREATE TRIGGER trg_audit_organizations
    AFTER INSERT OR UPDATE OR DELETE ON core.organizations
    FOR EACH ROW EXECUTE FUNCTION audit.log_entity_change();

CREATE TRIGGER trg_audit_projects
    AFTER INSERT OR UPDATE OR DELETE ON core.projects
    FOR EACH ROW EXECUTE FUNCTION audit.log_entity_change();

-- =============================================================================
-- VIEWS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- View: Active organization members with user details
-- -----------------------------------------------------------------------------
CREATE VIEW core.active_organization_members_view AS
SELECT
    om.organization_id,
    om.user_id,
    om.role AS member_role,
    COALESCE(om.display_name, u.profile->>'display_name', u.username, u.email) AS display_name,
    u.email,
    u.profile->'avatar_url' AS avatar_url,
    u.status AS account_status,
    om.accepted_at,
    u.last_login_at
FROM core.organization_members om
JOIN auth.users u ON u.id = om.user_id
WHERE om.is_active = true
  AND u.status = 'active';

-- -----------------------------------------------------------------------------
-- View: Project summary with task counts
-- -----------------------------------------------------------------------------
CREATE VIEW core.project_summary_view AS
SELECT
    p.id,
    p.organization_id,
    p.key,
    p.name,
    p.status,
    p.lead_id,
    lead.email AS lead_email,
    COUNT(t.id) AS total_tasks,
    COUNT(t.id) FILTER (WHERE t.status = 'done') AS completed_tasks,
    COUNT(t.id) FILTER (WHERE t.status IN ('in_progress', 'in_review')) AS active_tasks,
    COUNT(t.id) FILTER (WHERE t.due_date < now() AND t.status NOT IN ('done', 'cancelled')) AS overdue_tasks,
    p.start_date,
    p.target_end_date,
    p.created_at,
    p.updated_at
FROM core.projects p
LEFT JOIN core.tasks t ON t.project_id = p.id
LEFT JOIN auth.users lead ON lead.id = p.lead_id
WHERE p.archived_at IS NULL
GROUP BY p.id, lead.email;

-- -----------------------------------------------------------------------------
-- View: User task workload
-- -----------------------------------------------------------------------------
CREATE VIEW core.user_task_workload_view AS
SELECT
    u.id AS user_id,
    u.email,
    u.username,
    COUNT(t.id) AS total_assigned_tasks,
    COUNT(t.id) FILTER (WHERE t.status IN ('todo', 'in_progress', 'in_review')) AS active_tasks,
    COUNT(t.id) FILTER (WHERE t.status = 'blocked') AS blocked_tasks,
    COUNT(t.id) FILTER (WHERE t.due_date < now() AND t.status NOT IN ('done', 'cancelled')) AS overdue_tasks,
    COALESCE(SUM(t.estimated_minutes) FILTER (WHERE t.status NOT IN ('done', 'cancelled')), 0) AS remaining_estimated_minutes,
    COALESCE(SUM(t.story_points) FILTER (WHERE t.status NOT IN ('done', 'cancelled')), 0) AS remaining_story_points,
    array_agg(DISTINCT t.project_id) FILTER (WHERE t.project_id IS NOT NULL) AS project_ids
FROM auth.users u
LEFT JOIN core.tasks t ON u.id = ANY(t.assignee_ids)
WHERE u.status = 'active'
GROUP BY u.id, u.email, u.username;

-- =============================================================================
-- SAMPLE COMMENTS FOR DOCUMENTATION
-- =============================================================================

-- PostgreSQL-Specific Features Used in This Schema:
--
-- 1. ENUM Types:
--    - auth.account_status, auth.mfa_method
--    - core.project_status, core.priority_level, core.task_status, core.subscription_tier
--    - audit.action_type
--    Unlike MySQL ENUMs which are column-specific, PostgreSQL ENUMs are reusable types.
--
-- 2. COMPOSITE Types:
--    - core.address_type, core.money_amount, core.time_slot
--    Allow structured data without separate tables.
--
-- 3. Array Types:
--    - text[] for tags, labels, languages, backup_codes
--    - uuid[] for assignee_ids, active_member_ids
--    With GIN indexes for efficient array operations.
--
-- 4. JSONB:
--    - Flexible schema for profile, settings, metadata, custom_fields
--    - GIN indexes with jsonb_path_ops for efficient querying
--
-- 5. Full-Text Search:
--    - tsvector columns with weighted search vectors
--    - GIN indexes for efficient text search
--    - Trigger-maintained search vectors
--
-- 6. Table Partitioning:
--    - analytics.daily_project_stats partitioned by date range
--    - audit.audit_log partitioned by event_time
--    Declarative partitioning (PostgreSQL 10+)
--
-- 7. Generated Columns (STORED):
--    - tasks.remaining_minutes
--    - time_entries.duration_minutes
--    - resource_bookings.booking_range
--
-- 8. Exclusion Constraints:
--    - resource_bookings.bookings_no_overlap prevents double-booking
--    Uses GiST index with && operator for range overlap
--
-- 9. Partial Indexes:
--    - idx_users_active_email indexes only active users
--    - idx_user_mfa_primary unique only when is_primary = true
--
-- 10. Table Inheritance:
--     - audit.task_changes and audit.project_changes inherit from audit.entity_changes
--
-- 11. PostgreSQL Data Types:
--     - inet/cidr for IP addresses
--     - point for geographic coordinates
--     - tstzrange for time ranges
--     - bytea for binary data
--
-- 12. Cross-Schema Foreign Keys:
--     - auth.user_roles references core.organizations
--     - core.projects references auth.users
--
-- 13. Custom Sequences:
--     - auth.permission_id_seq with START, MIN, MAX, CACHE options
--
-- 14. Materialized Views:
--     - analytics.project_dashboard_summary
--     - analytics.organization_usage_metrics
--     Support REFRESH MATERIALIZED VIEW CONCURRENTLY
--
-- 15. Triggers and Functions:
--     - Automatic updated_at maintenance
--     - Task number generation
--     - Full-text search vector updates
--     - Audit logging
