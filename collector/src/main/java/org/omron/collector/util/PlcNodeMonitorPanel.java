package org.omron.collector.util;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Optional;
import org.ctrl.db.service.QualidadeService;
import org.ctrl.db.repository.DefeitoRepository;
import org.ctrl.db.model.Qualidade;
import org.ctrl.db.model.Defeito;
import java.util.HashMap;
import org.omron.collector.service.DatabaseManager.QualityGroup;
import org.omron.collector.service.DatabaseManager.TagData;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.RrValueService;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadRR;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadTC;

public class PlcNodeMonitorPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int INTER_TAG_DELAY_MS = 1000;
    private static final int ERROR_RETRY_DELAY_MS = 1500;
    private static final int MAX_ERROR_RETRY_DELAY_MS = 15000;
    private static final int HISTORY_RETENTION_DAYS = 14;
    private static final int HISTORY_PRUNE_INTERVAL_CYCLES = 500;
    private static final long NO_RESPONSE_DISCONNECT_MS = 3000L;
    private static final long MAX_RETRIES_STOP_MONITOR_MS = 3000L;

    private final int nodeIndex;
    private final int configuredNodeId;
    private final int dbDeviceId;
    private final String plcTitle;
    private final String plcMnemonic;
    private final String plcDescription;
    private final List<MonitoredTag> monitoredTags = new ArrayList<>();
    private final Supplier<List<MonitoredTag>> monitoredTagsSupplier;
    private final Object comLock;
    private final Supplier<Boolean> sharedConnectedSupplier;
    private final Supplier<SerialPortHandlerImp> sharedComHandlerSupplier;
    private final Runnable ensureDbAction;
    private final Supplier<DmValueService> dmValueServiceSupplier;
    private final Supplier<RrValueService> rrValueServiceSupplier;
    private final Supplier<QualidadeService> qualidadeServiceSupplier;
    private final Supplier<DefeitoRepository> defeitoRepositorySupplier;
    private final Supplier<Map<Integer, QualityGroup>> qualityGroupsSupplier;
    private final Consumer<String> globalLogger;
    private final Runnable sharedDisconnectAction;
    private final JPanel panel;

    private JTextField nodeField;
    private JTextField pollMsField;
    private JLabel commStatusLabel;
    private JLabel monitorStatusLabel;

    private volatile boolean monitoring;
    private Thread monitorThread;
    private IDevice plc;
    private DeviceInfo deviceInfo;

    public PlcNodeMonitorPanel(int nodeIndex,
            String plcTitle,
            String plcMnemonic,
            String plcDescription,
            int configuredNodeId,
            int dbDeviceId,
            List<MonitoredTag> monitoredTags,
            Supplier<List<MonitoredTag>> monitoredTagsSupplier,
            Object comLock,
            Supplier<Boolean> sharedConnectedSupplier,
            Supplier<SerialPortHandlerImp> sharedComHandlerSupplier,
            Runnable ensureDbAction,
            Supplier<DmValueService> dmValueServiceSupplier,
            Supplier<RrValueService> rrValueServiceSupplier,
            Supplier<QualidadeService> qualidadeServiceSupplier,
            Supplier<DefeitoRepository> defeitoRepositorySupplier,
            Supplier<Map<Integer, QualityGroup>> qualityGroupsSupplier,
            Consumer<String> globalLogger,
            Runnable sharedDisconnectAction) {
        this.nodeIndex = nodeIndex;
        this.configuredNodeId = Math.max(0, configuredNodeId);
        this.dbDeviceId = dbDeviceId;
        this.plcTitle = plcTitle;
        this.plcMnemonic = plcMnemonic;
        this.plcDescription = plcDescription == null ? "" : plcDescription;
        if (monitoredTags != null) {
            this.monitoredTags.addAll(monitoredTags);
        }
        this.monitoredTagsSupplier = monitoredTagsSupplier;
        this.comLock = comLock;
        this.sharedConnectedSupplier = sharedConnectedSupplier;
        this.sharedComHandlerSupplier = sharedComHandlerSupplier;
        this.ensureDbAction = ensureDbAction;
        this.dmValueServiceSupplier = dmValueServiceSupplier;
        this.rrValueServiceSupplier = rrValueServiceSupplier;
        this.qualidadeServiceSupplier = qualidadeServiceSupplier;
        this.defeitoRepositorySupplier = defeitoRepositorySupplier;
        this.qualityGroupsSupplier = qualityGroupsSupplier;
        this.globalLogger = globalLogger;
        this.sharedDisconnectAction = sharedDisconnectAction;
        this.panel = buildNodePanel(this.configuredNodeId);
    }

    public JPanel getPanel() {
        return panel;
    }

    void startMonitor() {
        if (monitoring) {
            logPrefix("Monitor ja esta em execucao.");
            return;
        }
        if (!isSharedConnected()) {
            logPrefix("Conecte a serial compartilhada antes de iniciar o monitor.");
            return;
        }

        ensureDbAction.run();
        reloadTagsFromDb();
        ensureDevice();

        final int pollMs;
        try {
            pollMs = Integer.parseInt(pollMsField.getText().trim());
            if (pollMs < 100) {
                throw new IllegalArgumentException("Poll deve ser >= 100ms.");
            }
        } catch (Exception ex) {
            logPrefix("Parametros invalidos: " + ex.getMessage());
            return;
        }

        monitoring = true;
        monitorThread = new Thread(() -> runMonitorLoop(pollMs), "collector-node-" + nodeIndex + "-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopMonitor() {
        monitoring = false;
        Thread localThread = monitorThread;
        if (localThread != null) {
            localThread.interrupt();
            if (localThread != Thread.currentThread()) {
                try {
                    localThread.join(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    monitorThread = null;
                }
            } else {
                monitorThread = null;
            }
        }
        setMonitorStatus("PARADO");
    }

    public void setCommStatus(String text) {
        runOnEdt(() -> commStatusLabel.setText(text));
    }

    private JPanel buildNodePanel(int defaultNodeId) {
        JPanel nodePanel = new JPanel(new GridBagLayout());
        nodePanel.setBorder(BorderFactory.createTitledBorder(plcTitle));
        nodePanel.setPreferredSize(new Dimension(0, 88));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        nodeField = new JTextField(Integer.toString(defaultNodeId));
        nodeField.setEditable(true);
        pollMsField = new JTextField("2000");
        nodeField.setColumns(6);
        pollMsField.setColumns(6);

        int row = 0;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = row;
        nodePanel.add(new JLabel("Node ID"), c);
        c.weightx = 0.2;
        c.gridx = 1;
        nodePanel.add(nodeField, c);

        c.weightx = 0;
        c.gridx = 2;
        nodePanel.add(new JLabel("Poll (ms)"), c);
        c.weightx = 0.2;
        c.gridx = 3;
        nodePanel.add(pollMsField, c);

        JButton startButton = new JButton("Iniciar monitor");
        startButton.addActionListener(e -> startMonitor());
        c.weightx = 0.3;
        c.gridx = 4;
        nodePanel.add(startButton, c);

        JButton stopButton = new JButton("Parar monitor");
        stopButton.addActionListener(e -> stopMonitor());
        c.weightx = 0.3;
        c.gridx = 5;
        nodePanel.add(stopButton, c);

        row++;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = row;
        nodePanel.add(new JLabel("Comunicacao"), c);
        commStatusLabel = new JLabel("DESCONECTADA");
        c.weightx = 0.35;
        c.gridx = 1;
        c.gridwidth = 2;
        nodePanel.add(commStatusLabel, c);
        c.gridwidth = 1;

        c.weightx = 0;
        c.gridx = 2;
        nodePanel.add(new JLabel("Monitor"), c);
        monitorStatusLabel = new JLabel("PARADO");
        c.weightx = 0.65;
        c.gridx = 3;
        c.gridwidth = 3;
        nodePanel.add(monitorStatusLabel, c);
        c.gridwidth = 1;

        return nodePanel;
    }

    private void runMonitorLoop(int pollMs) {
        List<MonitoredTag> activeTags = snapshotMonitoredTags();
        setMonitorStatus("RODANDO");
        logPrefix("Monitor iniciado para " + activeTags.size() + " TAGs.");

        Map<String, int[]> lastValues = new LinkedHashMap<>();
        int cycleCount = 0;
        int consecutiveErrors = 0;
        long noResponseStartMs = 0L;
        boolean disconnectRequested = false;
        long maxRetriesErrorStartMs = 0L;
        boolean monitorStopRequested = false;

        long lastQualityUpdateMs = 0;
        Map<Integer, QualityGroup> qualityGroups = new HashMap<>();
        try {
            qualityGroups = qualityGroupsSupplier.get();
            if (qualityGroups != null && !qualityGroups.isEmpty()) {
                logPrefix("Grupos de qualidade carregados: " + qualityGroups.size());
            }
        } catch (Exception e) {
            logPrefix("Erro ao carregar grupos de qualidade: " + e.getMessage());
        }

        while (monitoring) {
            try {
                if (!isSharedConnected()) {
                    throw new IllegalStateException("Serial compartilhada desconectada.");
                }

                for (MonitoredTag tag : activeTags) {
                    if (!monitoring) {
                        break;
                    }

                    String area = tag.getMemoryArea();
                    boolean isRrBit = "RR".equalsIgnoreCase(area) && tag.getBit() >= 0;

                    MemoryVariable memory = new MemoryVariable(
                            tag.getName(),
                            area,
                            tag.getAddress(),
                            isRrBit ? 1 : tag.getLengthWords());

                    org.ctrl.vend.omron.toolbus.memory.MemoryRead readCmd;
                    if ("RR".equalsIgnoreCase(area)) {
                        readCmd = new AreaReadRR(plc, tag.getAddress(), isRrBit ? 1 : tag.getLengthWords());
                    } else if ("TC".equalsIgnoreCase(area) || "TIMER".equalsIgnoreCase(area)
                            || "COUNTER".equalsIgnoreCase(area)) {
                        readCmd = new AreaReadTC(plc, tag.getAddress(), tag.getLengthWords());
                    } else {
                        readCmd = new AreaReadDM(plc, memory);
                    }

                    synchronized (comLock) {
                        if (!isSharedConnected()) {
                            throw new IllegalStateException("Serial compartilhada desconectada.");
                        }
                        sharedComHandlerSupplier.get().send(readCmd);
                    }

                    int[] values = parseReply(readCmd.getReply(), isRrBit ? 1 : tag.getLengthWords());
                    if (values == null) {
                        logPrefix("Leitura invalida para TAG " + tag.getName() + ".");
                    } else {
                        int[] previous = lastValues.get(tag.getName());
                        if (hasChanged(previous, values)) {
                            if (isRrBit) {
                                // Extrai o bit do word lido
                                int bitValue = (values[0] >> tag.getBit()) & 0x01;
                                rrValueServiceSupplier.get().saveValue(deviceInfo, tag.getAddress(), tag.getBit(),
                                        bitValue != 0);
                                logPrefix("Alteracao salva (RR BIT): TAG " + tag.getName() + " (" + area + " "
                                        + tag.getAddress() + "." + tag.getBit() + ") = " + bitValue);
                            } else {
                                if (tag.isPersistHistory()) {
                                    dmValueServiceSupplier.get().saveRange(deviceInfo, tag.getAddress(), values);
                                } else {
                                    dmValueServiceSupplier.get().saveRangeCurrentOnly(deviceInfo, tag.getAddress(),
                                            values);
                                }
                                logPrefix(
                                        "Alteracao salva (" + (tag.isPersistHistory() ? "historico+current" : "current")
                                                + "): TAG " + tag.getName() + " (" + area + " " + tag.getAddress()
                                                + ".." + (tag.getAddress() + tag.getLengthWords() - 1) + ") = "
                                                + formatWords(values) + ".");
                            }
                            lastValues.put(tag.getName(), copyWords(values));

                        }

                        // Gatilho de qualidade: salva apenas na borda de subida (0 -> 1).
                        if (isQualityTriggerRisingEdge(previous, values) && qualityGroups != null) {
                            for (QualityGroup qg : qualityGroups.values()) {
                                if (qg.trigger != null && qg.trigger.name.equals(tag.getName())) {
                                    processQualityTrigger(qg);
                                }
                            }
                        }
                    }

                    Thread.sleep(INTER_TAG_DELAY_MS);
                }

                // Lógica de 5 minutos para Qualidade da Máquina
                long now = System.currentTimeMillis();
                if (now - lastQualityUpdateMs > 5 * 60 * 1000) {
                    if (qualityGroups != null) {
                        for (QualityGroup qg : qualityGroups.values()) {
                            updateMachineQuality(qg);
                        }
                    }
                    lastQualityUpdateMs = now;
                }

                setCommStatus("CONECTADA - ciclo " + LocalDateTime.now().format(TIME_FMT));
                cycleCount++;
                consecutiveErrors = 0;
                noResponseStartMs = 0L;
                disconnectRequested = false;
                maxRetriesErrorStartMs = 0L;
                monitorStopRequested = false;
                if (cycleCount % HISTORY_PRUNE_INTERVAL_CYCLES == 0) {
                    int deletedRows = dmValueServiceSupplier.get().pruneHistoryOlderThanDays(HISTORY_RETENTION_DAYS);
                    if (deletedRows > 0) {
                        logPrefix("Limpeza de historico: " + deletedRows + " registros removidos de memory_value.");
                    }
                }
                Thread.sleep(pollMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                consecutiveErrors++;
                setCommStatus("ERRO DE COMUNICACAO");
                logPrefix("Erro no monitoramento: " + describeError(ex));
                if (isNoResponseError(ex) && isSharedConnected()) {
                    if (noResponseStartMs == 0L) {
                        noResponseStartMs = System.currentTimeMillis();
                    }
                    long elapsedMs = System.currentTimeMillis() - noResponseStartMs;
                    if (!disconnectRequested && elapsedMs >= NO_RESPONSE_DISCONNECT_MS) {
                        disconnectRequested = true;
                        setCommStatus("SEM RESPOSTA - DESCONECTANDO");
                        logPrefix("Sem resposta por " + (elapsedMs / 1000)
                                + "s. Solicitando desconexao da serial compartilhada.");
                        sharedDisconnectAction.run();
                    }
                } else {
                    noResponseStartMs = 0L;
                }
                if (isMaximumRetriesError(ex)) {
                    if (maxRetriesErrorStartMs == 0L) {
                        maxRetriesErrorStartMs = System.currentTimeMillis();
                    }
                    long elapsedMs = System.currentTimeMillis() - maxRetriesErrorStartMs;
                    if (!monitorStopRequested && elapsedMs >= MAX_RETRIES_STOP_MONITOR_MS) {
                        monitorStopRequested = true;
                        setMonitorStatus("PARADO - MAX RETRIES");
                        logPrefix("MaximumNumberOfRetriesReachedException por " + (elapsedMs / 1000)
                                + "s. Parando monitor deste PLC.");
                        stopMonitor();
                        break;
                    }
                } else {
                    maxRetriesErrorStartMs = 0L;
                }
                try {
                    int factor = 1 << Math.min(consecutiveErrors - 1, 3);
                    int backoffMs = Math.min(MAX_ERROR_RETRY_DELAY_MS, ERROR_RETRY_DELAY_MS * factor);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        setMonitorStatus("PARADO");
        if (isSharedConnected()) {
            setCommStatus("CONECTADA");
        } else {
            setCommStatus("DESCONECTADA");
        }
        logPrefix("Monitor finalizado.");
    }

    private void ensureDevice() {
        int activeNodeId = resolveActiveNodeId();
        if (plc == null || plc.getId() != activeNodeId) {
            plc = new DeviceImp(activeNodeId, plcMnemonic, plcTitle, plcDescription);
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
            deviceInfo = new DeviceInfo(dbDeviceId, plcMnemonic, plc.getName(), plc.getDescription());
        }
    }

    private int resolveActiveNodeId() {
        String raw = nodeField == null ? null : nodeField.getText();
        if (raw == null || raw.trim().isEmpty()) {
            return configuredNodeId;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed < 0 ? configuredNodeId : parsed;
        } catch (NumberFormatException ex) {
            return configuredNodeId;
        }
    }

    private void reloadTagsFromDb() {
        if (monitoredTagsSupplier == null) {
            return;
        }
        List<MonitoredTag> loaded = monitoredTagsSupplier.get();
        synchronized (monitoredTags) {
            monitoredTags.clear();
            if (loaded != null) {
                monitoredTags.addAll(loaded);
            }
        }
        logPrefix("Lista de TAG atualizada do banco: " + monitoredTags.size() + " item(ns).");
    }

    private List<MonitoredTag> snapshotMonitoredTags() {
        synchronized (monitoredTags) {
            return new ArrayList<>(monitoredTags);
        }
    }

    private void setMonitorStatus(String text) {
        runOnEdt(() -> monitorStatusLabel.setText(text));
    }

    private void logPrefix(String message) {
        globalLogger.accept("[PLC " + nodeIndex + "] " + message);
    }

    private boolean isSharedConnected() {
        return Boolean.TRUE.equals(sharedConnectedSupplier.get());
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        SwingUtilities.invokeLater(task);
    }

    private static int[] parseReply(IData reply, int length) {
        if (reply == null) {
            return null;
        }
        int[] dataBuff = reply.toHexArray();
        if (dataBuff == null) {
            return null;
        }

        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            String value = "";
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (idx < dataBuff.length) {
                    value = value + (char) dataBuff[idx];
                }
            }
            try {
                out[i] = Integer.parseInt(value.trim(), 16);
            } catch (NumberFormatException ex) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static boolean hasChanged(int[] previous, int[] current) {
        if (current == null) {
            return false;
        }
        if (previous == null || previous.length != current.length) {
            return true;
        }
        for (int i = 0; i < current.length; i++) {
            if (previous[i] != current[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQualityTriggerRisingEdge(int[] previous, int[] current) {
        if (current == null || current.length == 0) {
            return false;
        }
        if (previous == null || previous.length == 0) {
            return current[0] > 0;
        }
        return previous[0] <= 0 && current[0] > 0;
    }

    private void processQualityTrigger(QualityGroup qg) {
        logPrefix(">>> Processando Gatilho de Qualidade...");
        try {
            // 1. Ler as 13 tags complementares
            int year = readTagValue(qg.year);
            int month = readTagValue(qg.month);
            int day = readTagValue(qg.day);
            int hour = readTagValue(qg.hour);
            int min = readTagValue(qg.minute);
            int sec = readTagValue(qg.second);
            int sampling = readTagValue(qg.sampling);

            // Defeitos
            Map<Integer, Integer> defectMap = new HashMap<>();
            for (int i = 0; i < 3; i++) {
                int defNum = readTagValue(qg.defectsCode[i]);
                int defCount = readTagValue(qg.defectsTotal[i]);
                if (defNum > 0 && defCount > 0) {
                    defectMap.put(defNum, defCount);
                }
            }

            // 2. Construir Data/Hora
            // Supondo formato decimal simples (ou BCD se fosse o caso, mas aqui usamos o
            // int bruto lido)
            LocalDateTime plcTime;
            try {
                int normalizedYear = normalizePlcYear(year);
                plcTime = LocalDateTime.of(normalizedYear, month, day, hour, min, sec);
            } catch (Exception e) {
                logPrefix("Erro ao converter data do CLP: " + e.getMessage() + ". Usando hora local.");
                plcTime = LocalDateTime.now();
            }

            // 3. Criar objeto Qualidade
            Qualidade qualidade = new Qualidade();
            long qualityMachineId = (qg.trigger != null && qg.trigger.machineId > 0)
                    ? qg.trigger.machineId
                    : deviceInfo.getId().longValue();
            qualidade.setMachineId(qualityMachineId);
            qualidade.setValue(sampling);
            qualidade.setHora(plcTime);

            // 4. Mapear Defeitos para IDs do banco
            for (Map.Entry<Integer, Integer> entry : defectMap.entrySet()) {
                Optional<Defeito> d = defeitoRepositorySupplier.get().findByNumber(entry.getKey());
                if (d.isPresent()) {
                    qualidade.addDefeito(d.get().getId(), d.get().getName(), entry.getValue(), sampling);
                } else {
                    logPrefix("AVISO: Defeito numero " + entry.getKey() + " nao encontrado no banco.");
                }
            }

            // 5. Salvar
            qualidadeServiceSupplier.get().saveWithShiftDetection(qualidade);
            logPrefix(" Registro de Qualidade salvo com sucesso. Turno detectado automaticamente.");

        } catch (Exception e) {
            logPrefix("ERRO ao processar qualidade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateMachineQuality(QualityGroup qg) {
        if (qg.machineQualityCurrent == null && qg.machineQualityPersisted == null)
            return;

        logPrefix(">>> Atualizando Qualidade da Maquina (intervalo 5 min)...");
        try {
            if (qg.machineQualityCurrent != null) {
                int val = readTagValue(qg.machineQualityCurrent);
                dmValueServiceSupplier.get().saveRangeCurrentOnly(deviceInfo, qg.machineQualityCurrent.address,
                        new int[] { val });
            }
            if (qg.machineQualityPersisted != null) {
                int val = readTagValue(qg.machineQualityPersisted);
                dmValueServiceSupplier.get().saveRange(deviceInfo, qg.machineQualityPersisted.address,
                        new int[] { val });
            }
        } catch (Exception e) {
            logPrefix("Erro ao atualizar qualidade da maquina: " + e.getMessage());
        }
    }

    private int readTagValue(TagData td) throws Exception {
        if (td == null)
            return 0;
        MemoryVariable memory = new MemoryVariable(td.name, td.memoryArea, td.address, 1);
        AreaReadDM readCmd = new AreaReadDM(plc, memory);
        synchronized (comLock) {
            sharedComHandlerSupplier.get().send(readCmd);
        }
        int[] vals = parseReply(readCmd.getReply(), 1);
        return (vals != null && vals.length > 0) ? vals[0] : 0;
    }

    private static int normalizePlcYear(int rawYear) {
        if (rawYear >= 2000 && rawYear <= 2099) {
            return rawYear;
        }
        if (rawYear >= 4000 && rawYear <= 4099) {
            return rawYear - 2000;
        }
        if (rawYear >= 0 && rawYear <= 99) {
            return 2000 + rawYear;
        }
        return rawYear;
    }

    private static int[] copyWords(int[] values) {
        int[] copy = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            copy[i] = values[i];
        }
        return copy;
    }

    private static String formatWords(int[] words) {
        if (words == null || words.length == 0) {
            return "[]";
        }
        if (words.length == 1) {
            return "[" + words[0] + "]";
        }
        return "[" + words[0] + ", " + words[1] + "]";
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

    private static boolean isNoResponseError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            if (className.contains("TimeOut") || className.contains("Timeout")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("timeout")
                        || normalized.contains("timed out")
                        || normalized.contains("sem resposta")
                        || normalized.contains("no response")
                        || normalized.contains("checksum")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isMaximumRetriesError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if ("MaximumNumberOfRetriesReachedException".equals(current.getClass().getSimpleName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("maximum number of retries reached")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static final class MonitoredTag {
        private final String name;
        private final String memoryArea;
        private final int address;
        private final int bit;
        private final int lengthWords;
        private final boolean persistHistory;

        public MonitoredTag(String name, String memoryArea, int address, int bit, int lengthWords,
                boolean persistHistory) {
            this.name = name;
            this.memoryArea = memoryArea == null ? "DM" : memoryArea.toUpperCase();
            this.address = address;
            this.bit = bit;
            this.lengthWords = Math.max(1, lengthWords);
            this.persistHistory = persistHistory;
        }

        public String getName() {
            return name;
        }

        public String getMemoryArea() {
            return memoryArea;
        }

        public int getAddress() {
            return address;
        }

        public int getBit() {
            return bit;
        }

        public int getLengthWords() {
            return lengthWords;
        }

        public boolean isPersistHistory() {
            return persistHistory;
        }
    }
}
