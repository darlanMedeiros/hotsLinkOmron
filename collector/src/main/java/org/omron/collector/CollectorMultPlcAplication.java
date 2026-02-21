package org.omron.collector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.comm.serial.SerialPortFactoryJSerialComm;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.comm.serial.SerialUtils;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.TagService;
import org.ctrl.extras.Tag;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CollectorMultPlcAplication {

    private static final int PLC_NODES = 5;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int INTER_TAG_DELAY_MS = 1000;
    private static final int ERROR_RETRY_DELAY_MS = 1500;
    private static final int MAX_ERROR_RETRY_DELAY_MS = 15000;
    private static final int MAX_LOG_LINES = 4000;
    private static final int HISTORY_RETENTION_DAYS = 14;
    private static final int HISTORY_PRUNE_INTERVAL_CYCLES = 500;
    private static final Path LOG_FILE = Path.of("collector-mult-plc.log");
    private static volatile CollectorMultPlcAplication activeInstance;

    private static final Tag[] MONITORED_TAGS = new Tag[] {
            Tag.PECAPH29,
            Tag.PECAPH30,
            Tag.PECAPH31,
            Tag.PECAROLLERCARGA41,
            Tag.PECAROLLERDESC41,
            Tag.PECAROLLERCARGA42,
            Tag.PECAROLLERDESC42,
            Tag.PECAROLLERCARGA43,
            Tag.PECAROLLERDESC43,
            Tag.QUALIDADE41,
            Tag.QUALIDADE42,
            Tag.QUALIDADE43
    };

    private JFrame frame;
    private JTextArea logArea;

    private JComboBox<String> portCombo;
    private JComboBox<String> baudCombo;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField timeoutField;
    private JCheckBox rtsCheckBox;
    private JCheckBox dtrCheckBox;
    private JLabel serialStatusLabel;

    private final List<PlcNodePanel> plcPanels = new ArrayList<>();
    private final Object comLock = new Object();

    private SerialPortHandlerImp sharedComHandler;
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private TagService tagService;
    private volatile boolean shuttingDown;

    public static void main(String[] args) {
        installGlobalExceptionHandler();
        SwingUtilities.invokeLater(() -> {
            CollectorMultPlcAplication app = new CollectorMultPlcAplication();
            app.frame.setVisible(true);
        });
    }

    public CollectorMultPlcAplication() {
        activeInstance = this;
        buildUi();
    }

    private void buildUi() {
        frame = new JFrame("Collector Multi PLC Monitor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(new Dimension(1366, 700));
        frame.setLayout(new BorderLayout(5, 5));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndShutdown();
            }
        });

        frame.add(buildSharedSerialPanel(), BorderLayout.NORTH);

        JPanel container = new JPanel(new GridLayout(PLC_NODES, 1, 4, 4));
        container.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (int i = 0; i < PLC_NODES; i++) {
            PlcNodePanel panel = new PlcNodePanel(i + 1, i);
            plcPanels.add(panel);
            container.add(panel.panel);
        }

        frame.add(container, BorderLayout.CENTER);
        frame.add(buildLogPanel(), BorderLayout.EAST);
    }

    private JPanel buildSharedSerialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Comunicacao serial compartilhada"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        portCombo = new JComboBox<>();
        portCombo.setEditable(true);
        baudCombo = new JComboBox<>(new String[] { "4800", "9600", "14400", "19200", "38400", "57600", "115200" });
        baudCombo.setSelectedItem("9600");
        dataBitsField = new JTextField("7");
        stopBitsField = new JTextField("2");
        parityCombo = new JComboBox<>(new String[] { "EVEN", "NONE", "ODD", "MARK", "SPACE" });
        parityCombo.setSelectedItem("EVEN");
        timeoutField = new JTextField("10000");
        rtsCheckBox = new JCheckBox("RTS", true);
        dtrCheckBox = new JCheckBox("DTS", true);

        int row = 0;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Porta"), c);
        c.gridx = 1;
        panel.add(portCombo, c);
        refreshAvailablePorts(null);

        c.gridx = 2;
        panel.add(new JLabel("Baud"), c);
        c.gridx = 3;
        panel.add(baudCombo, c);

        c.gridx = 4;
        panel.add(new JLabel("Data bits"), c);
        c.gridx = 5;
        panel.add(dataBitsField, c);

        row++;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Stop bits"), c);
        c.gridx = 1;
        panel.add(stopBitsField, c);

        c.gridx = 2;
        panel.add(new JLabel("Paridade"), c);
        c.gridx = 3;
        panel.add(parityCombo, c);

        c.gridx = 4;
        panel.add(new JLabel("Timeout (ms)"), c);
        c.gridx = 5;
        panel.add(timeoutField, c);

        row++;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Controle linha"), c);
        c.gridx = 1;
        panel.add(rtsCheckBox, c);
        c.gridx = 2;
        panel.add(dtrCheckBox, c);

        row++;
        JButton connectButton = new JButton("Conectar serial");
        connectButton.addActionListener(e -> connectSharedSerial());
        c.gridx = 0;
        c.gridy = row;
        panel.add(connectButton, c);

        JButton disconnectButton = new JButton("Desconectar serial");
        disconnectButton.addActionListener(e -> disconnectSharedSerial());
        c.gridx = 1;
        panel.add(disconnectButton, c);

        c.gridx = 2;
        panel.add(new JLabel("Estado serial"), c);
        serialStatusLabel = new JLabel("DESCONECTADO");
        c.gridx = 3;
        c.gridwidth = 3;
        panel.add(serialStatusLabel, c);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log Global"));
        panel.setPreferredSize(new Dimension(600, 0));
        logArea = new JTextArea(10, 100);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private void connectSharedSerial() {
        if (isSharedConnected()) {
            log("Comunicacao serial compartilhada ja esta ativa.");
            return;
        }

        String requestedPort = getSelectedPortName();
        if (requestedPort.isEmpty()) {
            setSerialStatus("PORTA INVALIDA");
            refreshNodeCommStatus();
            log("Selecione uma porta serial valida.");
            return;
        }
        refreshAvailablePorts(requestedPort);
        if (!isPortAvailable(requestedPort)) {
            setSerialStatus("PORTA INEXISTENTE");
            refreshNodeCommStatus();
            log("Porta serial " + requestedPort + " nao encontrada. Selecione uma porta disponivel.");
            return;
        }

        try {
            SerialParameters sp = new SerialParameters();
            sp.setDevice(requestedPort);
            sp.setBaudRate(SerialPortAbstract.BaudRate.getBaudRate(Integer.parseInt(baudCombo.getSelectedItem().toString())));
            sp.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
            sp.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
            sp.setParity(SerialPortAbstract.Parity.valueOf(parityCombo.getSelectedItem().toString()));
            sp.setRtsEnabled(rtsCheckBox.isSelected());
            sp.setDtrEnabled(dtrCheckBox.isSelected());

            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            SerialPortHandlerImp handler = new SerialPortHandlerImp(SerialUtils.createSerial(sp));
            handler.setProtocolHandler(new ToolbusProtocol());
            if (handler instanceof IComControl) {
                int timeout = Integer.parseInt(timeoutField.getText().trim());
                ((IComControl) handler).setCommunicationTimeOut(timeout);
            }

            handler.initialize();
            handler.start();

            sharedComHandler = handler;
            ensureDb();
            setSerialStatus("CONECTADO");
            refreshNodeCommStatus();
            log("Comunicacao serial compartilhada conectada na porta " + sp.getDevice() + ".");
        } catch (Exception ex) {
            if (isSerialPortInUse(ex)) {
                setSerialStatus("PORTA EM USO");
                logError("Porta serial " + requestedPort + " em uso por outro processo", ex);
                showPortInUseDialog(requestedPort);
            } else {
                setSerialStatus("ERRO");
                logError("Falha na conexao serial", ex);
            }
            safeStopSharedHandler();
            refreshNodeCommStatus();
        }
    }

    private void disconnectSharedSerial() {
        for (PlcNodePanel panel : plcPanels) {
            panel.stopMonitor();
        }
        safeStopSharedHandler();
        setSerialStatus("DESCONECTADO");
        refreshNodeCommStatus();
        log("Comunicacao serial compartilhada encerrada.");
    }

    private boolean isSharedConnected() {
        return sharedComHandler != null && sharedComHandler.isStarted();
    }

    private void safeStopSharedHandler() {
        if (sharedComHandler != null) {
            synchronized (comLock) {
                try {
                    sharedComHandler.stop();
                } catch (Exception ex) {
                    log("Erro ao parar comunicacao serial: " + ex.getMessage());
                } finally {
                    sharedComHandler = null;
                }
            }
        }
    }

    private void setSerialStatus(String text) {
        runOnEdt(() -> serialStatusLabel.setText(text));
    }

    private void refreshNodeCommStatus() {
        String shared = isSharedConnected() ? "CONECTADA" : "DESCONECTADA";
        for (PlcNodePanel panel : plcPanels) {
            panel.setCommStatus(shared);
        }
    }

    private final class PlcNodePanel {

        private final int nodeIndex;
        private final JPanel panel;

        private JTextField nodeField;
        private JTextField pollMsField;
        private JLabel commStatusLabel;
        private JLabel monitorStatusLabel;

        private volatile boolean monitoring;
        private Thread monitorThread;
        private IDevice plc;
        private DeviceInfo deviceInfo;

        private PlcNodePanel(int nodeIndex, int defaultNodeId) {
            this.nodeIndex = nodeIndex;
            this.panel = buildNodePanel(defaultNodeId);
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

        private void startMonitor() {
            if (monitoring) {
                logPrefix("Monitor ja esta em execucao.");
                return;
            }
            if (!isSharedConnected()) {
                logPrefix("Conecte a serial compartilhada antes de iniciar o monitor.");
                return;
            }
            ensureDb();
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

        private void runMonitorLoop(int pollMs) {
            setMonitorStatus("RODANDO");
            logPrefix("Monitor iniciado para " + MONITORED_TAGS.length + " TAGs.");

            Map<String, int[]> lastValues = new LinkedHashMap<>();
            int cycleCount = 0;
            int consecutiveErrors = 0;

            while (monitoring) {
                try {
                    if (!isSharedConnected()) {
                        throw new IllegalStateException("Serial compartilhada desconectada.");
                    }

                    for (Tag tag : MONITORED_TAGS) {
                        if (!monitoring) {
                            break;
                        }

                        AreaReadDM read = new AreaReadDM(plc, tag.toMemoryVariable());
                        synchronized (comLock) {
                            if (!isSharedConnected()) {
                                throw new IllegalStateException("Serial compartilhada desconectada.");
                            }
                            sharedComHandler.send(read);
                        }

                        int[] values = parseReply(read.getReply(), tag.getLengthWords());
                        if (values == null) {
                            logPrefix("Leitura invalida para TAG " + tag.getName() + ".");
                        } else {
                            int[] previous = lastValues.get(tag.getName());
                            if (hasChanged(previous, values)) {
                                dmValueService.saveRange(deviceInfo, tag.getAddress(), values);
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
                    if (cycleCount % HISTORY_PRUNE_INTERVAL_CYCLES == 0) {
                        int deletedRows = dmValueService.pruneHistoryOlderThanDays(HISTORY_RETENTION_DAYS);
                        if (deletedRows > 0) {
                            logPrefix("Limpeza de historico: " + deletedRows
                                    + " registros removidos de memory_value.");
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

        private void stopMonitor() {
            monitoring = false;
            Thread localThread = monitorThread;
            if (localThread != null) {
                localThread.interrupt();
                try {
                    localThread.join(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    monitorThread = null;
                }
            }
            setMonitorStatus("PARADO");
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

        private void setCommStatus(String text) {
            runOnEdt(() -> commStatusLabel.setText(text));
        }

        private void setMonitorStatus(String text) {
            runOnEdt(() -> monitorStatusLabel.setText(text));
        }

        private void logPrefix(String message) {
            log("[PLC " + nodeIndex + "] " + message);
        }

        private void ensureTagBindings() {
            if (tagService == null || deviceInfo == null) {
                return;
            }
            for (Tag tag : MONITORED_TAGS) {
                tagService.getOrCreateDmTag(deviceInfo, tag.getName(), tag.getAddress());
            }
        }
    }

    private synchronized void ensureDb() {
        if (dbContext == null) {
            dbContext = new AnnotationConfigApplicationContext(DbConfig.class);
            dmValueService = dbContext.getBean(DmValueService.class);
            tagService = dbContext.getBean(TagService.class);
        }
    }

    private synchronized void closeDb() {
        if (dbContext != null) {
            dbContext.close();
            dbContext = null;
            dmValueService = null;
            tagService = null;
        }
    }

    private void confirmAndShutdown() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Encerrar o Collector Multi PLC agora?",
                "Confirmar encerramento",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            log("Encerramento solicitado pelo usuario.");
            shutdown();
        } else {
            log("Encerramento cancelado.");
        }
    }

    private void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        activeInstance = null;
        disconnectSharedSerial();
        closeDb();
        if (frame != null) {
            frame.dispose();
        }
    }

    private void refreshAvailablePorts(String preferredPort) {
        String selected = (preferredPort == null || preferredPort.trim().isEmpty()) ? getSelectedPortName()
                : preferredPort.trim();
        try {
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            List<String> ports = SerialUtils.getPortIdentifiers();
            portCombo.removeAllItems();
            for (String port : ports) {
                portCombo.addItem(port);
            }
            if (selected == null || selected.isEmpty()) {
                if (ports.contains("COM1")) {
                    selected = "COM1";
                } else if (!ports.isEmpty()) {
                    selected = ports.get(0);
                }
            } else if (!ports.contains(selected)) {
                portCombo.addItem(selected);
            }
            if (selected != null && !selected.isEmpty()) {
                portCombo.setSelectedItem(selected);
            }
        } catch (Exception ex) {
            if (selected != null && !selected.isEmpty()) {
                portCombo.removeAllItems();
                portCombo.addItem(selected);
                portCombo.setSelectedItem(selected);
            }
            logError("Nao foi possivel listar portas automaticamente", ex);
        }
    }

    private String getSelectedPortName() {
        if (portCombo == null) {
            return "";
        }
        Object selectedItem = portCombo.getSelectedItem();
        if (selectedItem == null) {
            return "";
        }
        String value = selectedItem.toString().trim();
        if (value.isEmpty() && portCombo.getEditor() != null) {
            java.awt.Component editorComponent = portCombo.getEditor().getEditorComponent();
            if (editorComponent instanceof JTextComponent) {
                value = ((JTextComponent) editorComponent).getText().trim();
            }
        }
        return value;
    }

    private boolean isPortAvailable(String portName) {
        try {
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            List<String> ports = SerialUtils.getPortIdentifiers();
            for (String port : ports) {
                if (portName.equalsIgnoreCase(port)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            logError("Falha ao validar porta serial " + portName, ex);
            return false;
        }
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

    private void log(String message) {
        appendPersistentLog(message, null);
        runOnEdt(() -> {
            if (logArea == null) {
                return;
            }
            String time = LocalDateTime.now().format(TIME_FMT);
            logArea.append("[" + time + "] " + message + "\n");
            trimLogIfNeeded();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void logError(String context, Throwable error) {
        String message = context + ": " + describeError(error);
        appendPersistentLog(message, error);
        runOnEdt(() -> {
            if (logArea == null) {
                return;
            }
            String time = LocalDateTime.now().format(TIME_FMT);
            logArea.append("[" + time + "] " + message + "\n");
            trimLogIfNeeded();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void trimLogIfNeeded() {
        int lineCount = logArea.getLineCount();
        if (lineCount <= MAX_LOG_LINES) {
            return;
        }
        int linesToRemove = lineCount - MAX_LOG_LINES;
        try {
            int endOffset = logArea.getLineEndOffset(linesToRemove - 1);
            logArea.replaceRange("", 0, endOffset);
        } catch (BadLocationException ignored) {
            // Keep application running even if line offsets mismatch momentarily.
        }
    }

    private static boolean isSerialPortInUse(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            if ("PortInUseException".equals(className)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("in use")
                        || normalized.contains("em uso")
                        || normalized.contains("busy")
                        || normalized.contains("access denied")
                        || normalized.contains("acesso negado")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void showPortInUseDialog(String portName) {
        runOnEdt(() -> JOptionPane.showMessageDialog(
                frame,
                "A porta serial " + portName + " ja esta em uso por outro processo.\n"
                        + "Feche o aplicativo que esta usando essa porta e tente novamente.",
                "Porta em uso",
                JOptionPane.WARNING_MESSAGE));
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        SwingUtilities.invokeLater(task);
    }

    private static void installGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            String message = "Uncaught exception thread=" + thread.getName() + ": "
                    + describeError(error);
            appendPersistentLog(message, error);
            CollectorMultPlcAplication app = activeInstance;
            if (app != null) {
                app.logError("Erro nao tratado", error);
            }
        });
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

    private static synchronized void appendPersistentLog(String message, Throwable error) {
        try {
            StringBuilder line = new StringBuilder();
            line.append("[")
                    .append(LocalDateTime.now().format(FILE_TIME_FMT))
                    .append("] ")
                    .append(message)
                    .append(System.lineSeparator());
            if (error != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                error.printStackTrace(pw);
                pw.flush();
                line.append(sw).append(System.lineSeparator());
            }
            Files.writeString(
                    LOG_FILE,
                    line.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging should not break the application flow.
        }
    }
}
