# hotsLinkOmron

Projeto de comunicacao com CLP Omron via protocolo HostLink, com coleta de dados, persistencia em Postgres e API REST para frontend.

## Modulos
1. `api`: exposicao REST para o frontend, leitura/escrita de dados persistidos e operacoes de tags.
2. `core`: biblioteca base (protocolo HostLink, serial, modelos, repositorios e servicos compartilhados).
3. `collector`: regra de negocio de coleta, comunicacao com CLP e persistencia periodica no banco.

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

## Executar API
1. Configure o Postgres (exemplo: database `omron`, usuario `omron_user`).
2. Ajuste variaveis de ambiente quando necessario: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
3. Inicie a aplicacao `api` (classe `org.ctrl.db.ApiApplication`).

## Executar Collector
1. Ajuste os parametros seriais em `collector/src/main/java/org/omron/collector/DmMonitorApplication.java`.
2. Garanta acesso ao mesmo banco configurado no `core`.
3. Execute a classe `org.omron.collector.DmMonitorApplication`.

## Endpoints GET
Arquivo completo:
1. `GET_ENDPOINTS.md`

Exemplos:
1. `GET http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC/dm/0`
3. `GET http://localhost:8080/api/devices/PLC/rr/10/bit/0`
4. `GET http://localhost:8080/api/devices/PLC/tag/producao`

## Endpoints POST (TAG)
1. `POST http://localhost:8080/api/devices/PLC/tags/dm/0?name=producao`
2. `POST http://localhost:8080/api/devices/PLC/tags/rr/10/bit/0?name=lampada`

## Demos GUI
1. `core/src/main/java/test/demo/gui/TagTestGui.java`
2. `core/src/main/java/test/demo/gui/DmTestGui.java`
3. `core/src/main/java/test/demo/gui/RrWrBitGui.java`
