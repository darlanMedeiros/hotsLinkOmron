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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.IStatusCode;
import org.ctrl.utils.OmronUtils;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.ToolbusWordBit;
import org.ctrl.vend.omron.toolbus.commands.AreaReadRR;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteWR;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

public class RrWrBitGui {

    private JFrame frame;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;

    private JTextField readAddressField;
    private JTextField readLengthField;

    private JTextField lastHexField;
    private JTextField lastDecField;
    private JTextField lastBinField;

    private JTextField writeAddressField;
    private JTextField writeBitField;
    private JComboBox<String> writeBitValueCombo;
    private JComboBox<String> writeModeCombo;

    private JTextArea logArea;

    private SerialPortHandlerPjcImp comHandler;
    private ToolbusProtocol protocol;
    private IDevice plc;
    private IDeviceRegister deviceRegister;

    private final OmronUtils utils = new OmronUtils();
    private final ToolbusWordBit wordBit = new ToolbusWordBit();

    private Integer lastReadAddress = null;
    private String lastReadHex = null;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            RrWrBitGui window = new RrWrBitGui();
            window.frame.setVisible(true);
        });
    }

    public RrWrBitGui() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Teste RR/WR Bits - Omron Host Link");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(780, 560));
        frame.setLayout(new BorderLayout(10, 10));

        JPanel configPanel = buildConfigPanel();
        JPanel ioPanel = buildIoPanel();
        JPanel logPanel = buildLogPanel();

        frame.add(configPanel, BorderLayout.NORTH);
        frame.add(ioPanel, BorderLayout.CENTER);
        frame.add(logPanel, BorderLayout.SOUTH);
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

        JButton connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connect());
        c.gridx = 2;
        panel.add(connectButton, c);

        JButton disconnectButton = new JButton("Desconectar");
        disconnectButton.addActionListener(e -> disconnect());
        c.gridx = 3;
        panel.add(disconnectButton, c);

        return panel;
    }

    private JPanel buildIoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Leitura RR / Escrita WR (Bits)"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        readAddressField = new JTextField("10");
        readLengthField = new JTextField("1");

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("RR addr"), c);
        c.gridx = 1;
        panel.add(readAddressField, c);
        c.gridx = 2;
        panel.add(new JLabel("Len"), c);
        c.gridx = 3;
        panel.add(readLengthField, c);

        JButton readButton = new JButton("Ler RR");
        readButton.addActionListener(e -> readRr());
        c.gridx = 4;
        panel.add(readButton, c);

        lastHexField = new JTextField();
        lastHexField.setEditable(false);
        lastDecField = new JTextField();
        lastDecField.setEditable(false);
        lastBinField = new JTextField();
        lastBinField.setEditable(false);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Last HEX"), c);
        c.gridx = 1;
        panel.add(lastHexField, c);
        c.gridx = 2;
        panel.add(new JLabel("Last DEC"), c);
        c.gridx = 3;
        panel.add(lastDecField, c);
        c.gridx = 4;
        panel.add(new JLabel("Last BIN"), c);
        c.gridx = 5;
        panel.add(lastBinField, c);

        writeAddressField = new JTextField("10");
        writeBitField = new JTextField("0");
        writeBitValueCombo = new JComboBox<>(new String[] { "1", "0" });
        writeModeCombo = new JComboBox<>(new String[] { "BCD", "HEX" });

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("WR addr"), c);
        c.gridx = 1;
        panel.add(writeAddressField, c);
        c.gridx = 2;
        panel.add(new JLabel("Bit (0-15)"), c);
        c.gridx = 3;
        panel.add(writeBitField, c);
        c.gridx = 4;
        panel.add(new JLabel("Valor"), c);
        c.gridx = 5;
        panel.add(writeBitValueCombo, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Mode"), c);
        c.gridx = 1;
        panel.add(writeModeCombo, c);

        JButton writeButton = new JButton("Escrever WR");
        writeButton.addActionListener(e -> writeWrBit());
        c.gridx = 2;
        panel.add(writeButton, c);

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
        }
    }

    private void readRr() {
        try {
            connect();
            ensureDevice();

            int addr = Integer.parseInt(readAddressField.getText().trim());
            int len = Integer.parseInt(readLengthField.getText().trim());

            AreaReadRR read = new AreaReadRR(plc, addr, len);
            comHandler.send(read);

            String reply = read.getReply() == null ? null : read.getReply().toString();
            IStatusCode status = read.getResponseStatusCode();

            if (reply != null) {
                int dec = utils.convertToHexDec16(reply);
                String bin = utils.getFormatedBinary(reply);
                lastHexField.setText(reply);
                lastDecField.setText(Integer.toString(dec));
                lastBinField.setText(bin);
                lastReadAddress = addr;
                lastReadHex = reply;
            }

            log("READ RR addr=" + addr + " len=" + len + " reply=" + reply
                    + " status=" + (status == null ? "null" : status.getCode() + " - " + status.getDescription()));
        } catch (ComException ex) {
            log("Erro leitura: " + ex.getMessage());
        } catch (Exception ex) {
            log("Erro leitura: " + ex.getMessage());
        }
    }

    private void writeWrBit() {
        try {
            connect();
            ensureDevice();

            int addr = Integer.parseInt(writeAddressField.getText().trim());
            int bit = Integer.parseInt(writeBitField.getText().trim());
            if (bit < 0 || bit > 15) {
                log("Bit deve estar entre 0 e 15.");
                return;
            }

            boolean bitValue = writeBitValueCombo.getSelectedItem().toString().equals("1");

            int baseValue = 0;
            if (lastReadAddress != null && lastReadAddress == addr && lastReadHex != null) {
                baseValue = utils.convertToHexDec16(lastReadHex);
            } else {
                log("Sem leitura previa para esse endereco, base=0.");
            }

            String baseHex = utils.getFormateHexWrite(baseValue);
            wordBit.setWorldBits(addr, baseHex);
            wordBit.setBit(bit, bitValue);

            int newValue = Integer.parseInt(wordBit.getBitToWorld(), 2);
            int[] values = new int[] { newValue };
            int mode = writeModeCombo.getSelectedItem().toString().equals("BCD") ? MemoryWrite.BCD : MemoryWrite.HEX;

            AreaWriteWR write = new AreaWriteWR(plc, addr, values, mode);
            comHandler.send(write);

            IStatusCode status = write.getResponseStatusCode();
            log("WRITE WR addr=" + addr + " bit=" + bit + " value=" + (bitValue ? 1 : 0)
                    + " word=" + newValue
                    + " status=" + (status == null ? "null" : status.getCode() + " - " + status.getDescription()));
        } catch (ComException ex) {
            log("Erro escrita: " + ex.getMessage());
        } catch (Exception ex) {
            log("Erro escrita: " + ex.getMessage());
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}



