# hotsLinkOmron

Projeto para comunicacao com CLP Omron via protocolo HostLink, com coleta de dados, persistencia em Postgres, API REST em Spring Boot e frontend web.

## Modulos
1. `api`: exposicao REST para consulta de DM/RR/TAG.
2. `core`: biblioteca base (HostLink, serial, modelos, repositorios e servicos).
3. `collector`: rotina de coleta e persistencia no banco.
4. `web`: frontend React + Vite.

## Arquitetura
Visao completa em `ARCHITECTURE.md`.

## Banco de dados
Scripts:
1. `core/src/main/resources/db/schema.sql`
2. `core/src/main/resources/db/init-db.sql`

Tabelas principais:
1. `device`
2. `memory`
3. `memory_value`
4. `memory_value_current`
5. `tag`

## Subir localmente
Pre-requisitos:
1. Java 17+
2. Maven 3.9+
3. Node.js 18+
4. Postgres configurado

### Backend (API Spring Boot)
1. Configure `DB_URL`, `DB_USER`, `DB_PASSWORD` se necessario.
2. Execute na raiz:

```bash
mvn -pl api spring-boot:run
```

API padrao: `http://localhost:8080`

### Frontend (Vite)
1. Crie `web/.env` a partir de `web/.env.example`.
2. Execute:

```bash
cd web
npm install
npm run dev
```

Frontend padrao: `http://localhost:5173`

## Integracao frontend-backend
- No desenvolvimento, o frontend chama `/api/...`.
- O Vite faz proxy para `VITE_API_URL` (default `http://localhost:8080`).
- Configuracao em `web/vite.config.ts`.
- Exemplo de ambiente em `web/.env.example`.

## Status atual do frontend
- Estrutura e build do modulo `web` estao funcionando.
- Proxy para backend esta configurado.
- `web/src/app/App.tsx` ainda usa dados mockados.
- Proximo passo: trocar mocks por chamadas reais para `/api/devices/...`.

## Endpoints principais
Ver lista em `GET_ENDPOINTS.md`.

Exemplos:
1. `GET http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC/dm/0/current`
3. `GET http://localhost:8080/api/devices/PLC/rr/10/bit/0`
4. `GET http://localhost:8080/api/devices/PLC/tag/producao`
5. `POST http://localhost:8080/api/devices/PLC/tags/dm/0?name=producao`

## Demos GUI (core)
1. `core/src/main/java/test/demo/gui/TagTestGui.java`
2. `core/src/main/java/test/demo/gui/DmTestGui.java`
3. `core/src/main/java/test/demo/gui/RrWrBitGui.java`