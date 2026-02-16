package org.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

public class SerialPortJSerialComm extends SerialPort {

    private com.fazecast.jSerialComm.SerialPort port;
    private final AtomicBoolean opened = new AtomicBoolean(false);
    private InputStream in;
    private OutputStream out;

    public SerialPortJSerialComm(SerialParameters sp) {
        super(sp);
    }

    @Override
    public void write(int b) throws IOException {
        if (!isOpened() || out == null) {
            throw new IOException("Port not opened");
        }
        out.write(b);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (!isOpened() || out == null) {
            throw new IOException("Port not opened");
        }
        out.write(bytes);
    }

    @Override
    public void open() throws SerialPortException {
        SerialParameters sp = getSerialParameters();
        try {
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

            in = port.getInputStream();
            out = port.getOutputStream();
            opened.set(true);
        } catch (SerialPortInvalidPortException ex) {
            throw new SerialPortException("Serial port does not exist " + sp.getDevice(), ex);
        } catch (SerialPortException ex) {
            throw ex;
        } catch (Exception ex) {
            throw buildOpenFailure(sp.getDevice(), ex);
        }
    }

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

    @Override
    public int read() throws IOException {
        if (!isOpened() || in == null) {
            throw new IOException("Port not opened");
        }
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!isOpened() || in == null) {
            throw new IOException("Port not opened");
        }
        return in.read(b, off, len);
    }

    @Override
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ignored) {
            // best-effort cleanup
        } finally {
            in = null;
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ignored) {
            // best-effort cleanup
        } finally {
            out = null;
        }
        try {
            if (port != null && port.isOpen()) {
                port.closePort();
            }
        } catch (Exception ignored) {
            // best-effort cleanup
        } finally {
            port = null;
            opened.set(false);
        }
    }

    @Override
    public boolean isOpened() {
        return opened.get();
    }

    @Override
    public OutputStream getOutputSream() {
        return out;
    }

    @Override
    public InputStream inputStream() {
        return in;
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public void sendBreak(int millis) {
        if (port != null && port.isOpen()) {
            port.setBreak();
            try {
                Thread.sleep(Math.max(1, millis));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                port.clearBreak();
            }
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
        String details = "";
        if (cause != null && cause.getMessage() != null) {
            details = cause.getMessage().toLowerCase(Locale.ROOT);
        }
        if (details.contains("busy") || details.contains("in use") || details.contains("access denied")) {
            return new SerialPortException("Serial port in use " + portName, cause);
        }
        if (details.contains("not found") || details.contains("does not exist")) {
            return new SerialPortException("Serial port does not exist " + portName, cause);
        }
        return new SerialPortException("Cannot open serial port " + portName, cause);
    }
}
