package org.omron.collector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import org.ctrl.db.service.TagService;
import org.ctrl.extras.Tag;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;

final class PlcNodeMonitorPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int INTER_TAG_DELAY_MS = 1000;
    private static final int ERROR_RETRY_DELAY_MS = 1500;
    private static final int MAX_ERROR_RETRY_DELAY_MS = 15000;
    private static final int HISTORY_RETENTION_DAYS = 14;
    private static final int HISTORY_PRUNE_INTERVAL_CYCLES = 500;
    private static final long NO_RESPONSE_DISCONNECT_MS = 3000L;
    private static final long MAX_RETRIES_STOP_MONITOR_MS = 3000L;

    private final int nodeIndex;
    private final Tag[] monitoredTags;
    private final Object comLock;
    private final Supplier<Boolean> sharedConnectedSupplier;
    private final Supplier<SerialPortHandlerImp> sharedComHandlerSupplier;
    private final Runnable ensureDbAction;
    private final Supplier<DmValueService> dmValueServiceSupplier;
    private final Supplier<TagService> tagServiceSupplier;
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

    PlcNodeMonitorPanel(int nodeIndex,
            int defaultNodeId,
            Tag[] monitoredTags,
            Object comLock,
            Supplier<Boolean> sharedConnectedSupplier,
            Supplier<SerialPortHandlerImp> sharedComHandlerSupplier,
            Runnable ensureDbAction,
            Supplier<DmValueService> dmValueServiceSupplier,
            Supplier<TagService> tagServiceSupplier,
            Consumer<String> globalLogger,
            Runnable sharedDisconnectAction) {
        this.nodeIndex = nodeIndex;
        this.monitoredTags = monitoredTags;
        this.comLock = comLock;
        this.sharedConnectedSupplier = sharedConnectedSupplier;
        this.sharedComHandlerSupplier = sharedComHandlerSupplier;
        this.ensureDbAction = ensureDbAction;
        this.dmValueServiceSupplier = dmValueServiceSupplier;
        this.tagServiceSupplier = tagServiceSupplier;
        this.globalLogger = globalLogger;
        this.sharedDisconnectAction = sharedDisconnectAction;
        this.panel = buildNodePanel(defaultNodeId);
    }

    JPanel getPanel() {
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
        ensureDevice();
        ensureTagBindings();

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

    void stopMonitor() {
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

    void setCommStatus(String text) {
        runOnEdt(() -> commStatusLabel.setText(text));
    }

    private JPanel buildNodePanel(int defaultNodeId) {
        JPanel nodePanel = new JPanel(new GridBagLayout());
        nodePanel.setBorder(BorderFactory.createTitledBorder("PLC " + nodeIndex));
        nodePanel.setPreferredSize(new Dimension(0, 88));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        nodeField = new JTextField(Integer.toString(defaultNodeId));
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
        setMonitorStatus("RODANDO");
        logPrefix("Monitor iniciado para " + monitoredTags.length + " TAGs.");

        Map<String, int[]> lastValues = new LinkedHashMap<>();
        int cycleCount = 0;
        int consecutiveErrors = 0;
        long noResponseStartMs = 0L;
        boolean disconnectRequested = false;
        long maxRetriesErrorStartMs = 0L;
        boolean monitorStopRequested = false;

        while (monitoring) {
            try {
                if (!isSharedConnected()) {
                    throw new IllegalStateException("Serial compartilhada desconectada.");
                }

                for (Tag tag : monitoredTags) {
                    if (!monitoring) {
                        break;
                    }

                    AreaReadDM read = new AreaReadDM(plc, tag.toMemoryVariable());
                    synchronized (comLock) {
                        if (!isSharedConnected()) {
                            throw new IllegalStateException("Serial compartilhada desconectada.");
                        }
                        sharedComHandlerSupplier.get().send(read);
                    }

                    int[] values = parseReply(read.getReply(), tag.getLengthWords());
                    if (values == null) {
                        logPrefix("Leitura invalida para TAG " + tag.getName() + ".");
                    } else {
                        int[] previous = lastValues.get(tag.getName());
                        if (hasChanged(previous, values)) {
                            dmValueServiceSupplier.get().saveRange(deviceInfo, tag.getAddress(), values);
                            logPrefix("Alteracao salva: TAG " + tag.getName() + " (DM " + tag.getAddress()
                                    + ".." + (tag.getAddress() + tag.getLengthWords() - 1) + ") = "
                                    + formatWords(values) + ".");
                            lastValues.put(tag.getName(), copyWords(values));
                        }
                    }

                    Thread.sleep(INTER_TAG_DELAY_MS);
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
        int nodeId = Integer.parseInt(nodeField.getText().trim());
        if (plc == null || plc.getId() != nodeId) {
            plc = new DeviceImp(nodeId, "PLC-" + nodeIndex, "PLC", "Omron PLC node " + nodeIndex);
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
            deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());
        }
    }

    private void setMonitorStatus(String text) {
        runOnEdt(() -> monitorStatusLabel.setText(text));
    }

    private void logPrefix(String message) {
        globalLogger.accept("[PLC " + nodeIndex + "] " + message);
    }

    private void ensureTagBindings() {
        TagService tagService = tagServiceSupplier.get();
        if (tagService == null || deviceInfo == null) {
            return;
        }
        for (Tag tag : monitoredTags) {
            tagService.getOrCreateDmTag(deviceInfo, tag.getName(), tag.getAddress());
        }
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
}
