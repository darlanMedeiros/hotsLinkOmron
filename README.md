<!-- spellcheck: off -->

# hotsLinkOmron

Projeto para comunicacao com CLP Omron via protocolo HostLink, com coleta de dados, persistencia em Postgres, API REST em Spring Boot e frontend React.

## Modulos

1. `core`: biblioteca base (protocolo HostLink, serial, modelos, repositorios e servicos).
2. `api`: exposicao REST para consulta de DM, RR e TAG.
3. `collector`: processos de coleta e persistencia no banco (modo GUI e modo headless).
4. `web`: dashboard React + Vite para monitoramento em tempo real.

## Pre-requisitos

1. Java 11+
2. Maven 3.9+
3. Node.js 18+
4. PostgreSQL 14+ (ou compativel)

## Banco de dados

Scripts SQL:

1. `core/src/main/resources/db/schema.sql` (cria estruturas sem reset)
2. `core/src/main/resources/db/init-db.sql` (recria schema `public`)

Variaveis de ambiente usadas por `DbConfig`:

1. `DB_URL` (default: `jdbc:postgresql://localhost:5432/omron?currentSchema=public`)
2. `DB_USER` (default: `omron_user`)
3. `DB_PASSWORD` (default: `admin`)

## Execucao local

### 1. API (Spring Boot)

Na raiz do projeto:

```bash
mvn -pl api spring-boot:run
```

URL padrao: `http://localhost:8080`

### 2. Collector GUI (Swing)

```bash
mvn -pl collector -am exec:java -Dexec.mainClass=org.omron.collector.CollectorGuiApplication
```

Ou via script Windows:

```powershell
./run-gui.ps1
```

### 3. Collector headless (loop DM)

```bash
mvn -pl collector -am exec:java -Dexec.mainClass=org.omron.collector.DmMonitorApplication
```

### 4. Frontend (Vite)

```bash
cd web
npm install
npm run dev
```

URL padrao: `http://localhost:5173`

## Integracao frontend-backend

1. O frontend chama `/api/...`.
2. O Vite faz proxy para `VITE_API_URL` (default: `http://localhost:8080`).
3. Configuracao em `web/vite.config.ts`.
4. Exemplo de ambiente em `web/.env.example`.

## Endpoints

Lista completa em `GET_ENDPOINTS.md`.

Exemplos:

1. `GET http://localhost:8080/api/devices/PLC1CARGA/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC1CARGA/dm/0/current`
3. `GET http://localhost:8080/api/devices/PLC1CARGA/rr/10/bit/0`
4. `GET http://localhost:8080/api/devices/tag/PECAPH29`
5. `GET http://localhost:8080/api/devices/PLC1CARGA/tag/PECAPH29`
6. `POST http://localhost:8080/api/devices/PLC1CARGA/tags/dm/29?name=PECAPH29`

## Documentacao adicional

1. Arquitetura: `ARCHITECTURE.md`
2. Endpoints: `GET_ENDPOINTS.md`
3. Integracao dashboard: `web/src/app/components/README-INTEGRATION.md`

## Observacoes

1. A API fixa timezone em `America/Sao_Paulo` (`ApiApplication`).
2. O dashboard web ja consome API real (nao usa mais mock).
3. O collector GUI grava log em `collector-gui.log`.
