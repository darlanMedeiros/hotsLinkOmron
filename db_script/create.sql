## 3. criar banco e usuario postgres

No PostgreSQL (psql ou pgAdmin), execute:

```sql
CREATE DATABASE omron;
CREATE USER omron_user WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE omron TO omron_user;
```