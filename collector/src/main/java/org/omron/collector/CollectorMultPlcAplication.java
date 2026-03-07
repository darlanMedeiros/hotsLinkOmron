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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.service.DmValueService;
import org.ctrl.extras.Tag;
import org.omron.collector.util.PlcNodeMonitorPanel;
import org.omron.collector.util.SharedSerial;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class CollectorMultPlcAplication {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOG_LINES = 4000;
    private static final Path LOG_FILE = Path.of("collector-mult-plc.log");
    private static final Pattern PLC_NODE_PATTERN = Pattern.compile("PLC\\s*([0-9]+).*");
    private static volatile CollectorMultPlcAplication activeInstance;

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

    private final List<PlcNodeMonitorPanel> plcPanels = new ArrayList<>();
    private final SharedSerial sharedSerial = new SharedSerial();
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private volatile boolean shuttingDown;
    private volatile boolean forcedDisconnectInProgress;

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

        List<PlcConfig> configs = loadPlcConfigsFromDb();
        JPanel container = new JPanel(new GridLayout(Math.max(1, configs.size()), 1, 4, 4));
        container.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        if (configs.isEmpty()) {
            log("Nenhum device/tag DM encontrado no banco. Cadastre devices e tags para iniciar monitoramento.");
        }

        for (int i = 0; i < configs.size(); i++) {
            PlcConfig cfg = configs.get(i);
            final int panelIndex = i + 1;
            PlcNodeMonitorPanel panel = new PlcNodeMonitorPanel(
                    panelIndex,
                    cfg.title,
                    cfg.mnemonic,
                    cfg.description,
                    cfg.nodeId,
                    cfg.tags,
                    sharedSerial.getIoLock(),
                    this::isSharedConnected,
                    sharedSerial::getHandler,
                    this::ensureDb,
                    () -> dmValueService,
                    this::log,
                    () -> requestDisconnectByUnresponsiveNode(panelIndex));
            plcPanels.add(panel);
            container.add(panel.getPanel());
        }

        frame.add(container, BorderLayout.CENTER);
        frame.add(buildLogPanel(), BorderLayout.EAST);
    }

    private List<PlcConfig> loadPlcConfigsFromDb() {
        ensureDb();
        JdbcTemplate jdbc = dbContext.getBean(JdbcTemplate.class);
        Map<String, Tag> catalog = buildTagCatalog();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT d.id AS device_id, d.mnemonic, d.name AS device_name, d.description AS device_description, " +
                        "t.name AS tag_name, m.address AS memory_address, m.name AS memory_name " +
                        "FROM public.device d " +
                        "LEFT JOIN public.tag t ON t.device_id = d.id " +
                        "LEFT JOIN public.memory m ON m.id = t.memory_id " +
                        "ORDER BY d.id, t.id");

        Map<String, PlcConfig> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String mnemonic = asString(row.get("mnemonic"));
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                continue;
            }
            PlcConfig cfg = grouped.get(mnemonic);
            if (cfg == null) {
                int nodeId = inferNodeId(mnemonic, grouped.size() + 1);
                cfg = new PlcConfig(
                        asString(row.get("device_name"), mnemonic),
                        mnemonic,
                        asString(row.get("device_description"), "Omron " + mnemonic),
                        nodeId,
                        new ArrayList<>());
                grouped.put(mnemonic, cfg);
            }

            String tagName = asString(row.get("tag_name"));
            Integer address = asInt(row.get("memory_address"));
            String memoryName = asString(row.get("memory_name"));
            if (tagName == null || address == null || address.intValue() < 0) {
                continue;
            }
            if (memoryName != null && !memoryName.startsWith("DM_")) {
                continue;
            }

            Tag known = catalog.get(tagName);
            int lengthWords = known == null || known.isBit() ? 1 : known.getLengthWords();
            cfg.tags.add(new PlcNodeMonitorPanel.MonitoredTag(tagName, address.intValue(), lengthWords));
        }

        return new ArrayList<>(grouped.values());
    }

    private Map<String, Tag> buildTagCatalog() {
        Map<String, Tag> out = new HashMap<>();
        java.lang.reflect.Field[] fields = Tag.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != Tag.class) {
                continue;
            }
            try {
                Object raw = field.get(null);
                if (raw instanceof Tag) {
                    Tag tag = (Tag) raw;
                    out.put(tag.getName(), tag);
                }
            } catch (IllegalAccessException ignored) {
                // ignore and continue
            }
        }
        return out;
    }

    private int inferNodeId(String mnemonic, int fallback) {
        Matcher matcher = PLC_NODE_PATTERN.matcher(mnemonic == null ? "" : mnemonic.toUpperCase());
        if (matcher.matches()) {
            try {
                int node = Integer.parseInt(matcher.group(1));
                return node > 0 ? node : fallback;
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String asString(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private static String asString(Object raw, String fallback) {
        String value = asString(raw);
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private static Integer asInt(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return Integer.valueOf(((Number) raw).intValue());
        }
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
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
        forcedDisconnectInProgress = false;

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
            SharedSerial.Config config = new SharedSerial.Config(
                    requestedPort,
                    Integer.parseInt(baudCombo.getSelectedItem().toString()),
                    Integer.parseInt(dataBitsField.getText().trim()),
                    Integer.parseInt(stopBitsField.getText().trim()),
                    SerialPortAbstract.Parity.valueOf(parityCombo.getSelectedItem().toString()),
                    Integer.parseInt(timeoutField.getText().trim()),
                    Boolean.valueOf(rtsCheckBox.isSelected()),
                    Boolean.valueOf(dtrCheckBox.isSelected()));
            sharedSerial.connect(config);
            ensureDb();
            setSerialStatus("CONECTADO");
            refreshNodeCommStatus();
            log("Comunicacao serial compartilhada conectada na porta " + requestedPort + ".");
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

    private synchronized void disconnectSharedSerial() {
        for (PlcNodeMonitorPanel panel : plcPanels) {
            panel.stopMonitor();
        }
        safeStopSharedHandler();
        setSerialStatus("DESCONECTADO");
        refreshNodeCommStatus();
        log("Comunicacao serial compartilhada encerrada.");
    }

    private void requestDisconnectByUnresponsiveNode(int nodeIndex) {
        if (shuttingDown || !isSharedConnected()) {
            return;
        }
        synchronized (this) {
            if (forcedDisconnectInProgress) {
                return;
            }
            forcedDisconnectInProgress = true;
        }
        Thread disconnectThread = new Thread(() -> {
            try {
                log("[PLC " + nodeIndex + "] Sem resposta prolongada. Desconectando serial compartilhada para liberar a porta.");
                disconnectSharedSerial();
            } finally {
                forcedDisconnectInProgress = false;
            }
        }, "collector-force-shared-disconnect");
        disconnectThread.setDaemon(true);
        disconnectThread.start();
    }

    private boolean isSharedConnected() {
        return sharedSerial.isConnected();
    }

    private void safeStopSharedHandler() {
        if (sharedSerial.isConnected()) {
            sharedSerial.disconnect();
        }
    }

    private void setSerialStatus(String text) {
        runOnEdt(() -> serialStatusLabel.setText(text));
    }

    private void refreshNodeCommStatus() {
        String shared = isSharedConnected() ? "CONECTADA" : "DESCONECTADA";
        for (PlcNodeMonitorPanel panel : plcPanels) {
            panel.setCommStatus(shared);
        }
    }

    private synchronized void ensureDb() {
        if (dbContext == null) {
            dbContext = new AnnotationConfigApplicationContext(DbConfig.class);
            dmValueService = dbContext.getBean(DmValueService.class);
        }
    }

    private synchronized void closeDb() {
        if (dbContext != null) {
            dbContext.close();
            dbContext = null;
            dmValueService = null;
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
            SharedSerial.refreshAvailablePorts(portCombo, selected, "COM1");
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
        return SharedSerial.getSelectedPortName(portCombo);
    }

    private boolean isPortAvailable(String portName) {
        try {
            return SharedSerial.isPortAvailable(portName);
        } catch (Exception ex) {
            logError("Falha ao validar porta serial " + portName, ex);
            return false;
        }
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
        return SharedSerial.isSerialPortInUse(error);
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
            String message = "Uncaught exception thread=" + thread.getName() + ": " + describeError(error);
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

    private static final class PlcConfig {
        private final String title;
        private final String mnemonic;
        private final String description;
        private final int nodeId;
        private final List<PlcNodeMonitorPanel.MonitoredTag> tags;

        private PlcConfig(String title, String mnemonic, String description, int nodeId,
                List<PlcNodeMonitorPanel.MonitoredTag> tags) {
            this.title = title;
            this.mnemonic = mnemonic;
            this.description = description;
            this.nodeId = nodeId;
            this.tags = tags;
        }
    }
}
