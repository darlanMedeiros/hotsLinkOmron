# Architecture

## Overview
O projeto esta dividido em tres modulos Maven com responsabilidades separadas:

1. `api`: exposicao HTTP/REST para o frontend.
2. `core`: biblioteca base compartilhada (comunicacao com CLP, modelos e acesso a dados).
3. `collector`: processo de coleta que aplica regras de negocio para ler o CLP e persistir no banco.

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
4. Nao deve depender de `api` nem de regras especificas de coleta do `collector`.

### collector
1. Executa monitoramento/leitura do CLP em loop ou agenda.
2. Aplica regra de negocio de coleta (faixas, comparacao de mudanca, frequencia, etc.).
3. Persiste valores no banco usando servicos do `core`.
4. Nao expõe endpoints HTTP.

## Data Flow
Fluxo de coleta:
1. `collector` abre comunicacao serial com o CLP.
2. `collector` le dados de memoria (ex.: DM).
3. `collector` aplica regra de negocio (ex.: salvar apenas alteracoes).
4. `collector` grava no banco via `core`.

Fluxo de consulta:
1. `frontend` chama endpoint no `api`.
2. `api` consulta servicos/repositorios do `core`.
3. `api` retorna os dados persistidos ao `frontend`.

## Dependency Direction
Direcao de dependencia recomendada:

1. `api` -> `core`
2. `collector` -> `core`
3. `core` -> (sem dependencia de `api` e `collector`)

Nao permitido:
1. `core` depender de `api`
2. `core` depender de `collector`
3. `api` depender de `collector`
