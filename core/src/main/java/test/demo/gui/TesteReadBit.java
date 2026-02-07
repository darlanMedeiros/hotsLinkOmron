package test.demo.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.IStatusCode;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.RrValueService;
import org.ctrl.utils.OmronUtils;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.ToolbusWordBit;
import org.ctrl.vend.omron.toolbus.commands.AreaReadRR;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TesteReadBit {

    private JFrame frame;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;
    private JTextField intervalField;
    private JTextField addressField;

    private JLabel[] bitLabels = new JLabel[8];
    private JLabel statusLabel;
    private JLabel dbStatusLabel;

    private SerialPortHandlerPjcImp comHandler;
    private ToolbusProtocol protocol;
    private IDevice plc;
    private IDeviceRegister deviceRegister;
    private Timer pollTimer;
    private AnnotationConfigApplicationContext ctx;
    private RrValueService rrValueService;

    private final OmronUtils utils = new OmronUtils();
    private final ToolbusWordBit wordBit = new ToolbusWordBit();

    private static final String DEVICE_MNEMONIC = "PLC";
    private boolean[] lastBits = new boolean[8];

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TesteReadBit window = new TesteReadBit();
            window.frame.setVisible(true);
        });
    }

    public TesteReadBit() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Teste Read Bit RR 10.00-10.07");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(600, 320));
        frame.setLayout(new BorderLayout(10, 10));

        JPanel configPanel = buildConfigPanel();
        JPanel bitsPanel = buildBitsPanel();
        JPanel controlPanel = buildControlPanel();

        frame.add(configPanel, BorderLayout.NORTH);
        frame.add(bitsPanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Conexao"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        portField = new JTextField("COM2");
        baudField = new JTextField("9600");
        dataBitsField = new JTextField("7");
        stopBitsField = new JTextField("2");
        timeoutField = new JTextField("10000");
        nodeField = new JTextField("0");
        parityCombo = new JComboBox<>(new String[] { "EVEN", "NONE", "ODD" });
        intervalField = new JTextField("500");
        addressField = new JTextField("10");

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Porta"), c);
        c.gridx = 1;
        panel.add(portField, c);

        c.gridx = 2;
        panel.add(new JLabel("Baud"), c);
        c.gridx = 3;
        panel.add(baudField, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Data bits"), c);
        c.gridx = 1;
        panel.add(dataBitsField, c);

        c.gridx = 2;
        panel.add(new JLabel("Stop bits"), c);
        c.gridx = 3;
        panel.add(stopBitsField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Parity"), c);
        c.gridx = 1;
        panel.add(parityCombo, c);

        c.gridx = 2;
        panel.add(new JLabel("Timeout ms"), c);
        c.gridx = 3;
        panel.add(timeoutField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Node ID"), c);
        c.gridx = 1;
        panel.add(nodeField, c);

        c.gridx = 2;
        panel.add(new JLabel("Interval ms"), c);
        c.gridx = 3;
        panel.add(intervalField, c);

        c.gridx = 0;
        c.gridy = 4;
        panel.add(new JLabel("Endereco base"), c);
        c.gridx = 1;
        panel.add(addressField, c);

        return panel;
    }

    private JPanel buildBitsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("RR bits"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        int baseAddr = getBaseAddress();
        for (int i = 0; i < 8; i++) {
            c.gridx = i % 4;
            c.gridy = i / 4;
            JPanel bitPanel = new JPanel(new BorderLayout());
            bitPanel.setBorder(BorderFactory.createTitledBorder(formatBitLabel(baseAddr, i)));
            JLabel label = new JLabel("OFF", JLabel.CENTER);
            label.setOpaque(true);
            label.setBackground(java.awt.Color.LIGHT_GRAY);
            bitPanel.add(label, BorderLayout.CENTER);
            panel.add(bitPanel, c);
            bitLabels[i] = label;
        }

        return panel;
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Controle"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connect());
        JButton startButton = new JButton("Iniciar");
        startButton.addActionListener(e -> startPolling());
        JButton stopButton = new JButton("Parar");
        stopButton.addActionListener(e -> stopPolling());

        c.gridx = 0;
        c.gridy = 0;
        panel.add(connectButton, c);
        c.gridx = 1;
        panel.add(startButton, c);
        c.gridx = 2;
        panel.add(stopButton, c);

        statusLabel = new JLabel("Status: parado");
        dbStatusLabel = new JLabel("DB: sem gravação");
        c.gridx = 3;
        panel.add(statusLabel, c);
        c.gridx = 4;
        panel.add(dbStatusLabel, c);

        return panel;
    }

    private void connect() {
        try {
            if (comHandler != null && comHandler.isStarted()) {
                setStatus("Ja conectado.");
                return;
            }

            SerialParameters sp = new SerialParameters();
            sp.setDevice(portField.getText().trim());
            sp.setBaudRate(SerialPort.BaudRate.getBaudRate(Integer.parseInt(baudField.getText().trim())));
            sp.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
            sp.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
            sp.setParity(SerialPort.Parity.valueOf(parityCombo.getSelectedItem().toString()));

            SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());
            comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));

            int timeout = Integer.parseInt(timeoutField.getText().trim());
            if (comHandler instanceof IComControl) {
                ((IComControl) comHandler).setCommunicationTimeOut(timeout);
            }

            comHandler.initialize();
            protocol = new ToolbusProtocol();
            comHandler.setProtocolHandler(protocol);
            comHandler.start();

            ensureDevice();
            ensureDb();
            setStatus("Conectado em " + sp.getDevice() + ".");
        } catch (Exception ex) {
            setStatus("Erro ao conectar: " + ex.getMessage());
        }
    }

    private void ensureDevice() {
        int nodeId = Integer.parseInt(nodeField.getText().trim());
        if (plc == null || plc.getId() != nodeId) {
            plc = new DeviceImp(nodeId, DEVICE_MNEMONIC, "PLC", "Omron PLC");
            deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
        }
    }

    private void ensureDb() {
        if (ctx == null) {
            ctx = new AnnotationConfigApplicationContext(DbConfig.class);
            rrValueService = ctx.getBean(RrValueService.class);
        }
    }

    private void startPolling() {
        connect();
        ensureDevice();
        updateBitTitles();

        int intervalMs;
        try {
            intervalMs = Integer.parseInt(intervalField.getText().trim());
        } catch (NumberFormatException ex) {
            intervalMs = 500;
        }

        if (pollTimer != null && pollTimer.isRunning()) {
            pollTimer.stop();
        }

        pollTimer = new Timer(intervalMs, e -> pollOnce());
        pollTimer.start();
        setStatus("Leitura ciclica iniciada.");
    }

    private void stopPolling() {
        if (pollTimer != null) {
            pollTimer.stop();
        }
        if (ctx != null) {
            ctx.close();
            ctx = null;
            rrValueService = null;
        }
        setStatus("Leitura ciclica parada.");
    }

    private void pollOnce() {
        try {
            if (comHandler == null || !comHandler.isStarted()) {
                setStatus("Nao conectado.");
                return;
            }
            int baseAddr = getBaseAddress();
            AreaReadRR read = new AreaReadRR(plc, baseAddr, 1);
            comHandler.send(read);

            String reply = read.getReply() == null ? null : read.getReply().toString();
            IStatusCode status = read.getResponseStatusCode();
            if (reply == null) {
                setStatus("Resposta RR nula.");
                return;
            }

            String baseHex = utils.getFormateHexWrite(utils.convertToHexDec16(reply));
            wordBit.setWorldBits(baseAddr, baseHex);
            for (int i = 0; i < 8; i++) {
                boolean on = wordBit.getWordBit(i);
                updateBitLabel(i, on);
                saveIfChanged(baseAddr, i, on);
            }

            setStatus("OK (" + (status == null ? "null" : status.getCode()) + ")");
        } catch (ComException ex) {
            setStatus("Erro leitura: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro leitura: " + ex.getMessage());
        }
    }

    private void updateBitLabel(int bit, boolean on) {
        JLabel label = bitLabels[bit];
        SwingUtilities.invokeLater(() -> {
            label.setText(on ? "ON" : "OFF");
            label.setBackground(on ? java.awt.Color.GREEN : java.awt.Color.LIGHT_GRAY);
        });
    }

    private void saveIfChanged(int address, int bit, boolean current) {
        if (rrValueService == null) {
            return;
        }
        boolean previous = lastBits[bit];
        if (current != previous) {
            lastBits[bit] = current;
            DeviceInfo device = new DeviceInfo(DEVICE_MNEMONIC, plc.getName(), plc.getDescription());
            rrValueService.saveValue(device, address, bit, current);
            setDbStatus("gravado " + formatBitLabel(address, bit) + "=" + (current ? "1" : "0"));
        }
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + msg));
    }

    private void setDbStatus(String msg) {
        SwingUtilities.invokeLater(() -> dbStatusLabel.setText("DB: " + msg));
    }

    private int getBaseAddress() {
        try {
            return Integer.parseInt(addressField.getText().trim());
        } catch (NumberFormatException ex) {
            return 10;
        }
    }

    private String formatBitLabel(int address, int bit) {
        return String.format("%d.%02d", address, bit);
    }

    private void updateBitTitles() {
        int baseAddr = getBaseAddress();
        for (int i = 0; i < 8; i++) {
            java.awt.Container parent = bitLabels[i].getParent();
            if (parent instanceof JPanel) {
                ((JPanel) parent).setBorder(BorderFactory.createTitledBorder(formatBitLabel(baseAddr, i)));
            }
        }
        frame.repaint();
    }
}
