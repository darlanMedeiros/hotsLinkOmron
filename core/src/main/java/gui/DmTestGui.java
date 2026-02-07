package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.IStatusCode;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.service.DmValueService;
import org.ctrl.extras.Tag;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DmTestGui {

    private JFrame frame;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;

    private JTextField readAddressField;
    private JComboBox<String> readTypeCombo;

    private JTextField writeAddressField;
    private JTextField writeValuesField;
    private JComboBox<String> writeTypeCombo;
    private JComboBox<String> writeModeCombo;

    private JTextField pollMsField;
    private JTextField chunkSizeField;
    private JTextField delayMsField;

    private JTextArea logArea;
    private JLabel syncStatusLabel;
    private JTable dbTable;
    private DefaultTableModel dbTableModel;

    private SerialPortHandlerPjcImp comHandler;
    private ToolbusProtocol protocol;
    private IDevice plc;
    private IDeviceRegister deviceRegister;
    private DeviceInfo deviceInfo;
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private Thread syncThread;
    private volatile boolean syncRunning = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            DmTestGui window = new DmTestGui();
            window.frame.setVisible(true);
        });
    }

    public DmTestGui() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Teste DM - Omron Host Link");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(820, 620));
        frame.setLayout(new BorderLayout(10, 10));

        JPanel configPanel = buildConfigPanel();
        JPanel ioPanel = buildIoPanel();
        JPanel logPanel = buildLogPanel();
        JPanel dbPanel = buildDbPanel();

        frame.add(configPanel, BorderLayout.NORTH);
        frame.add(ioPanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(logPanel, BorderLayout.CENTER);
        bottom.add(dbPanel, BorderLayout.EAST);
        frame.add(bottom, BorderLayout.SOUTH);
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

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Porta"), c);
        c.gridx = 1;
        panel.add(portField, c);

        c.gridx = 2;
        panel.add(new JLabel("Baud"), c);
        c.gridx = 3;
        panel.add(baudField, c);

        c.gridx = 4;
        panel.add(new JLabel("Data bits"), c);
        c.gridx = 5;
        panel.add(dataBitsField, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Stop bits"), c);
        c.gridx = 1;
        panel.add(stopBitsField, c);

        c.gridx = 2;
        panel.add(new JLabel("Parity"), c);
        c.gridx = 3;
        panel.add(parityCombo, c);

        c.gridx = 4;
        panel.add(new JLabel("Timeout ms"), c);
        c.gridx = 5;
        panel.add(timeoutField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Node ID"), c);
        c.gridx = 1;
        panel.add(nodeField, c);

        c.gridx = 2;
        panel.add(new JLabel("Poll ms"), c);
        pollMsField = new JTextField("1500");
        c.gridx = 3;
        panel.add(pollMsField, c);

        c.gridx = 4;
        panel.add(new JLabel("Chunk"), c);
        chunkSizeField = new JTextField("10");
        c.gridx = 5;
        panel.add(chunkSizeField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Delay ms"), c);
        delayMsField = new JTextField("100");
        c.gridx = 1;
        panel.add(delayMsField, c);

        JButton connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connect());
        c.gridx = 2;
        c.gridy = 3;
        panel.add(connectButton, c);

        JButton disconnectButton = new JButton("Desconectar");
        disconnectButton.addActionListener(e -> disconnect());
        c.gridx = 3;
        panel.add(disconnectButton, c);

        JButton startSyncButton = new JButton("Start Sync");
        startSyncButton.addActionListener(e -> startSync());
        c.gridx = 4;
        panel.add(startSyncButton, c);

        JButton stopSyncButton = new JButton("Stop Sync");
        stopSyncButton.addActionListener(e -> stopSync());
        c.gridx = 5;
        panel.add(stopSyncButton, c);

        c.gridx = 0;
        c.gridy = 4;
        panel.add(new JLabel("Sync Status"), c);
        syncStatusLabel = new JLabel("STOPPED");
        c.gridx = 1;
        c.gridwidth = 3;
        panel.add(syncStatusLabel, c);
        c.gridwidth = 1;

        return panel;
    }

    private JPanel buildIoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Leitura / Escrita DM"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        readAddressField = new JTextField("0");
        readTypeCombo = new JComboBox<>(new String[] { "WORD", "DWORD" });

        writeAddressField = new JTextField("0");
        writeValuesField = new JTextField("10,11");
        writeTypeCombo = new JComboBox<>(new String[] { "WORD", "DWORD" });
        writeModeCombo = new JComboBox<>(new String[] { "BCD", "HEX" });

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Read addr"), c);
        c.gridx = 1;
        panel.add(readAddressField, c);
        c.gridx = 2;
        panel.add(new JLabel("Type"), c);
        c.gridx = 3;
        panel.add(readTypeCombo, c);
        JButton readButton = new JButton("Ler DM");
        readButton.addActionListener(e -> readDm());
        c.gridx = 4;
        panel.add(readButton, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Write addr"), c);
        c.gridx = 1;
        panel.add(writeAddressField, c);
        c.gridx = 2;
        panel.add(new JLabel("Valores"), c);
        c.gridx = 3;
        panel.add(writeValuesField, c);
        c.gridx = 4;
        panel.add(writeTypeCombo, c);
        c.gridx = 5;
        panel.add(writeModeCombo, c);
        JButton writeButton = new JButton("Escrever DM");
        writeButton.addActionListener(e -> writeDm());
        c.gridx = 6;
        panel.add(writeButton, c);

        return panel;
    }

    private JPanel buildDbPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("DB Values"));

        dbTableModel = new DefaultTableModel(new Object[] { "Address", "Value", "Updated" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        dbTable = new JTable(dbTableModel);
        JScrollPane scroll = new JScrollPane(dbTable);
        scroll.setPreferredSize(new Dimension(300, 180));
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh DB");
        refreshButton.addActionListener(e -> refreshDbTable());
        panel.add(refreshButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log"));
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void connect() {
        try {
            if (comHandler != null && comHandler.isStarted()) {
                log("Ja conectado.");
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
            log("Conectado em " + sp.getDevice() + " (" + baudField.getText().trim() + ").");
        } catch (Exception ex) {
            log("Erro ao conectar: " + ex.getMessage());
        }
    }

    private void disconnect() {
        if (comHandler != null) {
            try {
                stopSync();
                comHandler.stop();
                log("Desconectado.");
            } catch (Exception ex) {
                log("Erro ao desconectar: " + ex.getMessage());
            }
        }
    }

    private void ensureDevice() {
        int nodeId = Integer.parseInt(nodeField.getText().trim());
        if (plc == null || plc.getId() != nodeId) {
            plc = new DeviceImp(nodeId, "PLC", "PLC", "Omron PLC");
            deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
            deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());
        }
    }

    private void readDm() {
        try {
            connect();
            ensureDevice();
            ensureDb();

            int addr = Integer.parseInt(readAddressField.getText().trim());
            Tag tag = buildDmTag(addr, readTypeCombo.getSelectedItem().toString());

            AreaReadDM read = new AreaReadDM(plc, tag.toMemoryVariable());
            comHandler.send(read);

            IStatusCode status = read.getResponseStatusCode();
            log("READ DM tag=" + tag.getName() + " type=" + tag.getDataType()
                    + " reply=" + read.getReply()
                    + " status=" + (status == null ? "null" : status.getCode() + " - " + status.getDescription()));

            int[] values = parseReply(read.getReply(), tag.getLengthWords());
            if (values != null && values.length > 0) {
                dmValueService.saveRange(deviceInfo, addr, values);
                refreshDbTable();
            }
        } catch (ComException ex) {
            log("Erro leitura: " + ex.getMessage());
        } catch (Exception ex) {
            log("Erro leitura: " + ex.getMessage());
        }
    }

    private void writeDm() {
        try {
            connect();
            ensureDevice();

            int addr = Integer.parseInt(writeAddressField.getText().trim());
            int mode = writeModeCombo.getSelectedItem().toString().equals("BCD") ? MemoryWrite.BCD : MemoryWrite.HEX;
            Tag tag = buildDmTag(addr, writeTypeCombo.getSelectedItem().toString());
            int[] values = parseValues(writeValuesField.getText().trim(), mode == MemoryWrite.HEX);
            if (values.length != tag.getLengthWords()) {
                log("Quantidade de valores (" + values.length + ") diferente do tipo " + tag.getDataType()
                        + " (" + tag.getLengthWords() + ").");
                return;
            }

            AreaWriteDM write = new AreaWriteDM(plc, tag.toMemoryVariable(), values, mode);
            comHandler.send(write);

            IStatusCode status = write.getResponseStatusCode();
            log("WRITE DM tag=" + tag.getName() + " type=" + tag.getDataType()
                    + " values=" + writeValuesField.getText().trim()
                    + " mode=" + writeModeCombo.getSelectedItem().toString()
                    + " status=" + (status == null ? "null" : status.getCode() + " - " + status.getDescription()));
        } catch (ComException ex) {
            log("Erro escrita: " + ex.getMessage());
        } catch (Exception ex) {
            log("Erro escrita: " + ex.getMessage());
        }
    }

    private Tag buildDmTag(int address, String typeText) {
        String name = String.format("DM_%04d", address);
        if ("DWORD".equalsIgnoreCase(typeText)) {
            return Tag.dmDWord(name, address);
        }
        return Tag.dmWord(name, address);
    }

    private int[] parseValues(String text, boolean hexInput) {
        String[] parts = text.split("[,;\\s]+");
        List<Integer> values = new ArrayList<>();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            int v = hexInput ? Integer.parseInt(p.trim(), 16) : Integer.parseInt(p.trim());
            values.add(v);
        }
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void refreshDbTable() {
        if (dmValueService == null || deviceInfo == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            dbTableModel.setRowCount(0);
            List<DmValue> rows = dmValueService.getRange(deviceInfo, 0, 1000);
            for (DmValue row : rows) {
                dbTableModel.addRow(new Object[] {
                        row.getAddress(),
                        row.getValue(),
                        row.getUpdatedAt()
                });
            }
        });
    }

    private void startSync() {
        if (syncRunning) {
            log("Sync ja em execucao.");
            return;
        }
        try {
            connect();
            ensureDevice();
            ensureDb();
        } catch (Exception ex) {
            log("Erro ao iniciar sync: " + ex.getMessage());
            return;
        }

        syncRunning = true;
        final int pollMs = parseInt(pollMsField.getText().trim(), 1500);
        final int chunkSize = parseInt(chunkSizeField.getText().trim(), 10);
        final int delayMs = parseInt(delayMsField.getText().trim(), 100);

        syncThread = new Thread(() -> {
            setSyncStatus("RUNNING");
            int[] lastValues = new int[11];
            for (int i = 0; i < lastValues.length; i++) {
                lastValues[i] = Integer.MIN_VALUE;
            }
            while (syncRunning) {
                try {
                    pollDmRange(0, 10, chunkSize, delayMs, lastValues);
                    Thread.sleep(pollMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log("Erro no sync: " + ex.getMessage());
                }
            }
            setSyncStatus("STOPPED");
        }, "dm-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void stopSync() {
        syncRunning = false;
        if (syncThread != null) {
            syncThread.interrupt();
            syncThread = null;
        }
        if (dbContext != null) {
            dbContext.close();
            dbContext = null;
            dmValueService = null;
        }
        setSyncStatus("STOPPED");
    }

    private void ensureDb() {
        if (dbContext == null) {
            dbContext = new AnnotationConfigApplicationContext(DbConfig.class);
            dmValueService = dbContext.getBean(DmValueService.class);
            if (plc != null) {
                dmValueService.setDeviceInfo("PLC", plc.getName(), plc.getDescription());
            }
        }
    }

    private void pollDmRange(int startAddr, int endAddr, int chunkSize, int delayMs, int[] lastValues) {
        int addr = startAddr;
        while (addr <= endAddr) {
            int remaining = endAddr - addr + 1;
            int length = Math.min(chunkSize, remaining);
            AreaReadDM read = new AreaReadDM(plc, addr, length);
            comHandler.send(read);

            int[] values = parseReply(read.getReply(), length);
            if (values != null) {
                List<DmValue> changed = new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    int currentAddr = addr + i;
                    int index = currentAddr - startAddr;
                    int currentVal = values[i];
                    if (lastValues[index] != currentVal) {
                        lastValues[index] = currentVal;
                        changed.add(new DmValue(currentAddr, currentVal, null));
                    }
                }
                if (!changed.isEmpty()) {
                    dmValueService.saveBatch(deviceInfo, changed);
                    log("Sync: " + changed.size() + " valores atualizados.");
                }
            }
            addr += length;
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
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
            String val = "";
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (idx < dataBuff.length) {
                    val = val + (char) dataBuff[idx];
                }
            }
            try {
                out[i] = Integer.parseInt(val.trim(), 16);
            } catch (NumberFormatException ex) {
                out[i] = 0;
            }
        }
        return out;
    }

    private void setSyncStatus(String status) {
        SwingUtilities.invokeLater(() -> syncStatusLabel.setText(status));
    }
}
