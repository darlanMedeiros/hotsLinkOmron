CREATE TABLE IF NOT EXISTS dm_values (
    address SMALLINT PRIMARY KEY CHECK (address >= 0 AND address <= 1000),
    value INTEGER NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dm_values_updated_at ON dm_values (updated_at);
