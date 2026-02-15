CREATE SEQUENCE IF NOT EXISTS member_id_seq;

CREATE UNLOGGED TABLE IF NOT EXISTS cache_store_unlogged (
    cache_key   VARCHAR(512) PRIMARY KEY,
    value       JSONB NOT NULL,
    expired_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cache_store_expired_at ON cache_store_unlogged (expired_at);
