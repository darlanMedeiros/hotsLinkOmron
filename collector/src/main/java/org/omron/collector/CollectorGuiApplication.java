package org.omron.collector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPort;
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

public class CollectorGuiApplication {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int INTER_TAG_DELAY_MS = 1000;
    private static final int ERROR_RETRY_DELAY_MS = 1500;
    private static final int MAX_ERROR_RETRY_DELAY_MS = 15000;
    private static final int MAX_LOG_LINES = 3000;
    private static final int HISTORY_RETENTION_DAYS = 14;
    private static final int HISTORY_PRUNE_INTERVAL_CYCLES = 500;
    private static final Path LOG_FILE = Path.of("collector-gui.log");
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
    private JComboBox<String> portCombo;
    private JComboBox<String> baudCombo;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;
    private JTextField pollMsField;
    private JLabel commStatusLabel;
    private JLabel monitorStatusLabel;
    private JTextArea logArea;

    private volatile boolean monitoring;
    private Thread monitorThread;
    private SerialPortHandlerImp comHandler;
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private TagService tagService;
    private IDevice plc;
    private DeviceInfo deviceInfo;
    private volatile boolean shuttingDown;

    public static void main(String[] args) {
        installGlobalExceptionHandler();
        SwingUtilities.invokeLater(() -> {
            CollectorGuiApplication app = new CollectorGuiApplication();
            app.frame.setVisible(true);
        });
    }

    public CollectorGuiApplication() {
        buildUi();
    }

    private void buildUi() {
        frame = new JFrame("Collector TAG Monitor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(new Dimension(880, 520));
        frame.setLayout(new BorderLayout(10, 10));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndShutdown();
            }
        });

        frame.add(buildSerialPanel(), BorderLayout.NORTH);
        frame.add(buildMonitorPanel(), BorderLayout.CENTER);
        frame.add(buildLogPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSerialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Comunicacao serial"));

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
        nodeField = new JTextField("0");
        timeoutField = new JTextField("10000");
        pollMsField = new JTextField("2000");

        int row = 0;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Porta"), c);
        c.gridx = 1;
        panel.add(portCombo, c);
        refreshAvailablePorts("COM2");

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
        panel.add(new JLabel("Node ID"), c);
        c.gridx = 5;
        panel.add(nodeField, c);

        row++;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Timeout (ms)"), c);
        c.gridx = 1;
        panel.add(timeoutField, c);

        c.gridx = 2;
        panel.add(new JLabel("Poll (ms)"), c);
        c.gridx = 3;
        panel.add(pollMsField, c);

        JButton connectButton = new JButton("Conectar CLP");
        connectButton.addActionListener(e -> connect());
        c.gridx = 4;
        panel.add(connectButton, c);

        JButton disconnectButton = new JButton("Desconectar");
        disconnectButton.addActionListener(e -> disconnect());
        c.gridx = 5;
        panel.add(disconnectButton, c);

        return panel;
    }

    private JPanel buildMonitorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Monitoramento TAGs (DWORD)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("TAGs monitoradas"), c);
        c.gridx = 1;
        panel.add(new JLabel(Integer.toString(MONITORED_TAGS.length)), c);

        c.gridx = 2;
        panel.add(new JLabel("Modo"), c);
        c.gridx = 3;
        panel.add(new JLabel("Leitura por TAG (DM DWORD)"), c);

        JButton startButton = new JButton("Iniciar monitor");
        startButton.addActionListener(e -> startMonitor());
        c.gridx = 0;
        c.gridy = 1;
        panel.add(startButton, c);

        JButton stopButton = new JButton("Parar monitor");
        stopButton.addActionListener(e -> stopMonitor());
        c.gridx = 1;
        panel.add(stopButton, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Estado comunicacao"), c);
        commStatusLabel = new JLabel("DESCONECTADO");
        c.gridx = 1;
        c.gridwidth = 3;
        panel.add(commStatusLabel, c);
        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Estado monitor"), c);
        monitorStatusLabel = new JLabel("PARADO");
        c.gridx = 1;
        c.gridwidth = 3;
        panel.add(monitorStatusLabel, c);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log"));
        logArea = new JTextArea(10, 80);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    // Foca na logica de comunicacao e monitoramento, mantendo a interface simples e
    // responsiva.
    private void connect() {
        if (isConnected()) {
            log("Comunicacao ja esta ativa.");
            return;
        }

        String requestedPort = getSelectedPortName();
        refreshAvailablePorts(requestedPort);
        requestedPort = getSelectedPortName();
        if (requestedPort.isEmpty()) {
            log("Selecione uma porta serial valida.");
            setCommStatus("PORTA INVALIDA");
            return;
        }

        try {
            SerialParameters sp = new SerialParameters();
            sp.setDevice(requestedPort);
            sp.setBaudRate(SerialPort.BaudRate.getBaudRate(Integer.parseInt(baudCombo.getSelectedItem().toString())));
            sp.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
            sp.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
            sp.setParity(SerialPort.Parity.valueOf(parityCombo.getSelectedItem().toString()));

            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            comHandler = new SerialPortHandlerImp(SerialUtils.createSerial(sp));
            comHandler.setProtocolHandler(new ToolbusProtocol());
            if (comHandler instanceof IComControl) {
                int timeout = Integer.parseInt(timeoutField.getText().trim());
                ((IComControl) comHandler).setCommunicationTimeOut(timeout);
            }

            comHandler.initialize();
            comHandler.start();

            ensureDevice();
            ensureDb();

            setCommStatus("CONECTADO");
            log("Conectado ao CLP na porta " + sp.getDevice() + ".");
        } catch (Exception ex) {
            if (isSerialPortInUse(ex)) {
                setCommStatus("PORTA EM USO");
                log("Porta serial " + requestedPort + " em uso por outro processo.");
                showPortInUseDialog(requestedPort);
            } else {
                setCommStatus("ERRO");
                log("Falha na conexao: " + ex.getMessage());
            }
            safeStopHandler();
        }
    }

    private void disconnect() {
        stopMonitor();
        safeStopHandler();
        closeDb();
        setCommStatus("DESCONECTADO");
        log("Comunicacao encerrada.");
    }

    private void startMonitor() {
        if (monitoring) {
            log("Monitor ja esta em execucao.");
            return;
        }
        if (!isConnected()) {
            log("Conecte ao CLP antes de iniciar o monitor.");
            return;
        }
        ensureDb();
        ensureTagBindings();

        final int pollMs;
        try {
            pollMs = Integer.parseInt(pollMsField.getText().trim());
            if (pollMs < 100) {
                throw new IllegalArgumentException("Poll deve ser >= 100ms.");
            }
        } catch (Exception ex) {
            log("Parametros invalidos: " + ex.getMessage());
            return;
        }

        monitoring = true;
        monitorThread = new Thread(() -> runMonitorLoop(pollMs), "collector-tag-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void runMonitorLoop(int pollMs) {
        setMonitorStatus("RODANDO");
        log("Monitor iniciado para " + MONITORED_TAGS.length + " TAGs.");

        Map<String, int[]> lastValues = new LinkedHashMap<>();
        int cycleCount = 0;
        int consecutiveErrors = 0;

        while (monitoring) {
            try {
                for (Tag tag : MONITORED_TAGS) {
                    if (!monitoring) {
                        break;
                    }
                    AreaReadDM read = new AreaReadDM(plc, tag.toMemoryVariable());
                    comHandler.send(read);

                    int[] values = parseReply(read.getReply(), tag.getLengthWords());
                    if (values == null) {
                        log("Leitura invalida para TAG " + tag.getName() + ".");
                    } else {
                        int[] previous = lastValues.get(tag.getName());
                        if (hasChanged(previous, values)) {
                            dmValueService.saveRange(deviceInfo, tag.getAddress(), values);
                            log("Alteracao salva: TAG " + tag.getName() + " (DM " + tag.getAddress()
                                    + ".." + (tag.getAddress() + tag.getLengthWords() - 1) + ") = "
                                    + formatWords(values) + ".");
                            lastValues.put(tag.getName(), copyWords(values));
                        }
                    }

                    // Evita rajadas de consultas na serial e reduz erros de checksum.
                    Thread.sleep(INTER_TAG_DELAY_MS);
                }

                setCommStatus("CONECTADO - ultimo ciclo " + LocalDateTime.now().format(TIME_FMT));
                cycleCount++;
                consecutiveErrors = 0;
                if (cycleCount % HISTORY_PRUNE_INTERVAL_CYCLES == 0) {
                    int deletedRows = dmValueService.pruneHistoryOlderThanDays(HISTORY_RETENTION_DAYS);
                    if (deletedRows > 0) {
                        log("Limpeza de historico: " + deletedRows
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
                log("Erro no monitoramento: " + ex.getMessage());
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
        log("Monitor finalizado.");
    }

    private void stopMonitor() {
        monitoring = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
        setMonitorStatus("PARADO");
    }

    private void ensureDb() {
        if (dbContext == null) {
            dbContext = new AnnotationConfigApplicationContext(DbConfig.class);
            dmValueService = dbContext.getBean(DmValueService.class);
            tagService = dbContext.getBean(TagService.class);
        }
    }

    private void closeDb() {
        if (dbContext != null) {
            dbContext.close();
            dbContext = null;
            dmValueService = null;
            tagService = null;
        }
    }

    private void ensureDevice() {
        int nodeId = Integer.parseInt(nodeField.getText().trim());
        if (plc == null || plc.getId() != nodeId) {
            plc = new DeviceImp(nodeId, "PLC", "PLC", "Omron PLC");
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
            deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());
        }
    }

    private boolean isConnected() {
        return comHandler != null && comHandler.isStarted();
    }

    private void safeStopHandler() {
        if (comHandler != null) {
            try {
                comHandler.stop();
            } catch (Exception ex) {
                log("Erro ao parar comunicacao: " + ex.getMessage());
            } finally {
                comHandler = null;
            }
        }
    }

    private void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        stopMonitor();
        safeStopHandler();
        closeDb();
        if (frame != null) {
            frame.dispose();
        }
    }

    private void confirmAndShutdown() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Encerrar o Collector agora?",
                "Confirmar encerramento",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            log("Encerramento solicitado pelo usuario.");
            shutdown();
        } else {
            log("Encerramento cancelado.");
        }
    }

    private void ensureTagBindings() {
        if (tagService == null || deviceInfo == null) {
            return;
        }
        for (Tag tag : MONITORED_TAGS) {
            tagService.getOrCreateDmTag(deviceInfo, tag.getName(), tag.getAddress());
        }
    }

    private int[] parseReply(IData reply, int length) {
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

    private void setCommStatus(String text) {
        runOnEdt(() -> commStatusLabel.setText(text));
    }

    private void setMonitorStatus(String text) {
        runOnEdt(() -> monitorStatusLabel.setText(text));
    }

    private boolean hasChanged(int[] previous, int[] current) {
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

    private int[] copyWords(int[] values) {
        int[] copy = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            copy[i] = values[i];
        }
        return copy;
    }

    private String formatWords(int[] words) {
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
            String time = LocalDateTime.now().format(TIME_FMT);
            logArea.append("[" + time + "] " + message + "\n");
            trimLogIfNeeded();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        SwingUtilities.invokeLater(task);
    }

    private void refreshAvailablePorts(String preferredPort) {
        String selected = (preferredPort == null || preferredPort.trim().isEmpty()) ? getSelectedPortName() : preferredPort.trim();
        try {
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            List<String> ports = SerialUtils.getPortIdentifiers();
            portCombo.removeAllItems();
            for (String port : ports) {
                portCombo.addItem(port);
            }
            if (selected == null || selected.isEmpty()) {
                if (ports.contains("COM2")) {
                    selected = "COM2";
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
            String message = "Nao foi possivel listar portas automaticamente: " + ex.getMessage();
            if (logArea != null) {
                log(message);
            } else {
                appendPersistentLog(message, null);
            }
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

    private boolean isSerialPortInUse(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            if ("PortInUseException".equals(className)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("in use") || normalized.contains("em uso")) {
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

    private void trimLogIfNeeded() {
        int lineCount = logArea.getLineCount();
        if (lineCount <= MAX_LOG_LINES) {
            return;
        }
        int linesToRemove = lineCount - MAX_LOG_LINES;
        try {
            int endOffset = logArea.getLineEndOffset(linesToRemove - 1);
            logArea.replaceRange("", 0, endOffset);
        } catch (BadLocationException ex) {
            // Keep monitor running even if GUI line offsets temporarily mismatch.
        }
    }

    private static void installGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            String message = "Uncaught exception thread=" + thread.getName() + ": "
                    + error.getClass().getSimpleName() + " - " + error.getMessage();
            appendPersistentLog(message, error);
        });
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
            // Logging should never interrupt application flow.
        }
    }
}
