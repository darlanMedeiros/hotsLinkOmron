# instalacao local (windows)

## 1. pre-requisitos

Instale no computador:

1. Java 21
2. Maven 3.9+
3. Node.js 18+
4. PostgreSQL 14+

## 2. obter o projeto

Clone ou copie o repositorio e abra terminal na raiz:

```powershell
cd C:\caminho\hotsLinkOmron
```

## 3. criar banco e usuario postgres

No PostgreSQL (psql ou pgAdmin), execute:

```sql
CREATE DATABASE omron;
CREATE USER omron_user WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE omron TO omron_user;
```

## 4. inicializar schema

Opcao A (recria schema public):

```powershell
psql -h localhost -U postgres -d omron -f core/src/main/resources/db/init-db.sql
```

Opcao B (cria estruturas sem reset):

```powershell
psql -h localhost -U postgres -d omron -f core/src/main/resources/db/schema.sql
```

## 5. conferir configuracao de ambiente

Arquivos relevantes:

1. `core/src/main/resources/db-dev.properties`
2. `core/src/main/resources/db-prod.properties`
3. `api/src/main/resources/application.properties` (default: `spring.profiles.active=dev`)

Configuracao padrao dev:

- URL: `jdbc:postgresql://localhost:5432/omron?currentSchema=public`
- Usuario: `omron_user`
- Senha: `admin`

Observacao: variaveis `DB_URL`, `DB_USER` e `DB_PASSWORD` sobrescrevem os arquivos.

## 6. subir API

Em um terminal na raiz:

```powershell
mvn -pl api -am spring-boot:run
```

API padrao: `http://localhost:8080`

## 7. subir collector

Em outro terminal na raiz:

```powershell
.\run-collector.ps1
```

Alternativa manual:

```powershell
mvn exec:java -pl collector "-Dexec.mainClass=org.omron.collector.CollectorMultPlcAplication"
```

## 8. subir web

Em outro terminal:

```powershell
cd web
npm install
npm run dev
```

Web padrao: `http://localhost:5173`

## 9. integracao web -> api

O frontend usa proxy `/api` para backend definido em `VITE_API_URL`.

Se precisar, crie `web/.env` com:

```env
VITE_API_URL=http://localhost:8080
```

## 10. checklist rapido

1. API respondendo em `http://localhost:8080`
2. Web abrindo em `http://localhost:5173`
3. Collector conectado e gravando no banco
4. Tela web exibindo dados da API
