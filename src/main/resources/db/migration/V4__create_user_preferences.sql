CREATE TABLE IF NOT EXISTS user_preferences (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    company_news        BOOLEAN NOT NULL DEFAULT FALSE,
    account_activity    BOOLEAN NOT NULL DEFAULT TRUE,
    meetups_near_you    BOOLEAN NOT NULL DEFAULT TRUE,
    new_messages        BOOLEAN NOT NULL DEFAULT FALSE,
    rating_reminders    BOOLEAN NOT NULL DEFAULT TRUE,
    item_update_notif   BOOLEAN NOT NULL DEFAULT TRUE,
    item_comment_notif  BOOLEAN NOT NULL DEFAULT TRUE,
    buyer_review_notif  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_tenant ON user_preferences(tenant_id);
