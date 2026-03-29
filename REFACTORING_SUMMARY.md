<!-- spellcheck: off -->

# Refatoração da Classe CollectorMultPlcAplication

## Resumo da Refatoração

A classe `CollectorMultPlcAplication` foi grande demais (1500+ linhas) com múltiplas responsabilidades. Refatorei dividindo em **7 subclasses especializadas**, cada uma com responsabilidade clara.

---

## Subclasses Criadas

### 1. **SerialCommunicationManager**

**Caminho**: `org.omron.collector.service.SerialCommunicationManager`

**Responsabilidades**:

- Conectar/desconectar porta serial
- Gerenciar estado da conexão
- Validar configurações de porta
- Tratar erros de conexão (porta em uso, inválida, etc)
- Gerenciar desconexão forçada por nodes não responsivos

**Métodos principais**:

- `connect(port, baud, dataBits, stopBits, parity, timeout, rts, dtr)`
- `disconnect()`
- `requestDisconnectByUnresponsiveNode(nodeId)`
- `isConnected()`
- `getSharedSerial()` - acesso ao SharedSerial para operações I/O

---

### 2. **DatabaseManager**

**Caminho**: `org.omron.collector.service.DatabaseManager`

**Responsabilidades**:

- Gerenciar ciclo de vida do contexto Spring (inicializar/encerrar)
- Executar queries de configuração (devices, tags, production tags)
- Operações CRUD de dados (salvar valores de memória)
- Encapsular acesso a JdbcTemplate e DmValueService

**Métodos principais**:

- `initialize()` / `close()`
- `loadAllDevicesWithTags()`
- `loadTagsForDevice(deviceMnemonic)`
- `loadManualProductionTagBindings(deviceMnemonic, tagNames)`
- `saveMemoryValuesBatch(deviceInfo, values)`
- `saveMemoryValuesCurrentOnly(deviceInfo, values)`

**Data Classes**:

- `DeviceConfigData` - configuração de device
- `TagData` - dados de tag individual
- `ManualProductionTagBinding` - binding de tag de produção manual

---

### 3. **LoggingService**

**Caminho**: `org.omron.collector.service.LoggingService`

**Responsabilidades**:

- Gerenciar logging em área de texto Swing
- Persistir logs em arquivo (`collector-mult-plc.log`)
- Limitar altura do log (máx 4000 linhas)
- Instalar handler global de exceções

**Métodos principais**:

- `log(message)` / `logError(context, error)`
- `logInfo(message)` / `logWarning(message)`
- `setLogArea(area)` / `setExternalCallback(callback)`
- `installGlobalHandler(callback)` - static

---

### 4. **PlcConfigurationLoader**

**Caminho**: `org.omron.collector.service.PlcConfigurationLoader`

**Responsabilidades**:

- Carregar configurações de PLCs do banco
- Construir catálogo de tags conhecidas via reflection
- Converter dados DB em estruturas de monitoramento
- Localizar PLCs por ID de node ou mnemônico

**Métodos principais**:

- `loadAllConfigurations()` → List<PlcConfiguration>
- `loadTagsForDevice(deviceMnemonic)` → List<MonitoredTag>
- `findConfigurationByNodeId(configs, nodeId)`
- `findConfigurationByMnemonic(configs, mnemonic)`

**Data Class**:

- `PlcConfiguration` - configuração completa de um PLC

---

### 5. **ManualDmDataProcessor**

**Caminho**: `org.omron.collector.service.ManualDmDataProcessor`

**Responsabilidades**:

- Parsear respostas de leitura DM
- Detectar e construir timestamps de dados
- Validar marcadores (data e relativos)
- Decodificar valores BCD
- Normalizar e formatar dados

**Métodos principais**:

- `parseSingleWord(reply)` → int
- `toHexWord(value)` → String
- `matchesManualDateMarker(address, value)` → boolean
- `matchesManualRelativeMarker(index, value)` → boolean
- `tryBuildManualTimestamp(valuesInOrder, offset)` → LocalDateTime
- `formatTimestamp(dt)` / `formatTimestampConcatenated(dt)`

---

### 6. **ManualDmScanManager**

**Caminho**: `org.omron.collector.service.ManualDmScanManager`

**Responsabilidades**:

- Executar thread de leitura sequencial de endereços DM
- Processar valores lidos (parsing, validação)
- Persistir valores de produção em BD
- Limpar valores após leitura
- Gerenciar callbacks de resultado e status

**Métodos principais**:

- `startScan(nodeId, startAddress)`
- `stopScan()`
- `isRunning()` → boolean
- `setResultCallback(callback)` / `setStatusCallback(callback)`
- `setProductionTagNames(names)`

---

### 7. **CollectorMultPlcAplication (Refatorado)**

**Caminho**: `org.omron.collector.CollectorMultPlcAplication`

**Responsabilidades após refatoração**:

- Orquestrar componentes (serial, db, leitura manual)
- Construir e gerenciar UI
- Gerenciar lifecycle da aplicação
- Conectar/desconectar componentes

**Mudanças**:

- Reduzida de ~1500 para ~400 linhas
- Injeção de dependências através de construtores das subclasses
- Delegação de lógica complexa às subclasses especializadas

---

## Benefícios da Refatoração

✅ **Maior Manutenibilidade**

- Cada classe com responsabilidade única e bem definida
- Fácil localizar e editar lógica específica

✅ **Melhor Testabilidade**

- Classes pequenas são mais fáceis de testar isoladamente
- Pode-se mockar dependências para testes unitários

✅ **Reutilização**

- Serviços podem ser usados em outras partes da aplicação
- Não precisa duplicar lógica de serial, DB ou logging

✅ **Escalabilidade**

- Adicionar novos serviços é mais fácil
- Modificações isoladas não afetam outras partes

✅ **Legibilidade**

- Classe principal mais simples de entender
- Nomes de classes descrevem sua responsabilidade

---

## Exemplo de Migração: Antes vs Depois

### Antes (Monolítica)

```java
public class CollectorMultPlcAplication {
    private JFrame frame;
    private JTextArea logArea;
    private SharedSerial sharedSerial;
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    // ... 70+ campos

    private void connectSharedSerial() {
        // 50 linhas de lógica
        SharedSerial.Config config = new SharedSerial.Config(...);
        sharedSerial.connect(config);
        // ... tratamento de erro, logging, atualização de status
    }

    private void log(String message) {
        // Lógica de logging duplicada
    }
}
```

### Depois (Modular)

```java
public class CollectorMultPlcAplication {
    private DatabaseManager dbManager = new DatabaseManager();
    private SerialCommunicationManager serialManager;
    private LoggingService logger;
    // 4 dependências claras

    private void connectSharedSerial() {
        serialManager.connect(port, baud, dataBits, ...);
        dbManager.initialize();
    }

    private void log(String message) {
        logger.log(message); // Sempre delegado
    }
}
```

---

## Próximos Passos (Opcional)

1. **Adicionar injeção de dependências** (Spring/CDI)
   - Ao invés de `new DatabaseManager()`, usar @Autowired

2. **Criar testes unitários**
   - Testar SerialCommunicationManager com porta mockada
   - Testar DatabaseManager com BD mockada
   - Testar ManualDmDataProcessor com dados fixtures

3. **Adicionar mais serviços**
   - UIFactory para construir painéis
   - StateManager para estado global da aplicação

4. **Refatorar UI** (se necessário)
   - Extrair construção de painéis para UIFactory
   - Separar prévia de componentes UI

---

## Estrutura de Diretórios

```
collector/src/main/java/org/omron/collector/
├── CollectorMultPlcAplication.java (REFATORADO)
├── service/
│   ├── SerialCommunicationManager.java
│   ├── DatabaseManager.java
│   ├── LoggingService.java
│   ├── PlcConfigurationLoader.java
│   ├── ManualDmScanManager.java
│   └── ManualDmDataProcessor.java
├── util/
│   ├── PlcNodeMonitorPanel.java (existente)
│   └── SharedSerial.java (existente)
```

---

**Refatoração concluída e testada para compilação!**
