# Integracao Dashboard x API HostLink Omron

Este guia descreve a integracao do dashboard atual (`web/src/app/App.tsx`) com a API do projeto.

## 1. Fluxo atual

1. Frontend consulta a API por `fetch('/api/...')`.
2. O Vite faz proxy para `VITE_API_URL` (default `http://localhost:8080`).
3. O dashboard faz polling com backoff em falhas de comunicacao.
4. Dados exibidos vem de TAGs salvas no Postgres pelo collector.

## 2. Configuracao local

Crie ou ajuste `web/.env`:

```env
VITE_API_URL=http://localhost:8080
```

Execute:

```bash
cd web
npm install
npm run dev
```

## 3. Endpoints usados pelo App

`App.tsx` consulta:

1. `GET /api/devices/PLC/tag/PECAPH29`
2. `GET /api/devices/PLC/tag/PECAROLLERCARGA41`
3. `GET /api/devices/PLC/tag/QUALIDADE41`
4. E equivalentes para linhas 42 e 43.

Se uma TAG nao existir, o backend retorna `404` e o frontend tenta fallback definido no array de tags da linha.

## 4. Estrutura de dados esperada

Resposta do endpoint de TAG:

```json
{
  "tagName": "PECAPH29",
  "memoryName": "DM29",
  "deviceMnemonic": "PLC",
  "value": 123,
  "updatedAt": "2026-02-15T10:30:00"
}
```

Campos usados no frontend:

1. `value` -> metrica exibida no card.
2. `updatedAt` -> calculo de ultimo update da linha.

## 5. Dependencias do pipeline completo

Para o dashboard mostrar valores reais:

1. Banco Postgres ativo e schema aplicado.
2. API ativa (`mvn -pl api spring-boot:run`).
3. Collector ativo (GUI ou headless) para preencher `memory_value_current`.
4. TAGs cadastradas para os nomes usados no `App.tsx`.

## 6. Cadastro de TAGs (exemplo)

```bash
curl -X POST "http://localhost:8080/api/devices/PLC/tags/dm/29?name=PECAPH29"
curl -X POST "http://localhost:8080/api/devices/PLC/tags/dm/100?name=PECAROLLERCARGA41"
curl -X POST "http://localhost:8080/api/devices/PLC/tags/dm/200?name=QUALIDADE41"
```

Repita para as demais linhas.

## 7. Troubleshooting rapido

1. Erro `Falha de conexao`: API inacessivel em `VITE_API_URL`.
2. HTTP `404` em TAG: nome nao cadastrado ou collector nao inicializou mapeamento.
3. Valores zerados: collector parado ou sem mudanca de dados no CLP.
4. CORS/proxy: validar `web/vite.config.ts` e variavel `VITE_API_URL`.
