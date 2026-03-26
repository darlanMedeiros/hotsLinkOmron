# ALL_ENDPOINTS

Base URL: `/api`

## Devices (`/api/devices`)
- `GET /api/devices`
- `GET /api/devices/{id}`
- `POST /api/devices`
- `PUT /api/devices/{id}`
- `DELETE /api/devices/{id}`

## Memories (`/api/memories`)
- `GET /api/memories`
- `GET /api/memories/{id}`
- `POST /api/memories`
- `PUT /api/memories/{id}`
- `DELETE /api/memories/{id}`

## Tags CRUD (`/api/tags`)
- `GET /api/tags`
- `GET /api/tags/{id}`
- `POST /api/tags`
- `PUT /api/tags/{id}`
- `DELETE /api/tags/{id}`

## Turnos (`/api/turnos`)
- `GET /api/turnos`
- `GET /api/turnos/{id}`
- `POST /api/turnos`
- `PUT /api/turnos/{id}`
- `DELETE /api/turnos/{id}`

## Fabricas (`/api/fabricas`)
- `GET /api/fabricas`
- `GET /api/fabricas/{id}`
- `POST /api/fabricas`
- `PUT /api/fabricas/{id}`
- `DELETE /api/fabricas/{id}`

## Mini Fabricas (`/api/mini-fabricas`)
- `GET /api/mini-fabricas`
- `GET /api/mini-fabricas/{id}`
- `POST /api/mini-fabricas`
- `PUT /api/mini-fabricas/{id}`
- `DELETE /api/mini-fabricas/{id}`

## Setores (`/api/setores`)
- `GET /api/setores`
- `GET /api/setores/{id}`
- `POST /api/setores`
- `PUT /api/setores/{id}`
- `DELETE /api/setores/{id}`

## Machines (`/api/machines`)
- `GET /api/machines`
- `GET /api/machines/{id}`
- `POST /api/machines`
- `PUT /api/machines/{id}`
- `DELETE /api/machines/{id}`

## Memory Values CRUD (`/api/memory-values`)
- `GET /api/memory-values`
- `GET /api/memory-values/{id}`
- `POST /api/memory-values`
- `PUT /api/memory-values/{id}`
- `DELETE /api/memory-values/{id}`

## Memory Search by Device
- `GET /api/devices/{mnemonic}/memory-values`

## DM
- `GET /api/devices/{mnemonic}/dm/{address}`
- `GET /api/devices/{mnemonic}/dm/{address}/last`
- `GET /api/devices/{mnemonic}/dm/{address}/current`
- `GET /api/devices/{mnemonic}/dm/last`
- `GET /api/devices/{mnemonic}/dm`

## RR
- `GET /api/devices/{mnemonic}/rr/{address}/bit/{bit}`
- `GET /api/devices/{mnemonic}/rr/10/bit/0`
- `GET /api/devices/{mnemonic}/rr/{address}/bits`
- `GET /api/devices/{mnemonic}/rr/{address}`

## Tag by Device / Global Tag
- `GET /api/devices/{mnemonic}/tag/{name}`
- `GET /api/devices/tag/{name}`

## Create Tag Mapping from Device + Memory Type
- `POST /api/devices/{mnemonic}/tags/dm/{address}`
- `POST /api/devices/{mnemonic}/tags/rr/{address}/bit/{bit}`
