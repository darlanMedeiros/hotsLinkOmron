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
    CONSTRAINT uq_setor_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS mini_fabrica_setor (
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE CASCADE,
    setor_id BIGINT NOT NULL REFERENCES setor(id) ON DELETE RESTRICT,
    PRIMARY KEY (mini_fabrica_id, setor_id)
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
    name VARCHAR(10) NOT NULL,
    address INTEGER NOT NULL DEFAULT 0 CHECK (address >= 0),
    bit SMALLINT NOT NULL DEFAULT -1 CHECK (bit BETWEEN -1 AND 15)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_memory_device_name ON memory(device_id, name, address, bit);

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
    machine_id BIGINT NOT NULL,
    memory_id INTEGER NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
    persist_history BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_tag_machine_name UNIQUE (machine_id, name)
);

CREATE TABLE IF NOT EXISTS machine (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE RESTRICT,
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE RESTRICT,
    setor_id BIGINT NOT NULL REFERENCES setor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_machine_mini_fabrica_setor
        FOREIGN KEY (mini_fabrica_id, setor_id)
        REFERENCES mini_fabrica_setor(mini_fabrica_id, setor_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_machine_name_per_mini_fabrica_setor UNIQUE (mini_fabrica_id, setor_id, name)
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
    name VARCHAR(120) NOT NULL,
    number INTEGER UNIQUE,
    CONSTRAINT uq_defeito_name UNIQUE (name)
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
          AND table_name = 'tag'
          AND column_name = 'persist_history'
    ) THEN
        ALTER TABLE public.tag
            ADD COLUMN persist_history BOOLEAN NOT NULL DEFAULT true;
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

-- mini_fabrica_setor migration
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'mini_fabrica_setor'
    ) THEN
        CREATE TABLE public.mini_fabrica_setor (
            mini_fabrica_id BIGINT NOT NULL REFERENCES public.mini_fabrica(id) ON DELETE CASCADE,
            setor_id BIGINT NOT NULL REFERENCES public.setor(id) ON DELETE RESTRICT,
            PRIMARY KEY (mini_fabrica_id, setor_id)
        );
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'setor'
          AND column_name = 'mini_fabrica_id'
    ) THEN
        INSERT INTO public.mini_fabrica_setor (mini_fabrica_id, setor_id)
        SELECT s.mini_fabrica_id, s.id
        FROM public.setor s
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'machine'
          AND column_name = 'mini_fabrica_id'
    ) THEN
        ALTER TABLE public.machine ADD COLUMN mini_fabrica_id BIGINT;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'setor'
          AND column_name = 'mini_fabrica_id'
    ) THEN
        UPDATE public.machine m
        SET mini_fabrica_id = s.mini_fabrica_id
        FROM public.setor s
        WHERE m.setor_id = s.id
          AND m.mini_fabrica_id IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'machine'
          AND column_name = 'mini_fabrica_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
              AND table_name = 'machine'
              AND constraint_name = 'fk_machine_mini_fabrica_setor'
        ) THEN
            ALTER TABLE public.machine
                ADD CONSTRAINT fk_machine_mini_fabrica_setor
                FOREIGN KEY (mini_fabrica_id, setor_id)
                REFERENCES public.mini_fabrica_setor(mini_fabrica_id, setor_id)
                ON DELETE RESTRICT
                NOT VALID;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
              AND table_name = 'machine'
              AND constraint_name = 'fk_machine_mini_fabrica'
        ) THEN
            ALTER TABLE public.machine
                ADD CONSTRAINT fk_machine_mini_fabrica
                FOREIGN KEY (mini_fabrica_id)
                REFERENCES public.mini_fabrica(id)
                ON DELETE RESTRICT
                NOT VALID;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_machine_name_per_mini_fabrica_setor'
        ) THEN
            CREATE UNIQUE INDEX IF NOT EXISTS uq_machine_name_per_mini_fabrica_setor
                ON public.machine(mini_fabrica_id, setor_id, name);
        END IF;
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE public.machine DROP CONSTRAINT IF EXISTS uq_machine_name_per_setor;
END $$;

DO $$
BEGIN
    ALTER TABLE public.setor DROP CONSTRAINT IF EXISTS uq_setor_name_per_mini_fabrica;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'setor'
          AND column_name = 'mini_fabrica_id'
    ) THEN
        ALTER TABLE public.setor DROP COLUMN mini_fabrica_id;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_setor_name'
    ) THEN
        ALTER TABLE public.setor
            ADD CONSTRAINT uq_setor_name UNIQUE (name);
    END IF;
END $$;

-- tag_machine migration
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND column_name = 'machine_id'
    ) THEN
        ALTER TABLE public.tag ADD COLUMN machine_id BIGINT;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND column_name = 'device_id'
    ) THEN
        UPDATE public.tag t
        SET machine_id = m.id
        FROM (
            SELECT device_id, MIN(id) AS id
            FROM public.machine
            GROUP BY device_id
        ) m
        WHERE m.device_id = t.device_id
          AND t.machine_id IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE public.tag DROP CONSTRAINT IF EXISTS fk_tag_memory_same_device;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND constraint_name = 'fk_tag_machine'
    ) THEN
        ALTER TABLE public.tag
            ADD CONSTRAINT fk_tag_machine
            FOREIGN KEY (machine_id)
            REFERENCES public.machine(id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE public.tag DROP CONSTRAINT IF EXISTS uq_tag_machine_name;
    ALTER TABLE public.tag
        ADD CONSTRAINT uq_tag_machine_name UNIQUE (machine_id, name);
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND column_name = 'device_id'
    ) THEN
        ALTER TABLE public.tag DROP COLUMN device_id;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_mini_fabrica_fabrica ON mini_fabrica(fabrica_id);
CREATE INDEX IF NOT EXISTS idx_mini_fabrica_setor_mini_fabrica ON mini_fabrica_setor(mini_fabrica_id);
CREATE INDEX IF NOT EXISTS idx_mini_fabrica_setor_setor ON mini_fabrica_setor(setor_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_no_id ON device(no_id) WHERE no_id IS NOT NULL;
DROP INDEX IF EXISTS idx_memory_device_name;
CREATE UNIQUE INDEX IF NOT EXISTS idx_memory_device_name ON memory(device_id, name, address, bit);
CREATE INDEX IF NOT EXISTS idx_memory_device ON memory(device_id);
CREATE INDEX IF NOT EXISTS idx_memory_device_address ON memory(device_id, name, address, bit);
CREATE UNIQUE INDEX IF NOT EXISTS idx_memory_id_device ON memory(id, device_id);
CREATE INDEX IF NOT EXISTS idx_memory_value_memory ON memory_value(memory_id);
CREATE INDEX IF NOT EXISTS idx_memory_value_updated_at ON memory_value(updated_at);
CREATE INDEX IF NOT EXISTS idx_memory_value_current_updated_at ON memory_value_current(updated_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_machine_name ON tag(machine_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_memory_unique ON tag(memory_id);
CREATE INDEX IF NOT EXISTS idx_tag_machine ON tag(machine_id);
CREATE INDEX IF NOT EXISTS idx_tag_memory ON tag(memory_id);
CREATE INDEX IF NOT EXISTS idx_machine_device ON machine(device_id);
CREATE INDEX IF NOT EXISTS idx_machine_mini_fabrica ON machine(mini_fabrica_id);
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

