DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE turno (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_final TIME NOT NULL,
    CONSTRAINT uq_turno_name UNIQUE (name)
);

CREATE TABLE fabrica (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uq_fabrica_name UNIQUE (name)
);

CREATE TABLE mini_fabrica (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    fabrica_id BIGINT NOT NULL REFERENCES fabrica(id) ON DELETE RESTRICT,
    CONSTRAINT uq_mini_fabrica_name_per_fabrica UNIQUE (fabrica_id, name)
);

CREATE TABLE setor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uq_setor_name UNIQUE (name)
);

CREATE TABLE mini_fabrica_setor (
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE CASCADE,
    setor_id BIGINT NOT NULL REFERENCES setor(id) ON DELETE RESTRICT,
    PRIMARY KEY (mini_fabrica_id, setor_id)
);

CREATE TABLE device (
    id SERIAL PRIMARY KEY,
    mnemonic VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    no_id INTEGER CHECK (no_id >= 0)
);

CREATE TABLE memory (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL REFERENCES device(id) ON DELETE CASCADE,
    name VARCHAR(10) NOT NULL CHECK (name IN ('DM', 'HR', 'RR', 'WR', 'TC')),
    address INTEGER NOT NULL DEFAULT 0 CHECK (address >= 0),
    bit SMALLINT NOT NULL DEFAULT -1 CHECK (bit BETWEEN -1 AND 15),
    UNIQUE (id, device_id)
);

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

CREATE TABLE tag (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    memory_id INTEGER NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
    persist_history BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_tag_machine_name UNIQUE (machine_id, name)
);

CREATE TABLE machine (
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

CREATE TABLE produto (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    formato VARCHAR(80) NOT NULL,
    metro_quadrado_formato INTEGER NOT NULL CHECK (metro_quadrado_formato >= 0),
    CONSTRAINT uq_produto_name_formato UNIQUE (name, formato)
);

CREATE TABLE defeito (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    CONSTRAINT uq_defeito_nome UNIQUE (nome)
);

CREATE TABLE producao (
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

CREATE TABLE qualidade (
    id BIGSERIAL PRIMARY KEY,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    value INTEGER NOT NULL,
    hora TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE RESTRICT
);

CREATE TABLE qualidade_defeito_valor (
    id BIGSERIAL PRIMARY KEY,
    qualidade_id BIGINT NOT NULL REFERENCES qualidade(id) ON DELETE CASCADE,
    defeito_id BIGINT NOT NULL REFERENCES defeito(id) ON DELETE RESTRICT,
    amostragem INTEGER NOT NULL CHECK (amostragem >= 0),
    value INTEGER NOT NULL CHECK (value >= 0),
    CONSTRAINT uq_qualidade_defeito UNIQUE (qualidade_id, defeito_id)
);

CREATE TABLE producao_por_turno (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL REFERENCES turno(id) ON DELETE RESTRICT,
    producao_id BIGINT NOT NULL REFERENCES producao(id) ON DELETE CASCADE,
    mini_fabrica_id BIGINT NOT NULL REFERENCES mini_fabrica(id) ON DELETE RESTRICT,
    data DATE NOT NULL,
    CONSTRAINT uq_producao_por_turno UNIQUE (turno_id, producao_id, mini_fabrica_id, data)
);

CREATE TABLE producao_por_turno_machine (
    producao_por_turno_id BIGINT NOT NULL REFERENCES producao_por_turno(id) ON DELETE CASCADE,
    machine_id BIGINT NOT NULL REFERENCES machine(id) ON DELETE RESTRICT,
    PRIMARY KEY (producao_por_turno_id, machine_id)
);

CREATE UNIQUE INDEX idx_device_mnemonic ON device(mnemonic);
CREATE UNIQUE INDEX idx_device_no_id ON device(no_id) WHERE no_id IS NOT NULL;
CREATE UNIQUE INDEX idx_memory_device_name ON memory(device_id, name, address, bit);
CREATE INDEX idx_memory_device ON memory(device_id);
CREATE INDEX idx_memory_device_address ON memory(device_id, name, address, bit);
CREATE UNIQUE INDEX idx_memory_id_device ON memory(id, device_id);
CREATE INDEX idx_memory_value_memory ON memory_value(memory_id);
CREATE INDEX idx_memory_value_updated_at ON memory_value(updated_at);
CREATE INDEX idx_memory_value_current_updated_at ON memory_value_current(updated_at);
CREATE UNIQUE INDEX idx_tag_machine_name ON tag(machine_id, name);
CREATE UNIQUE INDEX idx_tag_memory_unique ON tag(memory_id);
CREATE INDEX idx_tag_machine ON tag(machine_id);
CREATE INDEX idx_tag_memory ON tag(memory_id);
CREATE INDEX idx_mini_fabrica_fabrica ON mini_fabrica(fabrica_id);
CREATE INDEX idx_mini_fabrica_setor_mini_fabrica ON mini_fabrica_setor(mini_fabrica_id);
CREATE INDEX idx_mini_fabrica_setor_setor ON mini_fabrica_setor(setor_id);
CREATE INDEX idx_machine_device ON machine(device_id);
CREATE INDEX idx_machine_mini_fabrica ON machine(mini_fabrica_id);
CREATE INDEX idx_machine_setor ON machine(setor_id);
CREATE INDEX idx_producao_machine ON producao(machine_id);
CREATE INDEX idx_producao_turno ON producao(turno_id);
CREATE INDEX idx_producao_produto ON producao(produto_id);
CREATE INDEX idx_qualidade_machine ON qualidade(machine_id);
CREATE INDEX idx_qualidade_turno ON qualidade(turno_id);
CREATE INDEX idx_qualidade_hora ON qualidade(hora);
CREATE INDEX idx_qdv_qualidade ON qualidade_defeito_valor(qualidade_id);
CREATE INDEX idx_qdv_defeito ON qualidade_defeito_valor(defeito_id);
CREATE INDEX idx_ppt_turno ON producao_por_turno(turno_id);
CREATE INDEX idx_ppt_producao ON producao_por_turno(producao_id);
CREATE INDEX idx_ppt_mini_fabrica ON producao_por_turno(mini_fabrica_id);
CREATE INDEX idx_ppt_data ON producao_por_turno(data);
CREATE INDEX idx_pptm_machine ON producao_por_turno_machine(machine_id);
