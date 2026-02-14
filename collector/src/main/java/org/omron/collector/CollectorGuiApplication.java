package org.omron.collector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

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
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CollectorGuiApplication {

    private static final int MONITOR_LENGTH = 100;
    private static final int CHUNK_SIZE = 1;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private JFrame frame;
    private JTextField portField;
    private JComboBox<String> baudCombo;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityCombo;
    private JTextField nodeField;
    private JTextField timeoutField;
    private JTextField pollMsField;
    private JTextField startDmField;
    private JLabel commStatusLabel;
    private JLabel monitorStatusLabel;
    private JTextArea logArea;

    private volatile boolean monitoring;
    private Thread monitorThread;
    private SerialPortHandlerPjcImp comHandler;
    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private IDevice plc;
    private DeviceInfo deviceInfo;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CollectorGuiApplication app = new CollectorGuiApplication();
            app.frame.setVisible(true);
        });
    }

    public CollectorGuiApplication() {
        buildUi();
    }

    private void buildUi() {
        frame = new JFrame("Collector DM Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(880, 520));
        frame.setLayout(new BorderLayout(10, 10));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
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

        portField = new JTextField("COM2");
        baudCombo = new JComboBox<>(new String[] { "4800", "9600", "14400", "19200", "38400", "57600", "115200" });
        baudCombo.setSelectedItem("9600");
        dataBitsField = new JTextField("7");
        stopBitsField = new JTextField("2");
        parityCombo = new JComboBox<>(new String[] { "EVEN", "NONE", "ODD", "MARK", "SPACE" });
        parityCombo.setSelectedItem("EVEN");
        nodeField = new JTextField("0");
        timeoutField = new JTextField("10000");
        pollMsField = new JTextField("1000");

        int row = 0;
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel("Porta"), c);
        c.gridx = 1;
        panel.add(portField, c);

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
        panel.setBorder(BorderFactory.createTitledBorder("Monitoramento DM"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        startDmField = new JTextField("0");

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("DM inicial"), c);
        c.gridx = 1;
        panel.add(startDmField, c);

        c.gridx = 2;
        panel.add(new JLabel("Faixa monitorada"), c);
        c.gridx = 3;
        panel.add(new JLabel("DM inicial ate DM inicial + 99"), c);

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

    private void connect() {
        if (isConnected()) {
            log("Comunicacao ja esta ativa.");
            return;
        }

        try {
            SerialParameters sp = new SerialParameters();
            sp.setDevice(portField.getText().trim());
            sp.setBaudRate(SerialPort.BaudRate.getBaudRate(Integer.parseInt(baudCombo.getSelectedItem().toString())));
            sp.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
            sp.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
            sp.setParity(SerialPort.Parity.valueOf(parityCombo.getSelectedItem().toString()));

            SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());
            comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));
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
            setCommStatus("ERRO");
            log("Falha na conexao: " + ex.getMessage());
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

        final int startAddr;
        final int pollMs;
        try {
            startAddr = Integer.parseInt(startDmField.getText().trim());
            if (startAddr < 0) {
                throw new IllegalArgumentException("DM inicial deve ser >= 0.");
            }
            pollMs = Integer.parseInt(pollMsField.getText().trim());
            if (pollMs < 100) {
                throw new IllegalArgumentException("Poll deve ser >= 100ms.");
            }
        } catch (Exception ex) {
            log("Parametros invalidos: " + ex.getMessage());
            return;
        }

        monitoring = true;
        monitorThread = new Thread(() -> runMonitorLoop(startAddr, startAddr + MONITOR_LENGTH - 1, pollMs), "collector-dm-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void runMonitorLoop(int startAddr, int endAddr, int pollMs) {
        setMonitorStatus("RODANDO");
        log("Monitor iniciado: DM " + startAddr + " ate DM " + endAddr + ".");

        int[] lastValues = new int[MONITOR_LENGTH];
        Arrays.fill(lastValues, Integer.MIN_VALUE);
        boolean firstCycle = true;

        while (monitoring) {
            try {
                int addr = startAddr;
                while (addr <= endAddr && monitoring) {
                    int length = Math.min(CHUNK_SIZE, endAddr - addr + 1);
                    AreaReadDM read = new AreaReadDM(plc, addr, length);
                    comHandler.send(read);

                    int[] values = parseReply(read.getReply(), length);
                    if (values == null) {
                        log("Leitura invalida para DM " + addr + ".");
                    } else {
                        for (int i = 0; i < values.length; i++) {
                            int currentAddr = addr + i;
                            int index = currentAddr - startAddr;
                            int currentValue = values[i];

                            if (firstCycle) {
                                lastValues[index] = currentValue;
                                continue;
                            }

                            if (lastValues[index] != currentValue) {
                                dmValueService.saveRange(deviceInfo, currentAddr, new int[] { currentValue });
                                log("Alteracao salva: DM " + currentAddr + " de " + lastValues[index] + " para "
                                        + currentValue + ".");
                                lastValues[index] = currentValue;
                            }
                        }
                    }

                    addr += length;
                }

                if (firstCycle) {
                    log("Leitura inicial concluida. Mudancas serao persistidas a partir do proximo ciclo.");
                    firstCycle = false;
                }

                setCommStatus("CONECTADO - ultimo ciclo " + LocalDateTime.now().format(TIME_FMT));
                Thread.sleep(pollMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                setCommStatus("ERRO DE COMUNICACAO");
                log("Erro no monitoramento: " + ex.getMessage());
                try {
                    Thread.sleep(500);
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
        }
    }

    private void closeDb() {
        if (dbContext != null) {
            dbContext.close();
            dbContext = null;
            dmValueService = null;
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
        stopMonitor();
        safeStopHandler();
        closeDb();
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
        SwingUtilities.invokeLater(() -> commStatusLabel.setText(text));
    }

    private void setMonitorStatus(String text) {
        SwingUtilities.invokeLater(() -> monitorStatusLabel.setText(text));
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalDateTime.now().format(TIME_FMT);
            logArea.append("[" + time + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
