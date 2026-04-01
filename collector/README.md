<!-- spellcheck: off -->

# Modulo `collector`

Aplicacao desktop Swing para coleta de dados de PLCs Omron via HostLink, com:

- monitoramento continuo de multiplos PLCs em uma serial compartilhada;
- persistencia de valores DM no banco;
- leitura manual de bloco DM com limpeza (`#0000`) apos leitura.

## Objetivo

Centralizar a aquisicao de dados de dispositivos configurados no banco (`device`, `machine`, `tag`, `memory`) e gravar variacoes de valores em `memory_value` usando `DmValueService`.

## Entrypoints

- `org.omron.collector.CollectorMultPlcAplication`: tela principal atual (multi-PLC + aba de leitura manual DM).
- `org.omron.collector.CollectorGuiApplication`: versao legado/simples de interface.
- `org.omron.collector.DmMonitorApplication`: execucao de monitoramento sem interface grafica.

O `pom.xml` do modulo define `CollectorGuiApplication` no manifesto do JAR, mas o fluxo mais completo atualmente esta em `CollectorMultPlcAplication`.

## Arquitetura

### Camada de UI

- `CollectorMultPlcAplication`
  - monta a janela principal;
  - cria paineis por PLC (`PlcNodeMonitorPanel`);
  - controla conexao serial compartilhada;
  - integra a aba de leitura manual DM (`ManualDmScanManager`);
  - coordena ciclo de desligamento seguro.

- `PlcNodeMonitorPanel`
  - representa 1 PLC monitorado;
  - permite iniciar/parar monitor por node;
  - faz polling periodico de tags DM;
  - salva apenas quando valor muda;
  - aplica regras de resiliencia (backoff, timeout, stop por max retries).

### Comunicacao serial

- `SharedSerial`
  - encapsula `SerialPortHandlerImp`;
  - aplica protocolo Toolbus;
  - compartilha lock de I/O (`ioLock`) para acesso sincronizado entre threads;
  - fornece utilitarios de portas (`refreshAvailablePorts`, `isPortAvailable`).

- `SerialCommunicationManager`
  - orquestra conectar/desconectar;
  - publica status para UI;
  - detecta porta em uso;
  - executa desconexao forcada quando um node fica sem resposta prolongada.

### Dados e configuracao

- `DatabaseManager`
  - inicializa contexto Spring (`DbConfig`);
  - fornece `JdbcTemplate` e `DmValueService`;
  - carrega configuracoes de device/tag/memory;
  - persiste lotes com e sem historico.

- `PlcConfigurationLoader`
  - transforma registros de banco em configuracao de monitoramento;
  - resolve tamanho de leitura das tags via catalogo `Tag` (reflection).

### Scanner DM manual

- `ManualDmScanManager`
  - le sequencialmente DM a partir de endereco inicial;
  - para ao encontrar `#FFFF` ou ao atingir limite;
  - interpreta blocos de producao;
  - grava no banco;
  - escreve `#0000` nos enderecos lidos.

- `ManualDmDataProcessor`
  - parsing de palavras retornadas;
  - deteccao de marcadores;
  - montagem de timestamp (incluindo fallback BCD);
  - constantes de tamanho/offset de bloco manual.

### Logging

- `LoggingService`
  - escreve no `JTextArea` (UI);
  - persiste em arquivo (`collector-mult-plc.log`);
  - instala handler global para excecoes nao tratadas.

## Fluxo principal (Monitoramento)

1. Aplicacao carrega devices/tags do banco.
2. Usuario conecta a serial compartilhada.
3. Cada painel PLC inicia thread de monitoramento.
4. Para cada tag:
   - envia `AreaReadDM`;
   - converte resposta;
   - compara com ultimo valor conhecido;
   - persiste somente alteracoes.
5. Periodicamente, executa limpeza de historico antigo (`pruneHistoryOlderThanDays`).
6. Em erro de comunicacao:
   - aplica retry com backoff;
   - se timeout prolongado, solicita desconexao da serial compartilhada;
   - se erro de max retries prolongado, para o monitor do PLC afetado.

## Fluxo da aba "Leitura DM"

1. Usuario seleciona node e endereco inicial.
2. Scanner le ate:
   - encontrar `#FFFF`; ou
   - atingir limite de palavras.
3. Blocos validos sao identificados por marcador e timestamp.
4. Valores de producao sao mapeados para tags configuradas no banco.
5. Dados sao persistidos em lote (historico ou current-only conforme `persist_history`).
6. Enderecos lidos sao limpos com escrita `#0000`.

Arquivo auxiliar:

- `production_tags.txt`: lista de nomes de tags usadas no parser de producao manual.
  - se nao existir, e criado com valores padrao.

## Dependencias e requisitos

- Java 21.
- Maven 3.9+ (recomendado).
- Modulos `core` e `collector` compilaveis no mesmo multi-modulo.
- Banco PostgreSQL configurado via `DbConfig`.
- Tabelas esperadas nas queries:
  - `public.device`
  - `public.machine`
  - `public.tag`
  - `public.memory`

## Build e execucao

Na raiz do projeto:

```bash
mvn -pl collector -am clean package
```

Executar interface principal multi-PLC:

```bash
mvn -pl collector exec:java -Dexec.mainClass=org.omron.collector.CollectorMultPlcAplication
```

Executar monitor sem UI:

```bash
mvn -pl collector exec:java -Dexec.mainClass=org.omron.collector.DmMonitorApplication
```

## Arquivos gerados em runtime

- `collector-mult-plc.log`: log principal da aplicacao.
- `collector-gui.log`: log da versao `CollectorGuiApplication`.
- `production_tags.txt`: configuracao de tags de producao da leitura manual.

## Observacoes de manutencao

- O nome da classe principal atual esta com typo historico: `CollectorMultPlcAplication` (sem segundo "p").
- A descricao da regra de leitura manual na UI menciona 100 palavras, enquanto o limite tecnico no codigo (`MANUAL_DM_MAX_ADDRESSES`) esta em 1000.
- O lock `SharedSerial.ioLock` e obrigatorio para garantir seguranca de acesso concorrente ao handler serial.
- O modulo depende fortemente da consistencia entre `tag`, `memory` e endereco DM no banco.
