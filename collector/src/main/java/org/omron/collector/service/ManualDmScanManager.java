package org.omron.collector.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.omron.collector.util.SharedSerial;

/**
 * Gerencia execução de leitura manual de DM.
 * Responsabilidades:
 * - Executar leitura sequencial de endereços DM
 * - Processar e validar valores lidos
 * - Persistir dados de produção
 * - Limpar valores após leitura
 */
public class ManualDmScanManager {

    private static final int MANUAL_DM_MAX_ADDRESSES = 100;
    private static final int MANUAL_READ_DELAY_MS = 200;
    private static final int DM_TERMINATOR_VALUE = 0xFFFF;

    private final DatabaseManager dbManager;
    private final LoggingService logger;
    private final SerialCommunicationManager serialManager;

    private Thread scanThread;
    private Consumer<String> resultCallback;
    private Consumer<String> statusCallback;
    private boolean isRunning;

    private String[] productionTagNames = new String[] {
            "PRODUCAO_PH29",
            "PRODUCAO_PH30",
            "PRODUCAO_PH31",
            "PRODUCAO_SEC25",
            "PRODUCAO_SEC26",
            "PRODUCAO_SEC33"
    };

    public ManualDmScanManager(DatabaseManager dbManager, LoggingService logger,
            SerialCommunicationManager serialManager) {
        this.dbManager = dbManager;
        this.logger = logger;
        this.serialManager = serialManager;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setResultCallback(Consumer<String> callback) {
        this.resultCallback = callback;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setProductionTagNames(String[] names) {
        this.productionTagNames = names;
    }

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
                .loadManualProductionTagBindings(cfg.mnemonic, productionTagNames);

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

                for (int i = 0; i < productionTagNames.length; i++) {
                    int valueIndex = offset + ManualDmDataProcessor.getManualDmTagValuesOffset() + i;
                    if (valueIndex >= valuesInOrder.size()) {
                        break;
                    }

                    String tagName = productionTagNames[i];
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

    private void appendResult(String text) {
        if (resultCallback != null) {
            resultCallback.accept(text);
        }
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }

    private void log(String message) {
        if (logger != null) {
            logger.log(message);
        }
    }

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
