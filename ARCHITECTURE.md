# Architecture

## Overview

O projeto esta dividido em quatro modulos com responsabilidades separadas:

1. `core`: comunicacao HostLink, acesso a dados e regras base.
2. `api`: camada HTTP/REST para consumo externo.
3. `collector`: leitura do CLP e persistencia no banco.
4. `web`: dashboard React para visualizacao operacional.

## Responsibility by module

### core

1. Implementa serial + protocolo Omron Toolbus/HostLink.
2. Centraliza modelos (`DeviceInfo`, `MemoryValue`, `TagValue`) e repositorios JDBC.
3. Expoe servicos (`DmValueService`, `RrValueService`, `TagService`) reutilizados por `api` e `collector`.
4. Nao depende de `api`, `collector` ou `web`.

### api

1. Sobe a aplicacao Spring Boot (`ApiApplication`).
2. Expoe endpoints REST em `/api`:
   - DM: `DmValueRestController`
   - RR: `RrValueRestController`
   - TAG: `TagRestController`
3. Importa `DbConfig` do `core` para wiring de `DataSource`, repositorios e servicos.
4. Nao implementa comunicacao serial direta com CLP.

### collector

1. Usa serial + protocolo do `core` para leitura no CLP.
2. Persiste alteracoes no banco via servicos do `core`.
3. Possui 2 entradas:
   - `CollectorGuiApplication`: interface Swing para operacao e monitoramento de TAGs.
   - `DmMonitorApplication`: processo headless de leitura de faixa DM.
4. Nao expoe endpoints HTTP.

### web

1. Dashboard React/Vite em `web/src/app/App.tsx`.
2. Consome API real por `fetch('/api/...')` com polling.
3. Em desenvolvimento usa proxy Vite para `VITE_API_URL`.
4. Nao acessa CLP nem banco diretamente.

## Data flow

### Coleta e persistencia

1. `collector` abre comunicacao serial com o CLP.
2. Le valores de DM/TAG.
3. Detecta mudancas.
4. Grava historico em `memory_value` e estado atual em `memory_value_current` via `core`.

### Consulta via API

1. `web` chama endpoints `GET/POST /api/...`.
2. `api` delega para servicos/repositorios do `core`.
3. Dados sao lidos de Postgres e retornados para o frontend.

## Dependency direction

Permitido:

1. `api -> core`
2. `collector -> core`
3. `web -> api` (HTTP)

Nao permitido:

1. `core -> api`
2. `core -> collector`
3. `core -> web`
4. `api -> collector`
5. `web -> core` ou `collector`

## Runtime defaults

1. API: `http://localhost:8080`
2. Web: `http://localhost:5173`
3. Proxy Vite: `/api/* -> VITE_API_URL` (default `http://localhost:8080`)
4. Banco default (`DbConfig`):
   - `DB_URL=jdbc:postgresql://localhost:5432/omron?currentSchema=public`
   - `DB_USER=omron_user`
   - `DB_PASSWORD=admin`

## Operacao recomendada em dev

1. Subir Postgres e aplicar SQL do `core/src/main/resources/db`.
2. Iniciar API (`mvn -pl api spring-boot:run`).
3. Iniciar collector (GUI ou headless) para alimentar dados.
4. Iniciar web (`cd web && npm run dev`).
