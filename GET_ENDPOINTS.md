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
2. `GET /api/devices/tag/{name}` (busca por nome em todos os PLCs)
3. `POST /api/devices/{mnemonic}/tags/dm/{address}?name={tagName}`
4. `POST /api/devices/{mnemonic}/tags/rr/{address}/bit/{bit}?name={tagName}`

## Exemplos

1. `GET http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `GET http://localhost:8080/api/devices/PLC/dm/0/current`
3. `GET http://localhost:8080/api/devices/PLC/rr/10/bit/0`
4. `GET http://localhost:8080/api/devices/tag/PECAPH29`
5. `GET http://localhost:8080/api/devices/PLC1CARGA/tag/PECAROLLERCARGA41`
6. `POST http://localhost:8080/api/devices/PLC1CARGA/tags/dm/29?name=PECAPH29`

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
  "deviceMnemonic": "PLC1CARGA",
  "value": 123,
  "updatedAt": "2026-02-15T10:30:00"
}
```

`TagValue[]` (GET `/api/devices/tag/{name}`):

```json
[
  {
    "tagName": "PECAPH29",
    "memoryName": "DM29",
    "deviceMnemonic": "PLC4PRENSA",
    "value": 10,
    "updatedAt": "2026-02-22T09:40:00"
  },
  {
    "tagName": "PECAPH29",
    "memoryName": "DM29",
    "deviceMnemonic": "PLC1CARGA",
    "value": 8,
    "updatedAt": "2026-02-22T09:39:00"
  }
]
```

## SQL util (PostgreSQL)

PLCs cadastrados:

```sql
SELECT d.id, d.mnemonic, d.name, d.description
FROM public.device d
ORDER BY d.id;
```

TAGs salvas por PLC:

```sql
SELECT
  d.id       AS device_id,
  d.mnemonic AS plc_mnemonic,
  d.name     AS plc_name,
  t.name     AS tag_name,
  m.name     AS memory_name
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
ORDER BY d.id, t.name;
```

TAGs com valor atual:

```sql
SELECT
  d.id           AS device_id,
  d.mnemonic     AS plc_mnemonic,
  t.name         AS tag_name,
  m.name         AS memory_name,
  mvc.value      AS current_value,
  mvc.updated_at AS current_updated_at
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
ORDER BY d.id, t.name;
```
