package org.omron.collector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import org.ctrl.comm.serial.SerialPortAbstract;
import org.omron.collector.service.DatabaseManager;
import org.omron.collector.service.LoggingService;
import org.omron.collector.service.ManualDmScanManager;
import org.omron.collector.service.PlcConfigurationLoader;
import org.omron.collector.service.SerialCommunicationManager;
import org.omron.collector.util.PlcNodeMonitorPanel;

public class CollectorMultPlcAplication {

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

    private JComboBox<String> manualNodeCombo;
    private JTextField manualStartAddressField;
    private JButton manualScanButton;
    private JTextArea manualResultArea;
    private JLabel manualStatusLabel;

    private final DatabaseManager dbManager = new DatabaseManager();
    private final LoggingService logger;
    private final SerialCommunicationManager serialManager;
    private final ManualDmScanManager manualDmScanManager;
    private final PlcConfigurationLoader configLoader;

    private final List<PlcNodeMonitorPanel> plcPanels = new ArrayList<>();
    private volatile boolean shuttingDown;
    private List<PlcConfigurationLoader.PlcConfiguration> plcConfigs = new ArrayList<>();

    public static void main(String[] args) {
        LoggingService.installGlobalHandler(null);
        SwingUtilities.invokeLater(() -> {
            CollectorMultPlcAplication app = new CollectorMultPlcAplication();
            app.frame.setVisible(true);
        });
    }

    public CollectorMultPlcAplication() {
        this.logger = new LoggingService(createDummyTextArea());
        this.serialManager = new SerialCommunicationManager(this::updateSerialStatus, logger::log);
        this.manualDmScanManager = new ManualDmScanManager(dbManager, logger, serialManager);
        this.configLoader = new PlcConfigurationLoader(dbManager);

        buildUi();
        logger.setLogArea(logArea);
    }

    private JTextArea createDummyTextArea() {
        JTextArea dummy = new JTextArea();
        dummy.setEditable(false);
        return dummy;
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

        plcConfigs = configLoader.loadAllConfigurations();
        JPanel container = new JPanel(new GridLayout(Math.max(1, plcConfigs.size()), 1, 4, 4));
        container.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        if (plcConfigs.isEmpty()) {
            logger.log("Nenhum device/tag DM encontrado no banco. Cadastre devices e tags para iniciar monitoramento.");
        }

        for (PlcConfigurationLoader.PlcConfiguration cfg : plcConfigs) {
            final int nodeId = cfg.nodeId;
            PlcNodeMonitorPanel panel = new PlcNodeMonitorPanel(
                    nodeId,
                    cfg.title,
                    cfg.mnemonic,
                    cfg.description,
                    cfg.nodeId,
                    cfg.tags,
                    () -> configLoader.loadTagsForDevice(cfg.mnemonic),
                    serialManager.getSharedSerial().getIoLock(),
                    serialManager::isConnected,
                    serialManager.getSharedSerial()::getHandler,
                    dbManager::initialize,
                    () -> dbManager.getDmValueService(),
                    // () -> dbManager.getRrValueService(),
                    logger::log,
                    () -> serialManager.requestDisconnectByUnresponsiveNode(nodeId));
            plcPanels.add(panel);
            container.add(panel.getPanel());
        }

        JTabbedPane centerTabs = new JTabbedPane();
        centerTabs.addTab("Monitoramento", container);
        centerTabs.addTab("Leitura DM", buildManualDmPanel());
        frame.add(centerTabs, BorderLayout.CENTER);
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
        SerialCommunicationManager.refreshAvailablePorts(portCombo, null, "COM1");

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

    private JPanel buildManualDmPanel() {
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

        rebuildManualNodeOptions();

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

        manualDmScanManager.setResultCallback(text -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                manualResultArea.append("[" + time + "] " + text + "\n");
                manualResultArea.setCaretPosition(manualResultArea.getDocument().getLength());
            });
        });

        manualDmScanManager.setStatusCallback(status -> {
            SwingUtilities.invokeLater(() -> {
                if (manualStatusLabel != null) {
                    manualStatusLabel.setText(status);
                }
                if (manualScanButton != null) {
                    manualScanButton.setEnabled(!"LENDO".equalsIgnoreCase(status));
                }
            });
        });

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(manualResultArea), BorderLayout.CENTER);
        return panel;
    }

    private void rebuildManualNodeOptions() {
        if (manualNodeCombo == null) {
            return;
        }

        manualNodeCombo.removeAllItems();
        if (plcConfigs != null) {
            for (PlcConfigurationLoader.PlcConfiguration cfg : plcConfigs) {
                String label = cfg.mnemonic + " (node " + cfg.nodeId + ")";
                manualNodeCombo.addItem(label);
            }
        }

        if (manualNodeCombo.getItemCount() == 0) {
            manualNodeCombo.addItem("0");
        }
        manualNodeCombo.setSelectedIndex(0);
    }

    private void connectSharedSerial() {
        if (serialManager.isConnected()) {
            logger.log("Comunicacao serial compartilhada ja esta ativa.");
            return;
        }

        String requestedPort = SerialCommunicationManager.getSelectedPortName(portCombo);
        if (requestedPort.isEmpty()) {
            updateSerialStatus("PORTA INVALIDA");
            logger.log("Selecione uma porta serial valida.");
            return;
        }

        try {
            SerialCommunicationManager.refreshAvailablePorts(portCombo, requestedPort, "COM1");
            if (!SerialCommunicationManager.isPortAvailable(requestedPort)) {
                updateSerialStatus("PORTA INEXISTENTE");
                logger.log("Porta serial " + requestedPort + " nao encontrada. Selecione uma porta disponivel.");
                return;
            }

            serialManager.connect(
                    requestedPort,
                    Integer.parseInt(baudCombo.getSelectedItem().toString()),
                    Integer.parseInt(dataBitsField.getText().trim()),
                    Integer.parseInt(stopBitsField.getText().trim()),
                    SerialPortAbstract.Parity.valueOf(parityCombo.getSelectedItem().toString()),
                    Integer.parseInt(timeoutField.getText().trim()),
                    rtsCheckBox.isSelected(),
                    dtrCheckBox.isSelected());

            dbManager.initialize();
            updateSerialStatus("CONECTADO");
            refreshNodeCommStatus();

        } catch (Exception ex) {
            updateSerialStatus("ERRO");
            logger.logError("Falha na conexao serial", ex);
            refreshNodeCommStatus();
        }
    }

    private void disconnectSharedSerial() {
        manualDmScanManager.stopScan();
        for (PlcNodeMonitorPanel panel : plcPanels) {
            panel.stopMonitor();
        }
        serialManager.disconnect();
        updateSerialStatus("DESCONECTADO");
        refreshNodeCommStatus();
    }

    private void refreshNodeCommStatus() {
        String shared = serialManager.isConnected() ? "CONECTADA" : "DESCONECTADA";
        for (PlcNodeMonitorPanel panel : plcPanels) {
            panel.setCommStatus(shared);
        }
    }

    private void startManualDmScan() {
        if (manualDmScanManager.isRunning()) {
            logger.log("Scanner DM manual ja esta em execucao.");
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
            updateManualStatus("PARAMETRO INVALIDO");
            logger.log("Parametros invalidos para scanner DM manual: " + ex.getMessage());
            return;
        }

        SwingUtilities.invokeLater(() -> {
            manualScanButton.setEnabled(false);
            if (manualResultArea != null) {
                manualResultArea.setText("");
            }
        });

        try {
            manualDmScanManager.startScan(nodeId, startAddress);
        } catch (IllegalStateException ex) {
            logger.log(ex.getMessage());
            manualScanButton.setEnabled(true);
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

        for (PlcConfigurationLoader.PlcConfiguration cfg : plcConfigs) {
            String label = cfg.mnemonic + " (node " + cfg.nodeId + ")";
            if (label.equals(raw)) {
                return cfg.nodeId;
            }
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

    private void updateSerialStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            if (serialStatusLabel != null) {
                serialStatusLabel.setText(status);
            }
        });
    }

    private void updateManualStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            if (manualStatusLabel != null) {
                manualStatusLabel.setText(status);
            }
        });
    }

    private void confirmAndShutdown() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Encerrar o Collector Multi PLC agora?",
                "Confirmar encerramento",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            logger.log("Encerramento solicitado pelo usuario.");
            shutdown();
        } else {
            logger.log("Encerramento cancelado.");
        }
    }

    private void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;

        manualDmScanManager.stopScan();
        disconnectSharedSerial();
        dbManager.close();

        if (frame != null) {
            frame.dispose();
        }
    }
}
