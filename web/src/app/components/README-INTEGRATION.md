# Integração do Dashboard Industrial em Projeto Real

## 1. Copiar os Arquivos

Copie todos os componentes para seu projeto:

```
src/
  app/
    components/
      ProductionLine.tsx
      MetricCard.tsx
      QualityChart.tsx
      DefectsList.tsx
    App.tsx
```

## 2. Dependências Necessárias

Certifique-se de instalar as dependências:

```bash
npm install lucide-react
# ou
yarn add lucide-react
# ou
pnpm add lucide-react
```

## 3. Integração com API Real

### 3.1 Criar Service para API

```typescript
// src/services/productionApi.ts
export interface ProductionData {
  lineNumber: number;
  lineName: string;
  pecasPrensa: number;
  pecasRoller: number;
  aprovadas: number;
  rejeitadas: number;
  trendPrensa: number;
  trendRoller: number;
  defects: Defect[];
}

export const productionApi = {
  // Buscar dados de todas as linhas
  async getAllLines(): Promise<ProductionData[]> {
    const response = await fetch('/api/production/lines');
    return response.json();
  },

  // Buscar dados de uma linha específica
  async getLine(lineId: number): Promise<ProductionData> {
    const response = await fetch(`/api/production/lines/${lineId}`);
    return response.json();
  },

  // Atualização em tempo real via WebSocket
  subscribeToUpdates(callback: (data: ProductionData[]) => void) {
    const ws = new WebSocket('ws://seu-servidor.com/production');
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      callback(data);
    };

    return () => ws.close();
  }
};
```

### 3.2 Atualizar App.tsx com dados reais

```typescript
import { useEffect, useState } from 'react';
import { productionApi, ProductionData } from '../services/productionApi';

export default function App() {
  const [lines, setLines] = useState<ProductionData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Carregar dados iniciais
    loadProductionData();

    // Configurar atualização automática
    const interval = setInterval(loadProductionData, 30000); // A cada 30s

    // Ou usar WebSocket para tempo real
    const unsubscribe = productionApi.subscribeToUpdates(setLines);

    return () => {
      clearInterval(interval);
      unsubscribe();
    };
  }, []);

  async function loadProductionData() {
    try {
      const data = await productionApi.getAllLines();
      setLines(data);
      setLoading(false);
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
    }
  }

  if (loading) {
    return <div>Carregando...</div>;
  }

  return (
    // Renderizar com dados reais
    <div className="grid grid-cols-3 gap-4">
      {lines.map((line) => (
        <ProductionLine key={line.lineNumber} {...line} />
      ))}
    </div>
  );
}
```

## 4. Backend - Exemplo de API

### 4.1 Node.js + Express

```javascript
// server.js
const express = require('express');
const app = express();

// Endpoint para buscar dados das linhas
app.get('/api/production/lines', async (req, res) => {
  // Buscar do banco de dados
  const lines = await db.query(`
    SELECT 
      line_id,
      line_name,
      pecas_prensa,
      pecas_roller,
      aprovadas,
      rejeitadas,
      trend_prensa,
      trend_roller
    FROM production_lines
    WHERE active = true
  `);

  const defects = await db.query(`
    SELECT *
    FROM defects
    WHERE created_at > NOW() - INTERVAL '1 day'
    ORDER BY created_at DESC
    LIMIT 9
  `);

  // Agrupar defeitos por linha
  const response = lines.map(line => ({
    ...line,
    defects: defects.filter(d => d.line_id === line.line_id).slice(0, 3)
  }));

  res.json(response);
});
```

### 4.2 Python + FastAPI

```python
# main.py
from fastapi import FastAPI
from typing import List

app = FastAPI()

@app.get("/api/production/lines")
async def get_production_lines():
    # Buscar do banco de dados
    lines = await db.fetch_all("""
        SELECT 
            line_id,
            line_name,
            pecas_prensa,
            pecas_roller,
            aprovadas,
            rejeitadas,
            trend_prensa,
            trend_roller
        FROM production_lines
        WHERE active = true
    """)
    
    return lines
```

## 5. Banco de Dados - Estrutura

```sql
-- Tabela de linhas de produção
CREATE TABLE production_lines (
    line_id SERIAL PRIMARY KEY,
    line_name VARCHAR(100),
    pecas_prensa INTEGER,
    pecas_roller INTEGER,
    aprovadas INTEGER,
    rejeitadas INTEGER,
    trend_prensa DECIMAL(5,2),
    trend_roller DECIMAL(5,2),
    active BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de defeitos
CREATE TABLE defects (
    id SERIAL PRIMARY KEY,
    line_id INTEGER REFERENCES production_lines(line_id),
    tipo VARCHAR(100),
    descricao TEXT,
    quantidade INTEGER,
    severidade VARCHAR(20) CHECK (severidade IN ('alta', 'media', 'baixa')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_defects_line_date ON defects(line_id, created_at DESC);
CREATE INDEX idx_defects_created_at ON defects(created_at DESC);
```

## 6. Integração com Sistemas Industriais

### 6.1 OPC UA (Padrão Industrial)

```typescript
// src/services/opcuaClient.ts
import { OPCUAClient } from 'node-opcua';

export class IndustrialDataCollector {
  async connectToPLC(endpoint: string) {
    const client = OPCUAClient.create({
      endpointMustExist: false
    });

    await client.connect(endpoint);
    const session = await client.createSession();

    return session;
  }

  async readProductionData(session) {
    const nodesToRead = [
      'ns=2;s=Line1.Prensa.Count',
      'ns=2;s=Line1.Roller.Count',
      'ns=2;s=Line1.Quality.Approved',
      'ns=2;s=Line1.Quality.Rejected'
    ];

    const dataValues = await session.read(nodesToRead);
    return dataValues;
  }
}
```

### 6.2 MQTT (IoT)

```typescript
// src/services/mqttClient.ts
import mqtt from 'mqtt';

export class MQTTCollector {
  connect(brokerUrl: string) {
    const client = mqtt.connect(brokerUrl);

    client.on('connect', () => {
      client.subscribe('production/line1/#');
      client.subscribe('production/line2/#');
      client.subscribe('production/line3/#');
    });

    client.on('message', (topic, message) => {
      const data = JSON.parse(message.toString());
      // Processar e atualizar dashboard
      this.updateDashboard(topic, data);
    });
  }
}
```

## 7. Segurança

```typescript
// src/utils/auth.ts
export const authMiddleware = {
  // Adicionar autenticação
  async authenticate(token: string) {
    const response = await fetch('/api/auth/verify', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    return response.ok;
  }
};

// No App.tsx
const [isAuthenticated, setIsAuthenticated] = useState(false);

useEffect(() => {
  const token = localStorage.getItem('auth_token');
  authMiddleware.authenticate(token).then(setIsAuthenticated);
}, []);
```

## 8. Deploy

### 8.1 Build para Produção

```bash
npm run build
```

### 8.2 Docker

```dockerfile
# Dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --production

COPY . .
RUN npm run build

EXPOSE 3000

CMD ["npm", "start"]
```

### 8.3 Variáveis de Ambiente

```env
# .env.production
VITE_API_URL=https://api.suaempresa.com
VITE_WS_URL=wss://ws.suaempresa.com
VITE_UPDATE_INTERVAL=30000
```

## 9. Monitoramento e Logs

```typescript
// src/utils/logger.ts
export const logger = {
  error(message: string, error: any) {
    // Enviar para sistema de logging (Sentry, DataDog, etc)
    console.error(message, error);
    
    fetch('/api/logs', {
      method: 'POST',
      body: JSON.stringify({
        level: 'error',
        message,
        stack: error.stack,
        timestamp: new Date().toISOString()
      })
    });
  }
};
```

## 10. Performance

```typescript
// Usar React.memo para evitar re-renders desnecessários
export const ProductionLine = React.memo(({ /* props */ }) => {
  // componente
});

// Lazy loading
const ProductionLine = lazy(() => import('./components/ProductionLine'));
```

## Checklist de Integração

- [ ] Dependências instaladas
- [ ] API backend configurada
- [ ] Banco de dados criado
- [ ] Integração com sistemas industriais (PLC/SCADA)
- [ ] Autenticação implementada
- [ ] WebSocket ou polling configurado
- [ ] Testes realizados
- [ ] Build de produção testado
- [ ] Deploy realizado
- [ ] Monitoramento configurado

## 11. Configuracao local (Vite + backend Java)

No desenvolvimento, o frontend pode chamar apenas caminhos relativos (`/api/...`).
O Vite faz proxy para o backend usando `VITE_API_URL`.

### 11.1 Arquivo de ambiente

Crie `web/.env` baseado em `web/.env.example`:

```env
VITE_API_URL=http://localhost:8080
```

### 11.2 Proxy configurado

`web/vite.config.ts` ja esta configurado para redirecionar:

- `/api/*` -> `${VITE_API_URL}/api/*`

Assim voce evita CORS no desenvolvimento e mantem os mesmos caminhos no frontend.
