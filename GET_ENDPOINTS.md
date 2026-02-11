# GET Endpoints
<!-- cSpell:ignore Endpoints -->

Base path: `/api`

**DM**
1. `GET /api/devices/{mnemonic}/dm/{address}`
2. `GET /api/devices/{mnemonic}/dm/{address}/last`
3. `GET /api/devices/{mnemonic}/dm/{address}/current`
4. `GET /api/devices/{mnemonic}/dm/last`
5. `GET /api/devices/{mnemonic}/dm?start=0&end=1000`

**RR**
1. `GET /api/devices/{mnemonic}/rr/{address}/bit/{bit}`
2. `GET /api/devices/{mnemonic}/rr/10/bit/0`
3. `GET /api/devices/{mnemonic}/rr/{address}/bits?start=0&end=15`
4. `GET /api/devices/{mnemonic}/rr/{address}`

**TAG**
1. `GET /api/devices/{mnemonic}/tag/{name}`

**EXEMPLOS**

1. `http://localhost:8080/api/devices/PLC/dm?start=0&end=10`
2. `http://localhost:8080/api/devices/PLC/dm/0`
