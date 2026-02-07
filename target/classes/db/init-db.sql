DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE device (
    id SERIAL PRIMARY KEY,
    mnemonic VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE UNIQUE INDEX idx_device_mnemonic ON device(mnemonic);

CREATE TABLE memory (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX idx_memory_device_name ON memory(device_id, name);

CREATE TABLE memory_value (
    id SERIAL PRIMARY KEY,
    memory_id INTEGER NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE memory_value_current (
    memory_id INTEGER PRIMARY KEY REFERENCES memory(id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memory_device ON memory(device_id);
CREATE INDEX idx_memory_value_memory ON memory_value(memory_id);
CREATE INDEX idx_memory_value_updated_at ON memory_value(updated_at);
CREATE INDEX idx_memory_value_current_updated_at ON memory_value_current(updated_at);
