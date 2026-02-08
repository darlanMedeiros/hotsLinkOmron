# hotsLinkOmron

Projeto de comunicação com CLP Omron via protocolo HostLink, com persistência em Postgres e API REST para leitura de valores DM/RR e TAGs.

**Módulos**
1. `core`: comunicação com CLP, serviços e persistência.
2. `api`: API REST (Spring Boot).

**Banco de Dados**
Scripts:
1. `core/src/main/resources/db/schema.sql`
2. `core/src/main/resources/db/init-db.sql`

Tabelas principais:
1. `device`
2. `memory`
3. `memory_value`
4. `memory_value_current`
5. `tag`

**Executar API**
1. Configure o Postgres (ex.: `omron` com usuário `omron_user`).
2. Ajuste variáveis de ambiente se necessário:
   `DB_URL`, `DB_USER`, `DB_PASSWORD`.
3. Inicie a aplicação `api` (classe `org.ctrl.db.ApiApplication`).

**Endpoints GET**
Arquivo completo:
1. `GET_ENDPOINTS.md`

Exemplos:
1. `GET http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC/dm/0`
3. `GET http://localhost:8080/api/devices/PLC/rr/10/bit/0`
4. `GET http://localhost:8080/api/device/PLC/tag/producao`

**Endpoints POST (TAG)**
1. `POST http://localhost:8080/api/devices/PLC/tags/dm/0?name=producao`
2. `POST http://localhost:8080/api/devices/PLC/tags/rr/10/bit/0?name=lampada`

**Demos GUI**
1. `core/src/main/java/test/demo/gui/TagTestGui.java`
2. `core/src/main/java/test/demo/gui/DmTestGui.java`
3. `core/src/main/java/test/demo/gui/RrWrBitGui.java`
