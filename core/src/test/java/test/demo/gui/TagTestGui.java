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
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.RrValueService;
import org.ctrl.db.service.TagService;
import org.ctrl.extras.Tag;
import org.ctrl.utils.OmronUtils;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.ToolbusWordBit;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadRR;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteWR;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TagTestGui {

    private static final String DEVICE_MNEMONIC = "PLC";
    private static final Tag LAMPADA_TAG = Tag.rrBit("lampada", 10, 0);
    private static final int PRODUCAO_ADDR = 0;
    private static final Tag PRODUCAO_TAG = Tag.dmWord("producao", PRODUCAO_ADDR);

    private JFrame frame;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;

    private JLabel lampadaStatus;
    private JTextField producaoValueField;
    private JLabel statusLabel;
    private JLabel dbStatusLabel;

    private SerialPortHandlerPjcImp comHandler;
    private ToolbusProtocol protocol;
    private IDevice plc;
    private IDeviceRegister deviceRegister;
    private AnnotationConfigApplicationContext ctx;
    private RrValueService rrValueService;
    private DmValueService dmValueService;
    private TagService tagService;
    private org.ctrl.db.model.Tag lampadaTagDb;
    private org.ctrl.db.model.Tag producaoTagDb;

    private final OmronUtils utils = new OmronUtils();
    private final ToolbusWordBit wordBit = new ToolbusWordBit();

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
        frame = new JFrame("Tag Test GUI (lampada 10.00 / producao DM)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(720, 320));
        frame.setLayout(new BorderLayout(10, 10));

        JPanel configPanel = buildConfigPanel();
        JPanel tagPanel = buildTagPanel();
        JPanel statusPanel = buildStatusPanel();

        frame.add(configPanel, BorderLayout.NORTH);
        frame.add(tagPanel, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
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

        return panel;
    }

    private JPanel buildTagPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Tags"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Lampada
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("TAG lampada (" + LAMPADA_TAG.getAddressBit() + ")"), c);
        lampadaStatus = new JLabel("OFF");
        c.gridx = 1;
        panel.add(lampadaStatus, c);
        JButton readLampada = new JButton("Ler");
        readLampada.addActionListener(e -> readLampada());
        c.gridx = 2;
        panel.add(readLampada, c);
        JButton toggleLampada = new JButton("Alternar");
        toggleLampada.addActionListener(e -> toggleLampada());
        c.gridx = 3;
        panel.add(toggleLampada, c);

        // Producao
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("TAG producao (DM " + PRODUCAO_ADDR + ")"), c);
        producaoValueField = new JTextField("0");
        c.gridx = 1;
        panel.add(producaoValueField, c);
        JButton readProducao = new JButton("Ler");
        readProducao.addActionListener(e -> readProducao());
        c.gridx = 2;
        panel.add(readProducao, c);
        JButton writeProducao = new JButton("Escrever");
        writeProducao.addActionListener(e -> writeProducao());
        c.gridx = 3;
        panel.add(writeProducao, c);

        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        statusLabel = new JLabel("Status: parado");
        dbStatusLabel = new JLabel("DB: sem grava\u00e7\u00e3o");
        c.gridx = 0;
        c.gridy = 0;
        panel.add(statusLabel, c);
        c.gridx = 1;
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
            ensureTags();
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
            dmValueService = ctx.getBean(DmValueService.class);
            tagService = ctx.getBean(TagService.class);
        }
    }

    private void ensureTags() {
        if (tagService == null || plc == null) {
            return;
        }
        DeviceInfo device = new DeviceInfo(DEVICE_MNEMONIC, plc.getName(), plc.getDescription());
        if (lampadaTagDb == null) {
            lampadaTagDb = tagService.getOrCreateRrTag(device, "lampada",
                    LAMPADA_TAG.getAddress(), LAMPADA_TAG.getBit());
        }
        if (producaoTagDb == null) {
            producaoTagDb = tagService.getOrCreateDmTag(device, "producao", PRODUCAO_ADDR);
        }
    }

    private void readLampada() {
        try {
            connect();
            ensureDevice();
            ensureDb();
            ensureTags();

            int wordValue = readWordRr(LAMPADA_TAG.getAddress());
            String baseHex = utils.getFormateHexWrite(wordValue);
            wordBit.setWorldBits(LAMPADA_TAG.getAddress(), baseHex);
            boolean on = wordBit.getWordBit(LAMPADA_TAG.getBit());
            updateLampada(on);
            saveLampada(on);
            setStatus("Leitura lampada OK");
        } catch (ComException ex) {
            setStatus("Erro leitura lampada: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro leitura lampada: " + ex.getMessage());
        }
    }

    private void toggleLampada() {
        try {
            connect();
            ensureDevice();

            int baseValue = readWordRr(LAMPADA_TAG.getAddress());
            String baseHex = utils.getFormateHexWrite(baseValue);
            wordBit.setWorldBits(LAMPADA_TAG.getAddress(), baseHex);
            boolean current = wordBit.getWordBit(LAMPADA_TAG.getBit());
            boolean newValue = !current;
            wordBit.setBit(LAMPADA_TAG.getBit(), newValue);

            int newWordValue = Integer.parseInt(wordBit.getBitToWorld(), 2);
            AreaWriteWR write = new AreaWriteWR(plc, LAMPADA_TAG.getAddress(),
                    new int[] { newWordValue }, MemoryWrite.HEX);
            comHandler.send(write);

            updateLampada(newValue);
            saveLampada(newValue);
            setStatus("Escrito WR " + LAMPADA_TAG.getAddressBit());
        } catch (ComException ex) {
            setStatus("Erro escrita lampada: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro escrita lampada: " + ex.getMessage());
        }
    }

    private void readProducao() {
        try {
            connect();
            ensureDevice();
            ensureDb();
            ensureTags();

            AreaReadDM read = new AreaReadDM(plc, PRODUCAO_TAG.toMemoryVariable());
            comHandler.send(read);

            int[] values = parseReply(read.getReply(), PRODUCAO_TAG.getLengthWords());
            if (values != null && values.length > 0) {
                producaoValueField.setText(Integer.toString(values[0]));
                saveProducao(values[0]);
            }

            IStatusCode status = read.getResponseStatusCode();
            setStatus("Leitura producao OK (" + (status == null ? "null" : status.getCode()) + ")");
        } catch (ComException ex) {
            setStatus("Erro leitura producao: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro leitura producao: " + ex.getMessage());
        }
    }

    private void writeProducao() {
        try {
            connect();
            ensureDevice();

            int value = Integer.parseInt(Objects.requireNonNull(producaoValueField.getText()).trim());
            AreaWriteDM write = new AreaWriteDM(plc, PRODUCAO_TAG.toMemoryVariable(), new int[] { value }, MemoryWrite.HEX);
            comHandler.send(write);

            saveProducao(value);
            IStatusCode status = write.getResponseStatusCode();
            setStatus("Escrito producao OK (" + (status == null ? "null" : status.getCode()) + ")");
        } catch (ComException ex) {
            setStatus("Erro escrita producao: " + ex.getMessage());
        } catch (Exception ex) {
            setStatus("Erro escrita producao: " + ex.getMessage());
        }
    }

    private int readWordRr(int address) throws ComException {
        AreaReadRR read = new AreaReadRR(plc, address, 1);
        comHandler.send(read);
        String reply = read.getReply() == null ? null : read.getReply().toString();
        if (reply == null) {
            throw new IllegalStateException("RR read returned null reply for address " + address);
        }
        return utils.convertToHexDec16(reply);
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

    private void updateLampada(boolean on) {
        SwingUtilities.invokeLater(() -> lampadaStatus.setText(on ? "ON" : "OFF"));
    }

    private void saveLampada(boolean value) {
        if (rrValueService == null) {
            return;
        }
        ensureTags();
        DeviceInfo device = new DeviceInfo(DEVICE_MNEMONIC, plc.getName(), plc.getDescription());
        rrValueService.saveValue(device, LAMPADA_TAG.getAddress(), LAMPADA_TAG.getBit(), value);
        setDbStatus("gravado lampada=" + (value ? "1" : "0"));
    }

    private void saveProducao(int value) {
        if (dmValueService == null) {
            return;
        }
        ensureTags();
        DeviceInfo device = new DeviceInfo(DEVICE_MNEMONIC, plc.getName(), plc.getDescription());
        dmValueService.saveValue(device, PRODUCAO_ADDR, value);
        setDbStatus("gravado producao=" + value);
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + msg));
    }

    private void setDbStatus(String msg) {
        SwingUtilities.invokeLater(() -> dbStatusLabel.setText("DB: " + msg));
    }
}
