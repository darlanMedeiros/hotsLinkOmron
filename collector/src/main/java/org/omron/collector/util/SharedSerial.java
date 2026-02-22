package org.omron.collector.util;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.ctrl.comm.IComControl;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.comm.serial.SerialPortFactoryJSerialComm;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.comm.serial.SerialUtils;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;

public class SharedSerial {

    public static final class Config {
        private final String device;
        private final int baudRate;
        private final int dataBits;
        private final int stopBits;
        private final SerialPortAbstract.Parity parity;
        private final int timeoutMs;
        private final Boolean rtsEnabled;
        private final Boolean dtrEnabled;

        public Config(String device,
                int baudRate,
                int dataBits,
                int stopBits,
                SerialPortAbstract.Parity parity,
                int timeoutMs,
                Boolean rtsEnabled,
                Boolean dtrEnabled) {
            this.device = device;
            this.baudRate = baudRate;
            this.dataBits = dataBits;
            this.stopBits = stopBits;
            this.parity = parity;
            this.timeoutMs = timeoutMs;
            this.rtsEnabled = rtsEnabled;
            this.dtrEnabled = dtrEnabled;
        }
    }

    private final Object ioLock = new Object();
    private SerialPortHandlerImp handler;

    public Object getIoLock() {
        return ioLock;
    }

    public synchronized SerialPortHandlerImp getHandler() {
        return handler;
    }

    public synchronized boolean isConnected() {
        return handler != null && handler.isStarted();
    }

    public synchronized void connect(Config config) throws Exception {
        SerialParameters params = new SerialParameters();
        params.setDevice(config.device);
        params.setBaudRate(SerialPortAbstract.BaudRate.getBaudRate(config.baudRate));
        params.setDataBits(config.dataBits);
        params.setStopBits(config.stopBits);
        params.setParity(config.parity);
        if (config.rtsEnabled != null) {
            params.setRtsEnabled(config.rtsEnabled.booleanValue());
        }
        if (config.dtrEnabled != null) {
            params.setDtrEnabled(config.dtrEnabled.booleanValue());
        }

        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
        SerialPortHandlerImp newHandler = new SerialPortHandlerImp(SerialUtils.createSerial(params));
        newHandler.setProtocolHandler(new ToolbusProtocol());
        if (newHandler instanceof IComControl) {
            ((IComControl) newHandler).setCommunicationTimeOut(config.timeoutMs);
        }

        try {
            newHandler.initialize();
            newHandler.start();
            if (!newHandler.isStarted()) {
                throw new IllegalStateException(
                        "Nao foi possivel iniciar a comunicacao serial na porta " + config.device + ".");
            }
        } catch (Exception ex) {
            try {
                newHandler.stop();
            } catch (Exception ignored) {
                // Keep original connection exception.
            }
            throw ex;
        }

        disconnect();
        handler = newHandler;
    }

    public synchronized void disconnect() {
        if (handler == null) {
            return;
        }
        synchronized (ioLock) {
            try {
                handler.stop();
            } catch (Exception ignored) {
                // Best effort stop for shutdown paths.
            } finally {
                handler = null;
            }
        }
    }

    public static void refreshAvailablePorts(JComboBox<String> portCombo, String preferredPort, String fallbackPort) throws Exception {
        String selected = (preferredPort == null || preferredPort.trim().isEmpty())
                ? getSelectedPortName(portCombo)
                : preferredPort.trim();

        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
        List<String> ports = SerialUtils.getPortIdentifiers();
        portCombo.removeAllItems();
        for (String port : ports) {
            portCombo.addItem(port);
        }

        if (selected == null || selected.isEmpty()) {
            if (fallbackPort != null && ports.contains(fallbackPort)) {
                selected = fallbackPort;
            } else if (!ports.isEmpty()) {
                selected = ports.get(0);
            }
        } else if (!ports.contains(selected)) {
            portCombo.addItem(selected);
        }

        if (selected != null && !selected.isEmpty()) {
            portCombo.setSelectedItem(selected);
        }
    }

    public static String getSelectedPortName(JComboBox<String> portCombo) {
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

    public static boolean isPortAvailable(String portName) throws Exception {
        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
        List<String> ports = SerialUtils.getPortIdentifiers();
        for (String port : ports) {
            if (portName.equalsIgnoreCase(port)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSerialPortInUse(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            if ("PortInUseException".equals(className)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("in use")
                        || normalized.contains("em uso")
                        || normalized.contains("busy")
                        || normalized.contains("access denied")
                        || normalized.contains("acesso negado")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
