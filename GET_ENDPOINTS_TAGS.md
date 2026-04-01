<!-- spellcheck: off -->

# Tag Read Endpoints

Base path: /api

## 1. Device-specific tag value

- **Endpoint:** GET /api/devices/{mnemonic}/tag/{name}
- **Purpose:** retorna o valor atual de uma TAG cadastrada para um dispositivo específico.
- **Resposta:** TagValue com tagName, memoryName, deviceMnemonic, value, updatedAt.

## 2. Global tag value

- **Endpoint:** GET /api/devices/tag/{name}
- **Purpose:** recupera os valores mais recentes dessa TAG em todos os PLCs que a possuem.
- **Resposta:** array TagValue[] ordenado por deviceMnemonic.

## Exemplos

1. GET http://localhost:8080/api/devices/PLC1CARGA/tag/PRODUCAO_PH29
2. GET http://localhost:8080/api/devices/tag/PRODUCAO_PH29 (retorna lista com cada PLC)

## Formato de resposta

```json
{
  "tagName": "PRODUCAO_PH29",
  "memoryName": "DM_0308",
  "deviceMnemonic": "PRENSA",
  "value": 15063,
  "updatedAt": "2026-03-22T17:40:59"
}
```

## Nota para o frontend

- Use a query com {mnemonic} quando quiser o valor de uma TAG em um PLC específico.
- Chame a rota sem {mnemonic} quando precisar de todos os dispositivos que reportam a mesma TAG.
