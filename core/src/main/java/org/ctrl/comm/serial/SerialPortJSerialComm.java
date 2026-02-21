package org.ctrl.comm.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public class SerialPortJSerialComm extends SerialPortAbstract {

    private SerialPort port;
    private final AtomicBoolean opened = new AtomicBoolean(false);

    private InputStream in;
    private OutputStream out;

    public SerialPortJSerialComm(SerialParameters sp) {
        super(sp);
    }

    // =========================================================
    // OPEN
    // =========================================================
    @Override
    public void open() throws SerialPortException {

        SerialParameters sp = getSerialParameters();

        try {
            if (!isPortPresent(sp.getDevice())) {
                throw new SerialPortException("Serial port does not exist " + sp.getDevice());
            }

            port = com.fazecast.jSerialComm.SerialPort.getCommPort(sp.getDevice());

            port.setComPortParameters(
                    sp.getBaudRate(),
                    sp.getDataBits(),
                    mapStopBits(sp.getStopBits()),
                    mapParity(sp.getParity()));

            port.setFlowControl(com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DISABLED);

            port.setComPortTimeouts(
                    com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    Math.max(1, getReadTimeout()),
                    0);

            if (!port.openPort()) {
                throw buildOpenFailure(sp.getDevice(), null);
            }

            // 🔥 Aplicar controle de linha corretamente
            if (sp.isRtsEnabled()) {
                port.setRTS();
            } else {
                port.clearRTS();
            }

            if (sp.isDtrEnabled()) {
                port.setDTR();
            } else {
                port.clearDTR();
            }

            in = port.getInputStream();
            out = port.getOutputStream();

            opened.set(true);

        } catch (SerialPortInvalidPortException ex) {
            throw new SerialPortException("Serial port does not exist " + sp.getDevice(), ex);
        } catch (Exception ex) {
            throw buildOpenFailure(sp.getDevice(), ex);
        }
    }

    // =========================================================
    // WRITE
    // =========================================================
    @Override
    public void write(int b) throws IOException {
        checkOpen();
        out.write(b);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        checkOpen();
        out.write(bytes);
    }

    // =========================================================
    // READ
    // =========================================================
    @Override
    public int read() throws IOException {
        checkOpen();
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkOpen();
        return in.read(b, off, len);
    }

    // =========================================================
    // CLOSE
    // =========================================================
    @Override
    public void close() {

        try {
            if (in != null)
                in.close();
        } catch (Exception ignored) {
        } finally {
            in = null;
        }

        try {
            if (out != null)
                out.close();
        } catch (Exception ignored) {
        } finally {
            out = null;
        }

        try {
            if (port != null && port.isOpen()) {
                port.closePort();
            }
        } catch (Exception ignored) {
        } finally {
            port = null;
            opened.set(false);
        }
    }

    // =========================================================
    // STATUS
    // =========================================================
    @Override
    public boolean isOpened() {
        return opened.get();
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public InputStream inputStream() {
        return in;
    }

    // =========================================================
    // TIMEOUT
    // =========================================================
    @Override
    public void setReadTimeout(int readTimeout) {
        super.setReadTimeout(readTimeout);

        if (port != null) {
            port.setComPortTimeouts(
                    com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    Math.max(1, readTimeout),
                    0);
        }
    }

    // =========================================================
    // RTS / DTR CONTROL
    // =========================================================
    // =========================================================
    // RTS / DTR CONTROL
    // =========================================================
    @Override
    public void setRTS(boolean enabled) {
        if (port != null && port.isOpen()) {
            if (enabled) {
                port.setRTS();
            } else {
                port.clearRTS();
            }
        }
    }

    @Override
    public void setDTR(boolean enabled) {
        if (port != null && port.isOpen()) {
            if (enabled) {
                port.setDTR();
            } else {
                port.clearDTR();
            }
        }
    }

    @Override
    public boolean isRTS() {
        // jSerialComm não fornece getRTS()
        return false;
    }

    @Override
    public boolean isDTR() {
        // jSerialComm não fornece getDTR()
        return false;
    }

    // =========================================================
    // BREAK
    // =========================================================
    @Override
    public void sendBreak(int millis) {
        if (port != null && port.isOpen()) {
            port.setBreak();
            try {
                Thread.sleep(Math.max(1, millis));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                port.clearBreak();
            }
        }
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================
    private void checkOpen() throws IOException {
        if (!isOpened() || in == null || out == null) {
            throw new IOException("Serial port not opened");
        }
    }

    private int mapStopBits(int stopBits) {
        switch (stopBits) {
            case 2:
                return com.fazecast.jSerialComm.SerialPort.TWO_STOP_BITS;
            case 1:
            default:
                return com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT;
        }
    }

    private int mapParity(Parity parity) {
        switch (parity) {
            case EVEN:
                return com.fazecast.jSerialComm.SerialPort.EVEN_PARITY;
            case ODD:
                return com.fazecast.jSerialComm.SerialPort.ODD_PARITY;
            case MARK:
                return com.fazecast.jSerialComm.SerialPort.MARK_PARITY;
            case SPACE:
                return com.fazecast.jSerialComm.SerialPort.SPACE_PARITY;
            case NONE:
            default:
                return com.fazecast.jSerialComm.SerialPort.NO_PARITY;
        }
    }

    private SerialPortException buildOpenFailure(String portName, Exception cause) throws SerialPortException {
        if (!isPortPresent(portName)) {
            return new SerialPortException("Serial port does not exist " + portName, cause);
        }

        String details = cause != null && cause.getMessage() != null
                ? cause.getMessage().toLowerCase(Locale.ROOT)
                : "";

        if (details.contains("busy") || details.contains("in use") || details.contains("access denied")) {
            return new SerialPortException("Serial port in use " + portName, cause);
        }

        if (cause == null) {
            return new SerialPortException(
                    "Cannot open serial port " + portName + " (port busy, access denied, or unavailable)",
                    null);
        }

        if (details.contains("not found") || details.contains("does not exist")) {
            return new SerialPortException("Serial port does not exist " + portName, cause);
        }

        return new SerialPortException("Cannot open serial port " + portName, cause);
    }

    private boolean isPortPresent(String portName) {
        if (portName == null || portName.trim().isEmpty()) {
            return false;
        }
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort candidate : ports) {
            String name = candidate.getSystemPortName();
            if (portName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
