# API Endpoints

Base path: `/api`

## DM

1. `GET /api/devices/{mnemonic}/dm/{address}`
2. `GET /api/devices/{mnemonic}/dm/{address}/last`
3. `GET /api/devices/{mnemonic}/dm/{address}/current`
4. `GET /api/devices/{mnemonic}/dm/last`
5. `GET /api/devices/{mnemonic}/dm?start=0&end=1000`

## RR

1. `GET /api/devices/{mnemonic}/rr/{address}/bit/{bit}`
2. `GET /api/devices/{mnemonic}/rr/10/bit/0`
3. `GET /api/devices/{mnemonic}/rr/{address}/bits?start=0&end=15`
4. `GET /api/devices/{mnemonic}/rr/{address}`

## TAG

1. `GET /api/devices/{mnemonic}/tag/{name}`
2. `POST /api/devices/{mnemonic}/tags/dm/{address}?name={tagName}`
3. `POST /api/devices/{mnemonic}/tags/rr/{address}/bit/{bit}?name={tagName}`

## Exemplos

1. `GET http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC/dm/0/current`
3. `GET http://localhost:8080/api/devices/PLC/rr/10/bit/0`
4. `GET http://localhost:8080/api/devices/PLC/tag/PECAPH29`
5. `POST http://localhost:8080/api/devices/PLC/tags/dm/29?name=PECAPH29`

## Formato de resposta (resumo)

`MemoryValue` (DM/RR):

```json
{
  "name": "DM29",
  "value": 123,
  "updatedAt": "2026-02-15T10:30:00"
}
```

`TagValue` (TAG):

```json
{
  "tagName": "PECAPH29",
  "memoryName": "DM29",
  "deviceMnemonic": "PLC",
  "value": 123,
  "updatedAt": "2026-02-15T10:30:00"
}
```
