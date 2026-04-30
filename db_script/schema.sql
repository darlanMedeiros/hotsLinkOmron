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

CREATE INDEX IF NOT EXISTS idx_memory_device_name ON memory(device_id, name, address, bit);

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

CREATE TABLE IF NOT EXISTS quality_role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true
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
    amostragem INTEGER NOT NULL CHECK (amostragem >= 0),
    value INTEGER NOT NULL CHECK (value >= 0),
    CONSTRAINT uq_qualidade_defeito UNIQUE (qualidade_id, defeito_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'qualidade_defeito_valor'
          AND column_name = 'amostragem'
    ) THEN
        ALTER TABLE public.qualidade_defeito_valor
            ADD COLUMN amostragem INTEGER NOT NULL DEFAULT 0 CHECK (amostragem >= 0);
    END IF;
END $$;

UPDATE public.tag
SET
    quality_group = COALESCE(NULLIF(TRIM(quality_group), ''), 'DEFAULT'),
    quality_role = CASE
        WHEN name = 'Qualidade_Gatilho' THEN 'TRIGGER'
        WHEN name = 'Total_Defeitos1' THEN 'DEFECT_TOTAL_1'
        WHEN name = 'Total_Defeitos2' THEN 'DEFECT_TOTAL_2'
        WHEN name = 'Total_Defeitos3' THEN 'DEFECT_TOTAL_3'
        WHEN name = 'Codigo_Defeito1' THEN 'DEFECT_CODE_1'
        WHEN name = 'Codigo_Defeito2' THEN 'DEFECT_CODE_2'
        WHEN name = 'Codigo_Defeito3' THEN 'DEFECT_CODE_3'
        WHEN name = 'Qtde_Amostragem' THEN 'SAMPLING'
        WHEN name = 'Qualidade_Ano' THEN 'YEAR'
        WHEN name = 'Qualidade_Mes' THEN 'MONTH'
        WHEN name = 'Qualidade_Dia' THEN 'DAY'
        WHEN name = 'Qualidade_Hora' THEN 'HOUR'
        WHEN name = 'Qualidade_Min' THEN 'MINUTE'
        WHEN name = 'Qualidade_Seg' THEN 'SECOND'
        WHEN name = 'Qualidade_Maquina_Current' THEN 'MACHINE_QUALITY_CURRENT'
        WHEN name = 'Qualidade_Maquina_Persisted' THEN 'MACHINE_QUALITY_PERSISTED'
        ELSE quality_role
    END
WHERE
    (quality_group IS NULL OR TRIM(quality_group) = '')
    OR quality_role IS NULL;

INSERT INTO public.quality_role (name, description, active)
SELECT DISTINCT
    t.quality_role,
    'Papel de qualidade usado no collector',
    true
FROM public.tag t
WHERE t.quality_role IS NOT NULL
  AND TRIM(t.quality_role) <> ''
ON CONFLICT (name) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND column_name = 'quality_group'
    ) THEN
        ALTER TABLE public.tag
            ADD COLUMN quality_group VARCHAR(80);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tag'
          AND column_name = 'quality_role'
    ) THEN
        ALTER TABLE public.tag
            ADD COLUMN quality_role VARCHAR(40);
    END IF;
END $$;

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

-- Remove indice unico legado antes da normalizacao de address/bit para evitar
-- quebra por duplicidade durante o UPDATE abaixo.
DROP INDEX IF EXISTS idx_memory_device_name;
DROP INDEX IF EXISTS idx_tag_memory_unique;

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
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT device_id, name, address, bit
            FROM public.memory
            GROUP BY device_id, name, address, bit
            HAVING COUNT(*) > 1
        ) d
    ) THEN
        CREATE TEMP TABLE tmp_memory_dedup AS
        SELECT m.id AS loser_id,
               keep.keep_id
        FROM public.memory m
        JOIN (
            SELECT device_id, name, address, bit, MIN(id) AS keep_id
            FROM public.memory
            GROUP BY device_id, name, address, bit
            HAVING COUNT(*) > 1
        ) keep
          ON keep.device_id = m.device_id
         AND keep.name = m.name
         AND keep.address = m.address
         AND keep.bit = m.bit
        WHERE m.id <> keep.keep_id;

        -- Evita conflito de unique (machine_id, name) ao reatribuir tag.memory_id
        DELETE FROM public.tag t
        USING tmp_memory_dedup d, public.tag t_keep
        WHERE t.memory_id = d.loser_id
          AND t_keep.memory_id = d.keep_id
          AND t_keep.machine_id = t.machine_id
          AND t_keep.name = t.name;

        UPDATE public.tag t
        SET memory_id = d.keep_id
        FROM tmp_memory_dedup d
        WHERE t.memory_id = d.loser_id;

        -- Se mais de uma tag acabar apontando para o mesmo memory_id, mantemos a
        -- de menor id para satisfazer o indice unico idx_tag_memory_unique.
        DELETE FROM public.tag t
        USING (
            SELECT id
            FROM (
                SELECT id,
                       ROW_NUMBER() OVER (PARTITION BY memory_id ORDER BY id) AS rn
                FROM public.tag
            ) x
            WHERE x.rn > 1
        ) dup
        WHERE t.id = dup.id;

        INSERT INTO public.memory_value_current (memory_id, value, status, updated_at)
        SELECT src.keep_id, src.value, src.status, src.updated_at
        FROM (
            SELECT DISTINCT ON (d.keep_id)
                   d.keep_id,
                   mvc.value,
                   mvc.status,
                   mvc.updated_at
            FROM public.memory_value_current mvc
            JOIN tmp_memory_dedup d ON d.loser_id = mvc.memory_id
            ORDER BY d.keep_id, mvc.updated_at DESC, mvc.memory_id DESC
        ) src
        ON CONFLICT (memory_id) DO UPDATE
        SET value = EXCLUDED.value,
            status = EXCLUDED.status,
            updated_at = EXCLUDED.updated_at
        WHERE EXCLUDED.updated_at >= public.memory_value_current.updated_at;

        DELETE FROM public.memory_value_current mvc
        USING tmp_memory_dedup d
        WHERE mvc.memory_id = d.loser_id;

        UPDATE public.memory_value mv
        SET memory_id = d.keep_id
        FROM tmp_memory_dedup d
        WHERE mv.memory_id = d.loser_id;

        DELETE FROM public.memory m
        USING tmp_memory_dedup d
        WHERE m.id = d.loser_id;
    END IF;
END $$;

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
CREATE INDEX IF NOT EXISTS idx_tag_quality_group_role ON tag(machine_id, quality_group, quality_role);
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

DO $$ 
BEGIN 
    -- Garante que a coluna existe
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name='defeito' AND column_name='number') THEN
        ALTER TABLE public.defeito ADD COLUMN number INTEGER;
    END IF;

    -- Limpa duplicatas antes de aplicar a constraint (define como NULL registros extras com mesmo número)
    UPDATE public.defeito 
    SET number = NULL 
    WHERE id NOT IN (
        SELECT MIN(id)
        FROM public.defeito
        WHERE number IS NOT NULL
        GROUP BY number
    ) AND number IS NOT NULL;

    -- Adiciona a restrição de unicidade se não existir
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_defeito_number') THEN
        ALTER TABLE public.defeito ADD CONSTRAINT uq_defeito_number UNIQUE (number);
    END IF;
END $$;
