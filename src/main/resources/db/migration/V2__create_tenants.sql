CREATE TABLE IF NOT EXISTS tenants (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    logo_url    VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);

INSERT INTO tenants (id, name, slug, created_at, updated_at)
SELECT DISTINCT
    u.tenant_id,
    COALESCE(NULLIF(u.tenant_name, ''), 'SupportOps Tenant'),
    'tenant-' || REPLACE(u.tenant_id::text, '-', ''),
    now(),
    now()
FROM users u
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_tenant'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id);
    END IF;
END;
$$;
