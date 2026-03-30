package org.omron.collector.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;

/**
 * Gerencia a execução de varredura manual de endereços de memória DM em PLCs
 * Omron.
 * 
 * <p>
 * Esta classe é responsável por:
 * <ul>
 * <li>Executar leitura sequencial de endereços DM com validação</li>
 * <li>Processar e validar valores lidos utilizando
 * {@link ManualDmDataProcessor}</li>
 * <li>Persistir dados de produção no banco de dados através de
 * {@link DatabaseManager}</li>
 * <li>Limpar valores após leitura escrevendo zeros nos endereços lidos</li>
 * <li>Detectar blocos de produção e extrair timestamps</li>
 * <li>Gerenciar callbacks de resultado e status em tempo real</li>
 * </ul>
 * 
 * <p>
 * A classe opera em thread separada ({@link #scanThread}) para não bloquear a
 * aplicação.
 * Utiliza {@link SerialCommunicationManager} para comunicação serial
 * sincronizada com o PLC.
 * 
 * <p>
 * O processo de varredura pode ser controlado via {@link #startScan(int, int)}
 * e
 * {@link #stopScan()}, com status reportado através de callbacks.
 * 
 * @see ManualDmDataProcessor
 * @see DatabaseManager
 * @see SerialCommunicationManager
 */
public class ManualDmScanManager {

    private static final int MANUAL_DM_MAX_ADDRESSES = 1000;
    private static final int MANUAL_READ_DELAY_MS = 200;
    private static final int DM_TERMINATOR_VALUE = 0xFFFF;
    private static final Path PRODUCTION_TAGS_FILE = Paths.get("production_tags.txt");

    private final DatabaseManager dbManager;
    private final LoggingService logger;
    private final SerialCommunicationManager serialManager;

    private Thread scanThread;
    private Consumer<String> resultCallback;
    private Consumer<String> statusCallback;
    private boolean isRunning;

    private List<String> productionTagNames;

    /**
     * Construtor que inicializa o gerenciador de varredura manual DM.
     * 
     * <p>
     * Carrega a lista de TAGs de produção a partir do arquivo
     * {@code production_tags.txt}
     * ou utiliza valores padrão se o arquivo não existir.
     * 
     * @param dbManager     gerenciador de banco de dados para persistência
     * @param logger        serviço de logging para mensagens de erro e informação
     * @param serialManager gerenciador de comunicação serial sincronizada com o PLC
     */
    public ManualDmScanManager(DatabaseManager dbManager, LoggingService logger,
            SerialCommunicationManager serialManager) {
        this.dbManager = dbManager;
        this.logger = logger;
        this.serialManager = serialManager;
        this.productionTagNames = loadProductionTagNames();
    }

    /**
     * Verifica se uma varredura manual está em execução.
     * 
     * @return {@code true} se o scanner está em execução, {@code false} caso
     *         contrário
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Define o callback para receber mensagens de resultado e log da varredura.
     * 
     * <p>
     * O callback será invocado com mensagens descritivas sobre endereços lidos,
     * valores encontrados, blocos de produção detectados e erros.
     * 
     * @param callback função a ser chamada com mensagens de resultado, ou
     *                 {@code null} para desabilitar
     */
    public void setResultCallback(Consumer<String> callback) {
        this.resultCallback = callback;
    }

    /**
     * Define o callback para receber atualizações de status da varredura.
     * 
     * <p>
     * O callback será invocado com os seguintes status:
     * <ul>
     * <li>"LENDO" - varredura em progresso</li>
     * <li>"CONCLUIDO" - varredura finalizada com sucesso</li>
     * <li>"INTERROMPIDO" - varredura parada pelo usuário</li>
     * <li>"ERRO" - varredura finalizada com erro</li>
     * <li>"PARADO" - varredura parada manualmente</li>
     * <li>"DESCONECTADO" - conexão serial não disponível</li>
     * </ul>
     * 
     * @param callback função a ser chamada com atualizações de status, ou
     *                 {@code null} para desabilitar
     */
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    /**
     * Define a lista de nomes de TAGs de produção a serem utilizadas na varredura.
     * 
     * <p>
     * A lista é persistida no arquivo {@code production_tags.txt} para uso futuro.
     * 
     * @param names lista de nomes de TAGs de produção
     */
    public void setProductionTagNames(List<String> names) {
        this.productionTagNames = new ArrayList<>(names);
        saveProductionTagNames();
    }

    /**
     * Retorna uma cópia da lista de nomes de TAGs de produção configuradas.
     * 
     * @return lista imutável (cópia) dos nomes de TAGs de produção
     */
    public List<String> getProductionTagNames() {
        return new ArrayList<>(productionTagNames);
    }

    /**
     * Adiciona um novo nome de TAG de produção à lista, se ainda não estiver
     * presente.
     * 
     * <p>
     * A lista atualizada é persistida no arquivo {@code production_tags.txt}.
     * 
     * @param tagName nome da TAG de produção a adicionar
     */
    public void addProductionTagName(String tagName) {
        if (!productionTagNames.contains(tagName)) {
            productionTagNames.add(tagName);
            saveProductionTagNames();
        }
    }

    /**
     * Inicia uma varredura manual de endereços DM em uma thread separada.
     * 
     * <p>
     * A varredura lê sequencialmente endereços DM começando de
     * {@code startAddress},
     * validando cada leitura e compara com um marcador terminador ({@code 0xFFFF}).
     * Após conclusão, persiste os dados no banco de dados e limpa os endereços
     * lidos.
     * 
     * <p>
     * A varredura pode ser monitorada através dos callbacks de resultado e status
     * definidos via {@link #setResultCallback(Consumer)} e
     * {@link #setStatusCallback(Consumer)}.
     * 
     * @param nodeId       identificador do nó do PLC (usado para lookup de
     *                     configuração)
     * @param startAddress endereço DM inicial para começar a leitura
     * @throws IllegalStateException se uma varredura já está em execução ou a
     *                               conexão serial não está ativa
     * 
     * @see #stopScan()
     * @see #isRunning()
     */
    public void startScan(int nodeId, int startAddress) throws IllegalStateException {
        if (isRunning) {
            throw new IllegalStateException("Scanner DM manual ja esta em execucao.");
        }

        if (!serialManager.isConnected()) {
            updateStatus("DESCONECTADO");
            log("Conecte a serial compartilhada antes de executar a leitura DM manual.");
            throw new IllegalStateException("Serial nao conectada");
        }

        isRunning = true;
        updateStatus("LENDO");
        appendResult("Inicio leitura DM: node=" + nodeId + ", endereco inicial=" + startAddress + ".");

        scanThread = new Thread(() -> executeManualDmScan(nodeId, startAddress), "collector-manual-dm-scan");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    /**
     * Para uma varredura manual em execução de forma segura.
     * 
     * <p>
     * Aguarda até 1.5 segundos para a thread de varredura finalizar antes de
     * prosseguir.
     * Atualiza o status para "PARADO" e libera recursos associados.
     * 
     * <p>
     * Este método é seguro para ser chamado mesmo que nenhuma varredura esteja em
     * execução.
     */
    public void stopScan() {
        if (scanThread != null && scanThread.isAlive()) {
            scanThread.interrupt();
            try {
                scanThread.join(1500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        scanThread = null;
        isRunning = false;
        updateStatus("PARADO");
    }

    /**
     * Executa a varredura manual de DM em thread separada.
     * 
     * <p>
     * Processo:
     * <ol>
     * <li>Cria um device virtual com ID único</li>
     * <li>Lê sequencialmente até {code MANUAL_DM_MAX_ADDRESSES} endereços</li>
     * <li>Pausa {code MANUAL_READ_DELAY_MS} ms entre leituras</li>
     * <li>Para na leitura se encontrar o terminador {code DM_TERMINATOR_VALUE} ou
     * atingir o máximo</li>
     * <li>Persiste valores em blocos de produção identificados</li>
     * <li>Escreve zero nos endereços lidos para limpeza</li>
     * <li>Notifica callbacks com resultado ou erro</li>
     * </ol>
     * 
     * @param nodeId       identificador do nó do PLC
     * @param startAddress endereço DM inicial
     */
    private void executeManualDmScan(int nodeId, int startAddress) {
        List<Integer> addressesRead = new ArrayList<>();
        Map<Integer, Integer> valuesByAddress = new HashMap<>();
        boolean terminatorFound = false;

        try {
            IDevice plc = new DeviceImp(nodeId, "MANUAL_DM_" + nodeId, "Manual DM", "Leitura manual DM");
            DeviceRegisterImp.getInstance().addDevice(plc);

            for (int i = 0; i < MANUAL_DM_MAX_ADDRESSES; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Scanner interrompido");
                }

                int address = startAddress + i;
                MemoryVariable variable = new MemoryVariable("DM_SCAN_" + address, "DM", address, 1);
                AreaReadDM read = new AreaReadDM(plc, variable);

                synchronized (serialManager.getSharedSerial().getIoLock()) {
                    if (!serialManager.isConnected()) {
                        throw new IllegalStateException("Serial compartilhada desconectada durante leitura manual.");
                    }
                    serialManager.getSharedSerial().getHandler().send(read);
                }

                int value = ManualDmDataProcessor.parseSingleWord(read.getReply());
                addressesRead.add(Integer.valueOf(address));
                valuesByAddress.put(Integer.valueOf(address), Integer.valueOf(value));
                appendResult("DM " + address + " = #" + ManualDmDataProcessor.toHexWord(value));

                if (value == DM_TERMINATOR_VALUE) {
                    terminatorFound = true;
                    appendResult("Encontrado terminador #FFFF em DM " + address + ".");
                    break;
                }

                Thread.sleep(MANUAL_READ_DELAY_MS);
            }

            if (!terminatorFound) {
                appendResult("Terminador #FFFF nao encontrado em " + MANUAL_DM_MAX_ADDRESSES + " palavras.");
            }

            if (!addressesRead.isEmpty()) {
                persistProductionValues(nodeId, addressesRead, valuesByAddress);
                writeZeroToReadAddresses(plc, addressesRead);
                appendResult("#0000 escrito em " + addressesRead.size() + " endereco(s) lido(s).");
            }

            updateStatus("CONCLUIDO");
            log("Scanner DM manual concluido. Node " + nodeId + ", inicio DM " + startAddress
                    + ", total lido " + addressesRead.size() + ".");

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            updateStatus("INTERROMPIDO");
            appendResult("Scanner DM manual interrompido.");
        } catch (Exception ex) {
            updateStatus("ERRO");
            logger.logError("Falha no scanner DM manual", ex);
            appendResult("Falha: " + describeError(ex));
        } finally {
            isRunning = false;
        }
    }

    /**
     * Persiste os valores lidos de DM no banco de dados como valores de produção.
     * 
     * <p>
     * Processo:
     * <ol>
     * <li>Busca a configuração do device pelo nodeId</li>
     * <li>Carrega as vinculações de TAGs de produção do banco</li>
     * <li>Detecta blocos de produção usando {@link ManualDmDataProcessor}</li>
     * <li>Extrai timestamp de cada bloco</li>
     * <li>Mapeia valores lidos para TAGs de produção configuradas</li>
     * <li>Separa em batches: histórico e "current-only" conforme configuração</li>
     * <li>Persiste cada batch no banco de dados</li>
     * </ol>
     * 
     * <p>
     * Gera mensagens informativas via {@link #appendResult(String)} durante o
     * processo.
     * 
     * @param nodeId          identificador do nó do PLC
     * @param addressesRead   lista de endereços lidos em ordem
     * @param valuesByAddress mapa de endereço para valor lido
     * 
     * @see ManualDmDataProcessor#getManualDmGroupSize()
     * @see ManualDmDataProcessor#matchesManualDateMarker(int, int)
     * @see ManualDmDataProcessor#tryBuildManualTimestamp(List, int)
     */
    private void persistProductionValues(int nodeId, List<Integer> addressesRead,
            Map<Integer, Integer> valuesByAddress) {
        PlcConfigurationLoader.PlcConfiguration cfg = null;
        // Busca a configuração do device pelo node ID
        List<PlcConfigurationLoader.PlcConfiguration> configs = new PlcConfigurationLoader(dbManager)
                .loadAllConfigurations();

        for (PlcConfigurationLoader.PlcConfiguration config : configs) {
            if (config.nodeId == nodeId) {
                cfg = config;
                break;
            }
        }

        if (cfg == null) {
            appendResult("Nao foi possivel identificar o device do node " + nodeId
                    + " para salvar os valores da leitura manual.");
            log("Leitura DM manual: node " + nodeId + " sem device configurado para persistencia.");
            return;
        }

        Map<String, DatabaseManager.ManualProductionTagBinding> tagBindings = dbManager
                .loadManualProductionTagBindings(cfg.mnemonic, productionTagNames.toArray(new String[0]));

        if (tagBindings.isEmpty()) {
            appendResult("Nenhuma TAG de producao encontrada no banco para o device " + cfg.mnemonic + ".");
            log("Leitura DM manual: tags de producao nao encontradas para device " + cfg.mnemonic + ".");
            return;
        }

        List<Integer> valuesInOrder = new ArrayList<>(addressesRead.size());
        for (Integer rawAddress : addressesRead) {
            Integer value = valuesByAddress.get(rawAddress);
            if (value != null) {
                valuesInOrder.add(value);
            }
        }

        if (valuesInOrder.size() < ManualDmDataProcessor.getManualDmGroupSize()) {
            appendResult("Leitura insuficiente para bloco de producao (" + valuesInOrder.size() + " palavra(s)).");
            return;
        }

        DeviceInfo deviceInfo = new DeviceInfo(cfg.mnemonic, cfg.title, cfg.description);
        List<MemoryValue> historyBatch = new ArrayList<>();
        List<MemoryValue> currentOnlyBatch = new ArrayList<>();

        int parsedBlocks = 0;
        int offset = 0;
        while (offset + ManualDmDataProcessor.getManualDmGroupSize() - 1 < valuesInOrder.size()) {
            int markerAddress = addressesRead.get(offset).intValue();
            int markerValue = valuesInOrder.get(offset).intValue();

            if (!ManualDmDataProcessor.matchesManualDateMarker(markerAddress, markerValue)
                    && !ManualDmDataProcessor.matchesManualRelativeMarker(offset, markerValue)) {
                offset++;
                continue;
            }

            try {
                LocalDateTime timestamp = ManualDmDataProcessor.tryBuildManualTimestamp(valuesInOrder, offset);
                if (timestamp == null) {
                    offset++;
                    continue;
                }

                appendResult("Bloco de producao detectado (offset " + offset + "): timestamp "
                        + ManualDmDataProcessor.formatTimestamp(timestamp));

                for (int i = 0; i < productionTagNames.size(); i++) {
                    int valueIndex = offset + ManualDmDataProcessor.getManualDmTagValuesOffset() + i;
                    if (valueIndex >= valuesInOrder.size()) {
                        break;
                    }

                    String tagName = productionTagNames.get(i);
                    int value = valuesInOrder.get(valueIndex).intValue() & 0xFFFF;

                    DatabaseManager.ManualProductionTagBinding binding = tagBindings.get(tagName);
                    if (binding == null) {
                        appendResult("TAG " + tagName + " nao encontrada no banco para device " + cfg.mnemonic
                                + ". Valor lido=" + value + " ignorado.");
                        continue;
                    }

                    MemoryValue dbValue = new MemoryValue(binding.memoryName, value, timestamp);
                    if (binding.persistHistory) {
                        historyBatch.add(dbValue);
                    } else {
                        currentOnlyBatch.add(dbValue);
                    }

                    appendResult("TAG " + tagName + " -> DM " + binding.address + " = " + value
                            + " @ " + ManualDmDataProcessor.formatTimestamp(timestamp));
                }

                parsedBlocks++;
                offset += ManualDmDataProcessor.getManualDmGroupSize();
            } catch (Exception ex) {
                offset++;
            }
        }

        if (parsedBlocks == 0) {
            appendResult("Nenhum bloco de producao valido encontrado pela regra de indices manuais.");
            log("Leitura DM manual: nenhum bloco de producao valido encontrado.");
            return;
        }

        dbManager.initialize();
        dbManager.saveMemoryValuesBatch(deviceInfo, historyBatch);
        dbManager.saveMemoryValuesCurrentOnly(deviceInfo, currentOnlyBatch);

        int totalSaved = historyBatch.size() + currentOnlyBatch.size();
        appendResult("Persistencia concluida: " + parsedBlocks + " bloco(s), " + totalSaved
                + " valor(es) salvo(s) em TAG/memory.");
        log("Leitura DM manual salva em banco para device " + cfg.mnemonic + ": blocos=" + parsedBlocks
                + ", historico=" + historyBatch.size() + ", current-only=" + currentOnlyBatch.size() + ".");
    }

    /**
     * Escreve valor zero em todos os endereços DM que foram lidos.
     * 
     * <p>
     * Realiza a limpeza dos endereços lidos no PLC após persistência bem-sucedida.
     * Utiliza sincronização via
     * {@link SerialCommunicationManager#getSharedSerial()}
     * para garantir exclusão durante comunicação.
     * 
     * @param plc       dispositivo virtual criado para a varredura
     * @param addresses lista de endereços que foram lidos
     * @throws Exception se a comunicação serial falhar ou desconectar
     */
    private void writeZeroToReadAddresses(IDevice plc, List<Integer> addresses) throws Exception {
        synchronized (serialManager.getSharedSerial().getIoLock()) {
            if (!serialManager.isConnected()) {
                throw new IllegalStateException("Serial compartilhada desconectada antes da escrita de limpeza.");
            }
            for (Integer rawAddress : addresses) {
                int address = rawAddress.intValue();
                AreaWriteDM write = new AreaWriteDM(plc, address, new int[] { 0 }, MemoryWrite.HEX);
                serialManager.getSharedSerial().getHandler().send(write);
            }
        }
    }

    /**
     * Envia uma mensagem de resultado para o callback configurado.
     * 
     * @param text mensagem a enviar
     * @see #setResultCallback(Consumer)
     */
    private void appendResult(String text) {
        if (resultCallback != null) {
            resultCallback.accept(text);
        }
    }

    /**
     * Atualiza o status da varredura através do callback configurado.
     * 
     * @param status novo status (ex: "LENDO", "CONCLUIDO", "ERRO")
     * @see #setStatusCallback(Consumer)
     */
    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }

    /**
     * Registra uma mensagem de log via {@link LoggingService}.
     * 
     * @param message mensagem a registrar
     */
    private void log(String message) {
        if (logger != null) {
            logger.log(message);
        }
    }

    /**
     * Carrega a lista de nomes de TAGs de produção do arquivo
     * {@code production_tags.txt}.
     * 
     * <p>
     * Se o arquivo não existir, retorna a lista padrão e cria o arquivo.
     * Se o arquivo existir mas estar vazio, retorna a lista padrão sem
     * sobrescrever.
     * Ignora linhas em branco e comentários (que começam com #).
     * 
     * @return lista de nomes de TAGs de produção
     */
    private List<String> loadProductionTagNames() {
        List<String> defaultTags = List.of(
                "PRODUCAO_PH29",
                "PRODUCAO_PH30",
                "PRODUCAO_PH31",
                "PRODUCAO_SEC25",
                "PRODUCAO_SEC26",
                "PRODUCAO_SEC33");

        try {
            if (Files.exists(PRODUCTION_TAGS_FILE)) {
                List<String> loadedTags = Files.readAllLines(PRODUCTION_TAGS_FILE)
                        .stream()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList());
                if (!loadedTags.isEmpty()) {
                    return loadedTags;
                }
            }
        } catch (IOException e) {
            log("Erro ao carregar production_tags.txt: " + e.getMessage());
        }

        // Fallback para valores padrão
        try {
            Files.write(PRODUCTION_TAGS_FILE, defaultTags, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log("Erro ao criar arquivo padrão production_tags.txt: " + e.getMessage());
        }

        return new ArrayList<>(defaultTags);
    }

    /**
     * Persiste a lista de nomes de TAGs de produção no arquivo
     * {@code production_tags.txt}.
     * 
     * <p>
     * Sobrescreve completamente o arquivo existente com a lista atual.
     * Em caso de erro de I/O, apenas registra a falha sem lançar exceção.
     */
    private void saveProductionTagNames() {
        try {
            Files.write(PRODUCTION_TAGS_FILE, productionTagNames, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log("Erro ao salvar production_tags.txt: " + e.getMessage());
        }
    }

    /**
     * Retorna uma descrição legível de uma exceção ou erro.
     * 
     * <p>
     * Extrai o nome simples da classe de exceção e sua mensagem.
     * Se a mensagem estiver vazia, retorna apenas o nome da classe.
     * 
     * @param error exceção ou erro a descrever
     * @return descrição formatada (ex: "IOException - file not found")
     */
    private static String describeError(Throwable error) {
        if (error == null) {
            return "erro desconhecido";
        }
        String type = error.getClass().getSimpleName();
        String text = error.getMessage();
        if (text == null || text.trim().isEmpty()) {
            return type;
        }
        return type + " - " + text;
    }
}
