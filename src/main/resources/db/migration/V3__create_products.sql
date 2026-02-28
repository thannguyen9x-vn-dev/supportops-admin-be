CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    created_by  UUID REFERENCES users(id),
    name        VARCHAR(255) NOT NULL,
    subtitle    VARCHAR(255),
    category    VARCHAR(100) NOT NULL,
    brand       VARCHAR(100) NOT NULL,
    price       NUMERIC(12, 2) NOT NULL,
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_products_tenant ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_tenant_name ON products(tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_products_tenant_category ON products(tenant_id, category);

CREATE TABLE IF NOT EXISTS product_images (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         VARCHAR(1000) NOT NULL,
    sort_order  INT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_sort ON product_images(product_id, sort_order);
