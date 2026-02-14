# Architecture

## Overview
O projeto esta dividido em quatro modulos com responsabilidades separadas:

1. `api` (Spring Boot): exposicao HTTP/REST para consumo externo.
2. `core` (lib compartilhada): protocolo HostLink, modelos e acesso a dados.
3. `collector` (processo de coleta): leitura do CLP e persistencia.
4. `web` (React + Vite): frontend de visualizacao e operacao.

## Responsibilities by Module

### api
1. Disponibiliza endpoints para consulta de DM/RR/TAG.
2. Recebe requisicoes do frontend.
3. Nao deve conter logica de comunicacao direta com CLP.
4. Usa servicos/repositorios fornecidos por `core`.

### core
1. Implementa protocolo de comunicacao com CLP Omron (HostLink).
2. Centraliza modelos de dominio, repositorios e servicos de banco.
3. Fornece componentes reutilizaveis para `api` e `collector`.
4. Nao depende de `api`, `collector` ou `web`.

### collector
1. Executa monitoramento/leitura do CLP em loop ou agenda.
2. Aplica regra de negocio de coleta (frequencia, mudanca de valor, etc.).
3. Persiste valores no banco usando servicos do `core`.
4. Nao expoe endpoints HTTP.

### web
1. Renderiza dashboards e telas operacionais.
2. Consome endpoints REST da `api` via `/api/...`.
3. Em desenvolvimento usa proxy do Vite para evitar CORS.
4. Nao acessa banco ou CLP diretamente.

## Data Flow

### Fluxo de coleta
1. `collector` abre comunicacao serial com o CLP.
2. `collector` le dados de memoria (ex.: DM/RR).
3. `collector` aplica regra de negocio.
4. `collector` grava no banco via `core`.

### Fluxo de consulta
1. `web` chama endpoint no `api`.
2. `api` consulta servicos/repositorios do `core`.
3. `api` retorna dados persistidos ao `web`.

## Dependency Direction
Direcao recomendada:
1. `api` -> `core`
2. `collector` -> `core`
3. `web` -> `api` (via HTTP)
4. `core` -> (sem dependencia de `api`, `collector`, `web`)

Nao permitido:
1. `core` depender de `api`
2. `core` depender de `collector`
3. `core` depender de `web`
4. `api` depender de `collector`
5. `web` depender de `core` ou `collector`

## Local Development
1. API: `mvn -pl api spring-boot:run` (porta 8080)
2. Web: `cd web && npm run dev` (porta 5173)
3. Proxy do Vite: `/api/*` -> `VITE_API_URL` (default `http://localhost:8080`)