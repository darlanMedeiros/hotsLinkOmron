CREATE TABLE IF NOT EXISTS turno (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_final TIME NOT NULL,
    CONSTRAINT uq_turno_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS fabrica (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uq_fabrica_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS mini_fabrica (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    fabrica_id BIGINT NOT NULL REFERENCES fabrica(id) ON DELETE RESTRICT,
    CONSTRAINT uq_mini_fabrica_name_per_fabrica UNIQUE (fabrica_id, name)
);

CREATE TABLE IF NOT EXISTS setor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE RESTRICT,
    CONSTRAINT uq_setor_name_per_mini_fabrica UNIQUE (mini_fabrica_id, name)
);

CREATE TABLE IF NOT EXISTS device (
    id SERIAL PRIMARY KEY,
    mnemonic VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    no_id INTEGER UNIQUE CHECK (no_id >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_device_mnemonic ON device(mnemonic);

CREATE TABLE IF NOT EXISTS memory (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    address INTEGER NOT NULL DEFAULT 0 CHECK (address >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_memory_device_name ON memory(device_id, name);

CREATE TABLE IF NOT EXISTS memory_value (
    id SERIAL PRIMARY KEY,
    memory_id INTEGER NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS memory_value_current (
    memory_id INTEGER PRIMARY KEY REFERENCES memory(id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    status BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tag (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE CASCADE,
    memory_id INTEGER NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_memory_same_device
        FOREIGN KEY (memory_id, device_id)
        REFERENCES memory(id, device_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS machine (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE RESTRICT,
    setor_id BIGINT NOT NULL REFERENCES setor(id) ON DELETE RESTRICT,
    CONSTRAINT uq_machine_name_per_setor UNIQUE (setor_id, name)
);

CREATE TABLE IF NOT EXISTS produto (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    formato VARCHAR(80) NOT NULL,
    metro_quadrado_formato INTEGER NOT NULL CHECK (metro_quadrado_formato >= 0),
    CONSTRAINT uq_produto_name_formato UNIQUE (name, formato)
);

CREATE TABLE IF NOT EXISTS defeito (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    CONSTRAINT uq_defeito_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS producao (
    id BIGSERIAL PRIMARY KEY,
    ordem_value NUMERIC(18, 4) NOT NULL,
    value NUMERIC(18, 4) NOT NULL,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    minutos_trabalhando INTEGER NOT NULL DEFAULT 0 CHECK (minutos_trabalhando >= 0),
    minutos_parados INTEGER NOT NULL DEFAULT 0 CHECK (minutos_parados >= 0),
    produto_id BIGINT NOT NULL REFERENCES produto(id) ON DELETE RESTRICT,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS qualidade (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    value INTEGER NOT NULL,
    hora TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS qualidade_defeito_valor (
    id BIGSERIAL PRIMARY KEY,
    qualidade_id BIGINT NOT NULL REFERENCES qualidade(id) ON DELETE CASCADE,
    defeito_id BIGINT NOT NULL REFERENCES defeito(id) ON DELETE RESTRICT,
    value INTEGER NOT NULL CHECK (value >= 0),
    CONSTRAINT uq_qualidade_defeito UNIQUE (qualidade_id, defeito_id)
);

CREATE TABLE IF NOT EXISTS producao_por_turno (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE RESTRICT,
    producao_id BIGINT NOT NULL REFERENCES producao(id) ON DELETE CASCADE,
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE RESTRICT,
    data DATE NOT NULL,
    CONSTRAINT uq_producao_por_turno UNIQUE (turno_id, producao_id, mini_fabrica_id, data)
);

CREATE TABLE IF NOT EXISTS producao_por_turno_machine (
    producao_por_turno_id BIGINT NOT NULL REFERENCES producao_por_turno(id) ON DELETE CASCADE,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    PRIMARY KEY (producao_por_turno_id, machine_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'device'
          AND column_name = 'no_id'
    ) THEN
        ALTER TABLE public.device
            ADD COLUMN no_id INTEGER;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'no_id'
    ) THEN
        UPDATE public.device d
        SET no_id = n.no_id
        FROM public.no_id n
        WHERE n.device_id = d.id
          AND d.no_id IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_device_no_id_non_negative'
    ) THEN
        ALTER TABLE public.device
            ADD CONSTRAINT chk_device_no_id_non_negative
            CHECK (no_id IS NULL OR no_id >= 0)
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'memory'
          AND column_name = 'address'
    ) THEN
        ALTER TABLE public.memory
            ADD COLUMN address INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;

UPDATE public.memory
SET address = COALESCE(
    NULLIF(SUBSTRING(name FROM '^DM_0*([0-9]+)$'), '')::INTEGER,
    NULLIF(SUBSTRING(name FROM '^RR_0*([0-9]+)\\.[0-9]+$'), '')::INTEGER,
    0
);

CREATE INDEX IF NOT EXISTS idx_mini_fabrica_fabrica ON mini_fabrica(fabrica_id);
CREATE INDEX IF NOT EXISTS idx_setor_mini_fabrica ON setor(mini_fabrica_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_no_id ON device(no_id) WHERE no_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_memory_device ON memory(device_id);
CREATE INDEX IF NOT EXISTS idx_memory_device_address ON memory(device_id, address);
CREATE UNIQUE INDEX IF NOT EXISTS idx_memory_id_device ON memory(id, device_id);
CREATE INDEX IF NOT EXISTS idx_memory_value_memory ON memory_value(memory_id);
CREATE INDEX IF NOT EXISTS idx_memory_value_updated_at ON memory_value(updated_at);
CREATE INDEX IF NOT EXISTS idx_memory_value_current_updated_at ON memory_value_current(updated_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_device_name ON tag(device_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_memory_unique ON tag(memory_id);
CREATE INDEX IF NOT EXISTS idx_tag_device ON tag(device_id);
CREATE INDEX IF NOT EXISTS idx_tag_memory ON tag(memory_id);
CREATE INDEX IF NOT EXISTS idx_machine_device ON machine(device_id);
CREATE INDEX IF NOT EXISTS idx_machine_setor ON machine(setor_id);
CREATE INDEX IF NOT EXISTS idx_producao_machine ON producao(machine_id);
CREATE INDEX IF NOT EXISTS idx_producao_turno ON producao(turno_id);
CREATE INDEX IF NOT EXISTS idx_producao_produto ON producao(produto_id);
CREATE INDEX IF NOT EXISTS idx_qualidade_machine ON qualidade(machine_id);
CREATE INDEX IF NOT EXISTS idx_qualidade_turno ON qualidade(turno_id);
CREATE INDEX IF NOT EXISTS idx_qualidade_hora ON qualidade(hora);
CREATE INDEX IF NOT EXISTS idx_qdv_qualidade ON qualidade_defeito_valor(qualidade_id);
CREATE INDEX IF NOT EXISTS idx_qdv_defeito ON qualidade_defeito_valor(defeito_id);
CREATE INDEX IF NOT EXISTS idx_ppt_turno ON producao_por_turno(turno_id);
CREATE INDEX IF NOT EXISTS idx_ppt_producao ON producao_por_turno(producao_id);
CREATE INDEX IF NOT EXISTS idx_ppt_mini_fabrica ON producao_por_turno(mini_fabrica_id);
CREATE INDEX IF NOT EXISTS idx_ppt_data ON producao_por_turno(data);
CREATE INDEX IF NOT EXISTS idx_pptm_machine ON producao_por_turno_machine(machine_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_tag_memory_same_device'
    ) THEN
        ALTER TABLE public.tag
            ADD CONSTRAINT fk_tag_memory_same_device
            FOREIGN KEY (memory_id, device_id)
            REFERENCES public.memory(id, device_id)
            ON DELETE CASCADE
            NOT VALID;
    END IF;
END $$;
