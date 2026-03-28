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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.service.DmValueService;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.extras.Tag;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.omron.collector.util.PlcNodeMonitorPanel;
import org.omron.collector.util.SharedSerial;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class CollectorMultPlcAplication {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOG_LINES = 4000;
    private static final Path LOG_FILE = Path.of("collector-mult-plc.log");
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

    private static final int MANUAL_DM_MAX_ADDRESSES = 100;
    private static final int MANUAL_READ_DELAY_MS = 200;
    private static final int DM_TERMINATOR_VALUE = 0xFFFF;
    private static final int MANUAL_DM_DATE_PART_COUNT = 6;
    private static final DateTimeFormatter MANUAL_DM_DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MANUAL_DM_GROUP_SIZE = 13;
    private static final int MANUAL_DM_TAG_VALUES_OFFSET = 8;
    private static final String[] MANUAL_PRODUCTION_TAG_NAMES = new String[] {
            "PRODUCAO_PH29",
            "PRODUCAO_PH30",
            "PRODUCAO_PH31",
            "PRODUCAO_SEC25",
            "PRODUCAO_SEC26",
            "PRODUCAO_SEC33"
    };
    private List<PlcConfig> plcConfigs = new ArrayList<>();
    private final Map<String, Integer> manualNodeLookup = new HashMap<>();
    private JComboBox<String> manualNodeCombo;
    private JTextField manualStartAddressField;
    private JButton manualScanButton;
    private JTextArea manualResultArea;
    private JLabel manualStatusLabel;
    private Thread manualScanThread;

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

        plcConfigs = loadPlcConfigsFromDb();
        JPanel container = new JPanel(new GridLayout(Math.max(1, plcConfigs.size()), 1, 4, 4));
        container.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        if (plcConfigs.isEmpty()) {
            log("Nenhum device/tag DM encontrado no banco. Cadastre devices e tags para iniciar monitoramento.");
        }

        for (int i = 0; i < plcConfigs.size(); i++) {
            PlcConfig cfg = plcConfigs.get(i);
            final int nodeId = cfg.nodeId;
            PlcNodeMonitorPanel panel = new PlcNodeMonitorPanel(
                    nodeId,
                    cfg.title,
                    cfg.mnemonic,
                    cfg.description,
                    cfg.nodeId,
                    cfg.tags,
                    () -> loadTagsForDevice(cfg.mnemonic),
                    sharedSerial.getIoLock(),
                    this::isSharedConnected,
                    sharedSerial::getHandler,
                    this::ensureDb,
                    () -> dmValueService,
                    this::log,
                    () -> requestDisconnectByUnresponsiveNode(nodeId));
            plcPanels.add(panel);
            container.add(panel.getPanel());
        }
        JTabbedPane centerTabs = new JTabbedPane();
        centerTabs.addTab("Monitoramento", container);
        centerTabs.addTab("Leitura DM", buildManualDmPanel(plcConfigs));
        frame.add(centerTabs, BorderLayout.CENTER);
        frame.add(buildLogPanel(), BorderLayout.EAST);
    }

    private List<PlcConfig> loadPlcConfigsFromDb() {
        ensureDb();
        JdbcTemplate jdbc = dbContext.getBean(JdbcTemplate.class);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT d.id AS device_id, d.mnemonic, d.name AS device_name, d.description AS device_description, " +
                        "d.no_id AS device_node_id " +
                        "FROM public.device d " +
                        "ORDER BY d.id");

        Map<String, PlcConfig> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String mnemonic = asString(row.get("mnemonic"));
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                continue;
            }
            PlcConfig cfg = grouped.get(mnemonic);
            if (cfg == null) {
                Integer configuredNodeId = asInt(row.get("device_node_id"));
                int nodeId = configuredNodeId == null ? 0 : configuredNodeId.intValue();
                cfg = new PlcConfig(
                        asString(row.get("device_name"), mnemonic),
                        mnemonic,
                        asString(row.get("device_description"), "Omron " + mnemonic),
                        nodeId,
                        loadTagsForDevice(mnemonic));
                grouped.put(mnemonic, cfg);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    private List<PlcNodeMonitorPanel.MonitoredTag> loadTagsForDevice(String deviceMnemonic) {
        ensureDb();
        if (deviceMnemonic == null || deviceMnemonic.trim().isEmpty()) {
            return new ArrayList<>();
        }

        JdbcTemplate jdbc = dbContext.getBean(JdbcTemplate.class);
        Map<String, Tag> catalog = buildTagCatalog();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.name AS tag_name, t.persist_history AS persist_history, " +
                        "m.address AS memory_address, m.name AS memory_name " +
                        "FROM public.device d " +
                        "JOIN public.machine ma ON ma.device_id = d.id " +
                        "JOIN public.tag t ON t.machine_id = ma.id " +
                        "JOIN public.memory m ON m.id = t.memory_id AND m.device_id = d.id " +
                        "WHERE d.mnemonic = ? " +
                        "ORDER BY t.id",
                deviceMnemonic);

        List<PlcNodeMonitorPanel.MonitoredTag> tags = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String tagName = asString(row.get("tag_name"));
            Integer address = asInt(row.get("memory_address"));
            String memoryName = asString(row.get("memory_name"));
            boolean persistHistory = asBoolean(row.get("persist_history"), true);
            if (tagName == null || address == null || address.intValue() < 0) {
                continue;
            }
            if (memoryName != null && !memoryName.startsWith("DM_")) {
                continue;
            }
            Tag known = catalog.get(tagName);
            int lengthWords = known == null || known.isBit() ? 1 : known.getLengthWords();
            tags.add(new PlcNodeMonitorPanel.MonitoredTag(tagName, address.intValue(), lengthWords, persistHistory));
        }
        return tags;
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

    private static boolean asBoolean(Object raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return fallback;
        }
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
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
        stopManualScanThread();
        for (PlcNodeMonitorPanel panel : plcPanels) {
            panel.stopMonitor();
        }
        safeStopSharedHandler();
        setSerialStatus("DESCONECTADO");
        refreshNodeCommStatus();
        log("Comunicacao serial compartilhada encerrada.");
    }

    private void requestDisconnectByUnresponsiveNode(int nodeId) {
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
                log("[Node " + nodeId
                        + "] Sem resposta prolongada. Desconectando serial compartilhada para liberar a porta.");
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

    private JPanel buildManualDmPanel(List<PlcConfig> configs) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Leitura DM manual"));

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        manualNodeCombo = new JComboBox<>();
        manualNodeCombo.setEditable(true);
        manualStartAddressField = new JTextField("3000");
        manualStartAddressField.setColumns(8);
        manualScanButton = new JButton("Ler e limpar DM");
        manualScanButton.addActionListener(e -> startManualDmScan());
        manualStatusLabel = new JLabel("PARADO");

        rebuildManualNodeOptions(configs);

        int row = 0;
        c.gridx = 0;
        c.gridy = row;
        controls.add(new JLabel("Nodo ID"), c);
        c.gridx = 1;
        controls.add(manualNodeCombo, c);

        c.gridx = 2;
        controls.add(new JLabel("Endereco inicial DM"), c);
        c.gridx = 3;
        controls.add(manualStartAddressField, c);

        row++;
        c.gridx = 0;
        c.gridy = row;
        controls.add(new JLabel("Regra"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        controls.add(new JLabel("Le ate #FFFF ou maximo de 100 palavras; depois escreve #0000 nas lidas."), c);
        c.gridwidth = 1;

        row++;
        c.gridx = 0;
        c.gridy = row;
        controls.add(manualScanButton, c);
        c.gridx = 1;
        controls.add(new JLabel("Status"), c);
        c.gridx = 2;
        c.gridwidth = 2;
        controls.add(manualStatusLabel, c);

        manualResultArea = new JTextArea(16, 70);
        manualResultArea.setEditable(false);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(manualResultArea), BorderLayout.CENTER);
        return panel;
    }

    private void rebuildManualNodeOptions(List<PlcConfig> configs) {
        manualNodeLookup.clear();
        if (manualNodeCombo == null) {
            return;
        }

        manualNodeCombo.removeAllItems();
        if (configs != null) {
            for (PlcConfig cfg : configs) {
                String label = cfg.mnemonic + " (node " + cfg.nodeId + ")";
                if (!manualNodeLookup.containsKey(label)) {
                    manualNodeLookup.put(label, Integer.valueOf(cfg.nodeId));
                    manualNodeCombo.addItem(label);
                }
            }
        }

        if (manualNodeCombo.getItemCount() == 0) {
            manualNodeCombo.addItem("0");
        }
        manualNodeCombo.setSelectedIndex(0);
    }

    // =============================
    // FALTA: persistencia da leitura manual, associando os valores lidos a tags de
    // producao, usando timestamp do PLC
    // =============================
    private void startManualDmScan() {
        if (manualScanThread != null && manualScanThread.isAlive()) {
            log("Scanner DM manual ja esta em execucao.");
            return;
        }
        if (!isSharedConnected()) {
            setManualStatus("DESCONECTADO");
            log("Conecte a serial compartilhada antes de executar a leitura DM manual.");
            return;
        }

        final int nodeId;
        final int startAddress;
        try {
            nodeId = parseManualNodeId();
            startAddress = Integer.parseInt(manualStartAddressField.getText().trim());
            if (startAddress < 0) {
                throw new NumberFormatException("Endereco inicial deve ser >= 0.");
            }
        } catch (Exception ex) {
            setManualStatus("PARAMETRO INVALIDO");
            log("Parametros invalidos para scanner DM manual: " + describeError(ex));
            return;
        }

        runOnEdt(() -> {
            manualScanButton.setEnabled(false);
            if (manualResultArea != null) {
                manualResultArea.setText("");
            }
        });
        setManualStatus("LENDO");
        appendManualResult("Inicio leitura DM: node=" + nodeId + ", endereco inicial=" + startAddress + ".");

        manualScanThread = new Thread(() -> {
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
                    synchronized (sharedSerial.getIoLock()) {
                        if (!isSharedConnected()) {
                            throw new IllegalStateException(
                                    "Serial compartilhada desconectada durante leitura manual.");
                        }
                        sharedSerial.getHandler().send(read);
                    }

                    int value = parseSingleWord(read.getReply());
                    addressesRead.add(Integer.valueOf(address));
                    valuesByAddress.put(Integer.valueOf(address), Integer.valueOf(value));
                    appendManualResult("DM " + address + " = #" + toHexWord(value));

                    if (value == DM_TERMINATOR_VALUE) {
                        terminatorFound = true;
                        appendManualResult("Encontrado terminador #FFFF em DM " + address + ".");
                        break;
                    }

                    Thread.sleep(MANUAL_READ_DELAY_MS);
                }

                if (!terminatorFound) {
                    appendManualResult("Terminador #FFFF nao encontrado em " + MANUAL_DM_MAX_ADDRESSES + " palavras.");
                }

                if (!addressesRead.isEmpty()) {
                    persistManualProductionFromManualRead(nodeId, addressesRead, valuesByAddress);
                    writeZeroToReadAddresses(plc, addressesRead);
                    appendManualResult("#0000 escrito em " + addressesRead.size() + " endereco(s) lido(s).");
                }

                setManualStatus("CONCLUIDO");
                log("Scanner DM manual concluido. Node " + nodeId + ", inicio DM " + startAddress
                        + ", total lido " + addressesRead.size() + ".");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                setManualStatus("INTERROMPIDO");
                appendManualResult("Scanner DM manual interrompido.");
            } catch (Exception ex) {
                setManualStatus("ERRO");
                logError("Falha no scanner DM manual", ex);
                appendManualResult("Falha: " + describeError(ex));
            } finally {
                runOnEdt(() -> {
                    if (manualScanButton != null) {
                        manualScanButton.setEnabled(true);
                    }
                });
                manualScanThread = null;
            }
        }, "collector-manual-dm-scan");
        manualScanThread.setDaemon(true);
        manualScanThread.start();
    }

    // =============================
    // FALTA: persistencia da leitura manual, associando os valores lidos a tags de
    // producao, usando timestamp do PLC
    // =============================
    // write Zero to read addresses to "clear" the DM values after reading, as per
    // the defined rule
    private void writeZeroToReadAddresses(IDevice plc, List<Integer> addresses) throws Exception {
        synchronized (sharedSerial.getIoLock()) {
            if (!isSharedConnected()) {
                throw new IllegalStateException("Serial compartilhada desconectada antes da escrita de limpeza.");
            }
            for (Integer rawAddress : addresses) {
                int address = rawAddress.intValue();
                AreaWriteDM write = new AreaWriteDM(plc, address, new int[] { 0 }, MemoryWrite.HEX);
                sharedSerial.getHandler().send(write);
            }
        }
    }

    private int parseManualNodeId() {
        if (manualNodeCombo == null) {
            return 0;
        }
        Object selected = manualNodeCombo.getSelectedItem();
        String raw = selected == null ? "" : selected.toString().trim();
        if (raw.isEmpty()) {
            return 0;
        }

        Integer mapped = manualNodeLookup.get(raw);
        if (mapped != null) {
            return mapped.intValue();
        }

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (Character.isDigit(ch) || (ch == '-' && digits.length() == 0)) {
                digits.append(ch);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.length() == 0) {
            throw new NumberFormatException("Node ID invalido: " + raw);
        }
        int parsed = Integer.parseInt(digits.toString());
        if (parsed < 0) {
            throw new NumberFormatException("Node ID deve ser >= 0.");
        }
        return parsed;
    }

    private static int parseSingleWord(IData reply) {
        if (reply == null) {
            throw new IllegalStateException("Resposta vazia na leitura DM manual.");
        }
        int[] dataBuff = reply.toHexArray();
        if (dataBuff == null || dataBuff.length < 4) {
            throw new IllegalStateException("Resposta invalida na leitura DM manual.");
        }
        StringBuilder value = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            value.append((char) dataBuff[i]);
        }
        String normalized = value.toString().trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Resposta sem conteudo na leitura DM manual.");
        }
        return Integer.parseInt(normalized, 16) & 0xFFFF;
    }

    private static String toHexWord(int value) {
        return String.format("%04X", Integer.valueOf(value & 0xFFFF));
    }

    // persist the manual read values as production data, associating them to
    // production tags based on the defined rules, and using the PLC timestamp if
    // possible
    private void persistManualProductionFromManualRead(int nodeId, List<Integer> addressesRead,
            Map<Integer, Integer> valuesByAddress) {
        if (addressesRead == null || addressesRead.isEmpty() || valuesByAddress == null || valuesByAddress.isEmpty()) {
            return;
        }

        PlcConfig cfg = resolveManualPlcConfig(nodeId);
        if (cfg == null) {
            appendManualResult("Nao foi possivel identificar o device do node " + nodeId
                    + " para salvar os valores da leitura manual.");
            log("Leitura DM manual: node " + nodeId + " sem device configurado para persistencia.");
            return;
        }

        Map<String, ManualProductionTagBinding> tagBindings = loadManualProductionTagBindings(cfg.mnemonic);
        if (tagBindings.isEmpty()) {
            appendManualResult("Nenhuma TAG de producao encontrada no banco para o device " + cfg.mnemonic + ".");
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

        if (valuesInOrder.size() < MANUAL_DM_GROUP_SIZE) {
            appendManualResult(
                    "Leitura insuficiente para bloco de producao (" + valuesInOrder.size() + " palavra(s)).");
            return;
        }

        DeviceInfo deviceInfo = new DeviceInfo(cfg.mnemonic, cfg.title, cfg.description);
        List<MemoryValue> historyBatch = new ArrayList<>();
        List<MemoryValue> currentOnlyBatch = new ArrayList<>();

        int parsedBlocks = 0;
        for (int offset = 0; offset + MANUAL_DM_GROUP_SIZE - 1 < valuesInOrder.size();) {
            int markerAddress = addressesRead.get(offset).intValue();
            int markerValue = valuesInOrder.get(offset).intValue();
            if (!matchesManualDateMarker(markerAddress, markerValue)
                    && !matchesManualRelativeMarker(offset, markerValue)) {
                offset++;
                continue;
            }

            try {
                LocalDateTime timestamp = tryBuildManualTimestamp(valuesInOrder, offset);
                if (timestamp == null) {
                    offset++;
                    continue;
                }
                String concatenated = String.format(
                        "%04d%02d%02d%02d%02d%02d",
                        Integer.valueOf(timestamp.getYear()),
                        Integer.valueOf(timestamp.getMonthValue()),
                        Integer.valueOf(timestamp.getDayOfMonth()),
                        Integer.valueOf(timestamp.getHour()),
                        Integer.valueOf(timestamp.getMinute()),
                        Integer.valueOf(timestamp.getSecond()));

                appendManualResult("Bloco de producao detectado (offset " + offset + "): timestamp "
                        + timestamp.format(MANUAL_DM_DATE_TIME_FMT) + " [" + concatenated + "]");

                for (int i = 0; i < MANUAL_PRODUCTION_TAG_NAMES.length; i++) {
                    int valueIndex = offset + MANUAL_DM_TAG_VALUES_OFFSET + i;
                    if (valueIndex >= valuesInOrder.size()) {
                        break;
                    }
                    String tagName = MANUAL_PRODUCTION_TAG_NAMES[i];
                    int value = valuesInOrder.get(valueIndex).intValue() & 0xFFFF;
                    if (i == MANUAL_PRODUCTION_TAG_NAMES.length - 1
                            && valueIndex + 1 < valuesInOrder.size()) {
                        int possibleMarkerAddress = addressesRead.get(valueIndex).intValue();
                        int possibleMarkerValue = valuesInOrder.get(valueIndex).intValue();
                        if (matchesManualDateMarker(possibleMarkerAddress, possibleMarkerValue)
                                || matchesManualRelativeMarker(valueIndex, possibleMarkerValue)) {
                            continue;
                        }
                    }
                    ManualProductionTagBinding binding = tagBindings.get(tagName);
                    if (binding == null) {
                        appendManualResult("TAG " + tagName + " nao encontrada no banco para device " + cfg.mnemonic
                                + ". Valor lido=" + value + " ignorado.");
                        continue;
                    }

                    MemoryValue dbValue = new MemoryValue(binding.memoryName, value, timestamp);
                    if (binding.persistHistory) {
                        historyBatch.add(dbValue);
                    } else {
                        currentOnlyBatch.add(dbValue);
                    }

                    appendManualResult("TAG " + tagName + " -> DM " + binding.address + " = " + value
                            + " @ " + timestamp.format(MANUAL_DM_DATE_TIME_FMT));
                }

                parsedBlocks++;
                offset += MANUAL_DM_GROUP_SIZE;
            } catch (Exception ex) {
                offset++;
            }
        }

        if (parsedBlocks == 0) {
            appendManualResult("Nenhum bloco de producao valido encontrado pela regra de indices manuais.");
            log("Leitura DM manual: nenhum bloco de producao valido encontrado.");
            return;
        }

        ensureDb();
        if (!historyBatch.isEmpty()) {
            dmValueService.saveBatch(deviceInfo, historyBatch);
        }
        if (!currentOnlyBatch.isEmpty()) {
            dmValueService.saveBatchCurrentOnly(deviceInfo, currentOnlyBatch);
        }

        int totalSaved = historyBatch.size() + currentOnlyBatch.size();
        appendManualResult("Persistencia concluida: " + parsedBlocks + " bloco(s), " + totalSaved
                + " valor(es) salvo(s) em TAG/memory.");
        log("Leitura DM manual salva em banco para device " + cfg.mnemonic + ": blocos=" + parsedBlocks
                + ", historico=" + historyBatch.size() + ", current-only=" + currentOnlyBatch.size() + ".");
    }

    private Map<String, ManualProductionTagBinding> loadManualProductionTagBindings(String deviceMnemonic) {
        ensureDb();
        JdbcTemplate jdbc = dbContext.getBean(JdbcTemplate.class);

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < MANUAL_PRODUCTION_TAG_NAMES.length; i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }

        Object[] params = new Object[MANUAL_PRODUCTION_TAG_NAMES.length + 1];
        params[0] = deviceMnemonic;
        for (int i = 0; i < MANUAL_PRODUCTION_TAG_NAMES.length; i++) {
            params[i + 1] = MANUAL_PRODUCTION_TAG_NAMES[i];
        }

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.name AS tag_name, m.name AS memory_name, m.address AS memory_address, " +
                        "t.persist_history AS persist_history " +
                        "FROM public.device d " +
                        "JOIN public.machine ma ON ma.device_id = d.id " +
                        "JOIN public.tag t ON t.machine_id = ma.id " +
                        "JOIN public.memory m ON m.id = t.memory_id AND m.device_id = d.id " +
                        "WHERE d.mnemonic = ? AND t.name IN (" + placeholders + ")",
                params);

        Map<String, ManualProductionTagBinding> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String tagName = asString(row.get("tag_name"));
            String memoryName = asString(row.get("memory_name"));
            Integer address = asInt(row.get("memory_address"));
            boolean persistHistory = asBoolean(row.get("persist_history"), true);
            if (tagName == null || memoryName == null || memoryName.trim().isEmpty()
                    || address == null || address.intValue() < 0) {
                continue;
            }
            out.put(tagName, new ManualProductionTagBinding(memoryName, address.intValue(), persistHistory));
        }
        return out;
    }

    private PlcConfig resolveManualPlcConfig(int nodeId) {
        if (plcConfigs == null || plcConfigs.isEmpty()) {
            return null;
        }

        if (manualNodeCombo != null) {
            Object selected = manualNodeCombo.getSelectedItem();
            if (selected != null) {
                String label = selected.toString().trim();
                int marker = label.indexOf(" (node ");
                if (marker > 0) {
                    String mnemonic = label.substring(0, marker).trim();
                    for (PlcConfig cfg : plcConfigs) {
                        if (cfg.mnemonic.equalsIgnoreCase(mnemonic)) {
                            return cfg;
                        }
                    }
                }
            }
        }

        for (PlcConfig cfg : plcConfigs) {
            if (cfg.nodeId == nodeId) {
                return cfg;
            }
        }
        return null;
    }

    private static LocalDateTime tryBuildManualTimestamp(List<Integer> valuesInOrder, int offset) {
        LocalDateTime candidatePrimary = tryBuildManualTimestampCandidate(
                valuesInOrder,
                offset + 2,
                offset + 3,
                offset + 4,
                offset + 5,
                offset + 6,
                offset + 7);
        LocalDateTime candidateSecondary = tryBuildManualTimestampCandidate(
                valuesInOrder,
                offset + 1,
                offset + 2,
                offset + 3,
                offset + 4,
                offset + 5,
                offset + 6);
        return pickBestManualTimestamp(candidatePrimary, candidateSecondary);
    }

    private static LocalDateTime tryBuildManualTimestampCandidate(
            List<Integer> valuesInOrder,
            int yearIndex,
            int monthIndex,
            int dayIndex,
            int hourIndex,
            int minuteIndex,
            int secondIndex) {
        if (yearIndex < 0 || secondIndex >= valuesInOrder.size()) {
            return null;
        }

        int year = valuesInOrder.get(yearIndex).intValue();
        int month = valuesInOrder.get(monthIndex).intValue();
        int day = valuesInOrder.get(dayIndex).intValue();
        int hour = valuesInOrder.get(hourIndex).intValue();
        int minute = valuesInOrder.get(minuteIndex).intValue();
        int second = valuesInOrder.get(secondIndex).intValue();

        try {
            return buildManualTimestamp(year, month, day, hour, minute, second);
        } catch (Exception ignored) {
            // try BCD decoded values
        }

        int bcdYear = decodeBcdWord(year);
        int bcdMonth = decodeBcdWord(month);
        int bcdDay = decodeBcdWord(day);
        int bcdHour = decodeBcdWord(hour);
        int bcdMinute = decodeBcdWord(minute);
        int bcdSecond = decodeBcdWord(second);
        try {
            return buildManualTimestamp(bcdYear, bcdMonth, bcdDay, bcdHour, bcdMinute, bcdSecond);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime pickBestManualTimestamp(LocalDateTime candidateA, LocalDateTime candidateB) {
        if (candidateA == null) {
            return candidateB;
        }
        if (candidateB == null) {
            return candidateA;
        }

        int scoreA = scoreManualTimestamp(candidateA);
        int scoreB = scoreManualTimestamp(candidateB);
        return scoreB > scoreA ? candidateB : candidateA;
    }

    private static int scoreManualTimestamp(LocalDateTime value) {
        int year = value.getYear();
        int score = 0;
        if (year >= 2020 && year <= 2100) {
            score += 1000;
        }
        score -= Math.abs(year - LocalDateTime.now().getYear());
        return score;
    }

    @SuppressWarnings("unused")
    private static int findNextManualMarkerOffset(
            List<Integer> addressesRead,
            List<Integer> valuesInOrder,
            int startOffset) {
        for (int i = Math.max(0, startOffset); i < valuesInOrder.size(); i++) {
            int markerAddress = addressesRead.get(i).intValue();
            int markerValue = valuesInOrder.get(i).intValue();
            if (matchesManualDateMarker(markerAddress, markerValue)
                    || matchesManualRelativeMarker(i, markerValue)) {
                return i;
            }
        }
        return -1;
    }

    private static LocalDateTime buildManualTimestamp(int yearRaw, int month, int day, int hour, int minute,
            int second) {
        int year = normalizeManualYear(yearRaw);
        try {
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception ex) {
            if (day >= 1 && day <= 12 && month > 12) {
                return LocalDateTime.of(year, day, month, hour, minute, second);
            }
            throw ex;
        }
    }

    private static int decodeBcdWord(int raw) {
        int value = raw & 0xFFFF;
        int thousands = (value >> 12) & 0xF;
        int hundreds = (value >> 8) & 0xF;
        int tens = (value >> 4) & 0xF;
        int ones = value & 0xF;
        if (thousands > 9 || hundreds > 9 || tens > 9 || ones > 9) {
            return raw;
        }
        return (thousands * 1000) + (hundreds * 100) + (tens * 10) + ones;
    }

    private static int normalizeManualYear(int yearRaw) {
        if (yearRaw >= 0 && yearRaw < 100) {
            return 2000 + yearRaw;
        }
        return yearRaw;
    }

    @SuppressWarnings("unused")
    private void logManualDmDatesByMarker(List<Integer> addressesRead, Map<Integer, Integer> valuesByAddress) {
        if (addressesRead == null || addressesRead.isEmpty() || valuesByAddress == null || valuesByAddress.isEmpty()) {
            return;
        }
        boolean foundAny = false;
        for (Integer rawAddress : addressesRead) {
            int markerAddress = rawAddress.intValue();
            Integer markerValue = valuesByAddress.get(Integer.valueOf(markerAddress));
            if (markerValue == null || !matchesManualDateMarker(markerAddress, markerValue.intValue())) {
                continue;
            }
            int yearAddress = markerAddress + 1;
            int monthAddress = markerAddress + 2;
            int dayAddress = markerAddress + 3;
            int hourAddress = markerAddress + 4;
            int minuteAddress = markerAddress + 5;
            int secondAddress = markerAddress + 6;
            Integer year = valuesByAddress.get(Integer.valueOf(yearAddress));
            Integer month = valuesByAddress.get(Integer.valueOf(monthAddress));
            Integer day = valuesByAddress.get(Integer.valueOf(dayAddress));
            Integer hour = valuesByAddress.get(Integer.valueOf(hourAddress));
            Integer minute = valuesByAddress.get(Integer.valueOf(minuteAddress));
            Integer second = valuesByAddress.get(Integer.valueOf(secondAddress));
            if (year == null || month == null || day == null || hour == null || minute == null || second == null) {
                appendManualResult("Inicio de data detectado em DM " + markerAddress
                        + ", mas faltam dados ate DM " + (markerAddress + MANUAL_DM_DATE_PART_COUNT) + ".");
                log("Teste DM data/hora: inicio DM " + markerAddress + " incompleto para concatenacao.");
                continue;
            }
            try {
                LocalDateTime value = LocalDateTime.of(
                        year.intValue(),
                        month.intValue(),
                        day.intValue(),
                        hour.intValue(),
                        minute.intValue(),
                        second.intValue());
                String concatenated = String.format(
                        "%04d%02d%02d%02d%02d%02d",
                        Integer.valueOf(value.getYear()),
                        Integer.valueOf(value.getMonthValue()),
                        Integer.valueOf(value.getDayOfMonth()),
                        Integer.valueOf(value.getHour()),
                        Integer.valueOf(value.getMinute()),
                        Integer.valueOf(value.getSecond()));
                appendManualResult("Inicio de data detectado: DM " + markerAddress + " = " + markerAddress + ".");
                appendManualResult("Data/hora DM concatenada: " + concatenated);
                appendManualResult("Data/hora formatada para teste: " + value.format(MANUAL_DM_DATE_TIME_FMT));
                log("Teste DM data/hora (inicio DM " + markerAddress + "): " + concatenated
                        + " | formatada: " + value.format(MANUAL_DM_DATE_TIME_FMT));
                foundAny = true;
            } catch (Exception ex) {
                appendManualResult("Inicio de data em DM " + markerAddress + " com valores invalidos.");
                log("Teste DM data/hora: valores invalidos apos inicio DM " + markerAddress + ": "
                        + describeError(ex));
            }
        }
        if (!foundAny) {
            appendManualResult("Nenhum inicio de data encontrado pela regra DM X = X.");
            log("Teste DM data/hora: nenhum marcador DM X = X encontrado na leitura.");
        }
    }

    private static boolean matchesManualRelativeMarker(int markerIndex, int markerValue) {
        int normalizedValue = markerValue & 0xFFFF;
        if (normalizedValue == markerIndex) {
            return true;
        }
        String indexDigits = Integer.toString(markerIndex);
        if (indexDigits.length() <= 4) {
            int hexFromIndexDigits = Integer.parseInt(indexDigits, 16) & 0xFFFF;
            if (normalizedValue == hexFromIndexDigits) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesManualDateMarker(int markerAddress, int markerValue) {
        int normalizedValue = markerValue & 0xFFFF;
        if (normalizedValue == markerAddress) {
            return true;
        }

        String addressDigits = Integer.toString(markerAddress);
        if (addressDigits.length() <= 4) {
            int hexFromAddressDigits = Integer.parseInt(addressDigits, 16) & 0xFFFF;
            if (normalizedValue == hexFromAddressDigits) {
                return true;
            }
        }

        return false;
    }

    private void appendManualResult(String text) {
        runOnEdt(() -> {
            if (manualResultArea == null) {
                return;
            }
            String time = LocalDateTime.now().format(TIME_FMT);
            manualResultArea.append("[" + time + "] " + text + "\n");
            manualResultArea.setCaretPosition(manualResultArea.getDocument().getLength());
        });
    }

    private void setManualStatus(String text) {
        runOnEdt(() -> {
            if (manualStatusLabel != null) {
                manualStatusLabel.setText(text);
            }
        });
    }

    private void stopManualScanThread() {
        Thread running = manualScanThread;
        if (running == null) {
            return;
        }
        running.interrupt();
        try {
            running.join(1500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        manualScanThread = null;
        runOnEdt(() -> {
            if (manualScanButton != null) {
                manualScanButton.setEnabled(true);
            }
        });
        setManualStatus("PARADO");
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
        stopManualScanThread();
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

    // Logging to a file is synchronized to avoid interleaving lines from concurrent
    // log calls, but errors during logging are ignored to avoid breaking
    // application flow.
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

    // Data classes for manual production tag bindings and PLC configuration. These
    // are simple containers for related properties and do not contain behavior.
    private static final class ManualProductionTagBinding {
        private final String memoryName;
        private final int address;
        private final boolean persistHistory;

        // The constructor is private to enforce controlled creation through factory
        // methods or within the enclosing class, ensuring that all necessary properties
        // are provided and valid when instances are created.
        private ManualProductionTagBinding(String memoryName, int address, boolean persistHistory) {
            this.memoryName = memoryName;
            this.address = address;
            this.persistHistory = persistHistory;
        }
    }

    // The PlcConfig class encapsulates the configuration details for a PLC,
    // including its title, mnemonic, description, node ID, and the list of tags to
    // monitor. This structured approach allows for easy management and retrieval of
    // PLC configurations based on node IDs or other criteria.
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
