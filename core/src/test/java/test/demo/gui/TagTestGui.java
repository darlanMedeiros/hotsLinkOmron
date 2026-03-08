package test.demo.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.IStatusCode;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.comm.serial.SerialPortFactoryJSerialComm;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.comm.serial.SerialUtils;
import org.ctrl.extras.Tag;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;

public class TagTestGui {

    private static final String DEVICE_MNEMONIC = "PLC";
    private static final Tag TEST_TAG = Tag.dmWord("TEST_TAG", 100);

    private JFrame frame;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;

    private JTextField tagValueField;
    private JLabel statusLabel;

    private SerialPortHandlerImp comHandler;
    private IDevice plc;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TagTestGui window = new TagTestGui();
            window.frame.setVisible(true);
        });
    }

    public TagTestGui() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Tag Test GUI - Ler/Escrever");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(720, 260));
        frame.setLayout(new BorderLayout(10, 10));

        frame.add(buildConfigPanel(), BorderLayout.NORTH);
        frame.add(buildTagPanel(), BorderLayout.CENTER);
        frame.add(buildStatusPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Conexao"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        portField = new JTextField("COM1");
        baudField = new JTextField("9600");
        dataBitsField = new JTextField("7");
        stopBitsField = new JTextField("2");
        timeoutField = new JTextField("10000");
        nodeField = new JTextField("4");
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

        return panel;
    }

    private JPanel buildTagPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Tag DM"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("TAG " + TEST_TAG.getName() + " (DM " + TEST_TAG.getAddress() + ")"), c);

        tagValueField = new JTextField("0");
        tagValueField.setColumns(16);
        tagValueField.setPreferredSize(new Dimension(220, 30));
        c.gridx = 1;
        panel.add(tagValueField, c);

        JButton readButton = new JButton("Ler");
        readButton.addActionListener(e -> readTag());
        c.gridx = 2;
        panel.add(readButton, c);

        JButton writeButton = new JButton("Escrever");
        writeButton.addActionListener(e -> writeTag());
        c.gridx = 3;
        panel.add(writeButton, c);

        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        statusLabel = new JLabel("Status: parado");
        panel.add(statusLabel);
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
            sp.setBaudRate(SerialPortAbstract.BaudRate.getBaudRate(Integer.parseInt(baudField.getText().trim())));
            sp.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
            sp.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
            sp.setParity(SerialPortAbstract.Parity.valueOf(parityCombo.getSelectedItem().toString()));

            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            comHandler = new SerialPortHandlerImp(SerialUtils.createSerial(sp));

            int timeout = Integer.parseInt(timeoutField.getText().trim());
            if (comHandler instanceof IComControl) {
                ((IComControl) comHandler).setCommunicationTimeOut(timeout);
            }

            comHandler.initialize();
            comHandler.setProtocolHandler(new ToolbusProtocol());
            comHandler.start();

            ensureDevice();
            setStatus("Conectado em " + sp.getDevice() + ".");
        } catch (Exception ex) {
            setStatus("Erro ao conectar: " + ex.getMessage());
        }
    }

    private void ensureDevice() {
        int nodeId = Integer.parseInt(nodeField.getText().trim());
        if (plc == null || plc.getId() != nodeId) {
            plc = new DeviceImp(nodeId, DEVICE_MNEMONIC, "PLC", "Omron PLC");
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);
        }
    }

    private void readTag() {
        try {
            connect();
            ensureDevice();

            AreaReadDM read = new AreaReadDM(plc, TEST_TAG.toMemoryVariable());
            comHandler.send(read);

            int[] values = parseReply(read.getReply(), TEST_TAG.getLengthWords());
            if (values != null && values.length > 0) {
                tagValueField.setText(Integer.toString(values[0]));
            }
            IStatusCode status = read.getResponseStatusCode();
            setStatus("Leitura OK (" + (status == null ? "null" : status.getCode()) + ")");
        } catch (ComException ex) {
            setStatus("Erro leitura: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro leitura: " + ex.getMessage());
        }
    }

    private void writeTag() {
        try {
            connect();
            ensureDevice();

            int value = Integer.parseInt(Objects.requireNonNull(tagValueField.getText()).trim());
            AreaWriteDM write = new AreaWriteDM(plc, TEST_TAG.toMemoryVariable(), new int[] { value }, MemoryWrite.HEX);
            comHandler.send(write);

            IStatusCode status = write.getResponseStatusCode();
            setStatus("Escrita OK (" + (status == null ? "null" : status.getCode()) + ")");
        } catch (ComException ex) {
            setStatus("Erro escrita: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro escrita: " + ex.getMessage());
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

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + msg));
    }
}
