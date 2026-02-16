package org.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jssc.SerialPortTimeoutException;

public class SerialPortJssc extends SerialPort {

    private jssc.SerialPort port;
    private final AtomicBoolean opened = new AtomicBoolean(false);
    private InputStream in;
    private OutputStream out;

    public SerialPortJssc(SerialParameters sp) {
        super(sp);
    }

    @Override
    public void write(int b) throws IOException {
        if (!isOpened()) {
            throw new IOException("Port not opened");
        }
        try {
            port.writeByte((byte) b);
        } catch (jssc.SerialPortException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (!isOpened()) {
            throw new IOException("Port not opened");
        }
        try {
            port.writeBytes(bytes);
        } catch (jssc.SerialPortException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void open() throws SerialPortException {
        SerialParameters sp = getSerialParameters();
        try {
            port = new jssc.SerialPort(sp.getDevice());
            port.openPort();
            port.setParams(
                    sp.getBaudRate(),
                    mapDataBits(sp.getDataBits()),
                    mapStopBits(sp.getStopBits()),
                    mapParity(sp.getParity()));
            port.setFlowControlMode(jssc.SerialPort.FLOWCONTROL_NONE);
            in = new JsscInputStream();
            out = new JsscOutputStream();
            opened.set(true);
        } catch (jssc.SerialPortException ex) {
            throw mapOpenException(ex, sp.getDevice());
        }
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        super.setReadTimeout(readTimeout);
    }

    @Override
    public int read() throws IOException {
        return inputStream().read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream().read(b, off, len);
    }

    @Override
    public void close() {
        try {
            if (isOpened() && port != null) {
                opened.set(false);
                port.closePort();
            }
        } catch (Exception ignored) {
            // best-effort cleanup
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
        if (!isOpened() || port == null) {
            return;
        }
        try {
            port.sendBreak(millis);
        } catch (jssc.SerialPortException ignored) {
            // best-effort break
        }
    }

    private int mapDataBits(int dataBits) {
        switch (dataBits) {
            case 5:
                return jssc.SerialPort.DATABITS_5;
            case 6:
                return jssc.SerialPort.DATABITS_6;
            case 7:
                return jssc.SerialPort.DATABITS_7;
            case 8:
            default:
                return jssc.SerialPort.DATABITS_8;
        }
    }

    private int mapStopBits(int stopBits) {
        switch (stopBits) {
            case 2:
                return jssc.SerialPort.STOPBITS_2;
            case 3:
                return jssc.SerialPort.STOPBITS_1_5;
            case 1:
            default:
                return jssc.SerialPort.STOPBITS_1;
        }
    }

    private int mapParity(Parity parity) {
        switch (parity) {
            case EVEN:
                return jssc.SerialPort.PARITY_EVEN;
            case ODD:
                return jssc.SerialPort.PARITY_ODD;
            case MARK:
                return jssc.SerialPort.PARITY_MARK;
            case SPACE:
                return jssc.SerialPort.PARITY_SPACE;
            case NONE:
            default:
                return jssc.SerialPort.PARITY_NONE;
        }
    }

    private SerialPortException mapOpenException(jssc.SerialPortException ex, String portName) throws SerialPortException {
        String type = ex.getExceptionType();
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (normalized.contains("port_busy")) {
            return new SerialPortException("Serial port in use " + portName, ex);
        }
        if (normalized.contains("port_not_found")) {
            return new SerialPortException("Serial port does not exist " + portName, ex);
        }
        return new SerialPortException("Cannot open serial port " + portName + ": " + type, ex);
    }

    private class JsscInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            if (!isOpened() || port == null) {
                throw new IOException("Port not opened");
            }
            int timeout = Math.max(1, getReadTimeout());
            try {
                byte[] result = port.readBytes(1, timeout);
                if (result == null || result.length == 0) {
                    throw new IOException("Read timeout");
                }
                return result[0] & 0xFF;
            } catch (SerialPortTimeoutException ex) {
                throw new IOException("Read timeout", ex);
            } catch (jssc.SerialPortException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException("buffer");
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            if (!isOpened() || port == null) {
                throw new IOException("Port not opened");
            }
            int timeout = Math.max(1, getReadTimeout());
            try {
                byte[] result = port.readBytes(len, timeout);
                if (result == null || result.length == 0) {
                    throw new IOException("Read timeout");
                }
                System.arraycopy(result, 0, b, off, result.length);
                return result.length;
            } catch (SerialPortTimeoutException ex) {
                throw new IOException("Read timeout", ex);
            } catch (jssc.SerialPortException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public int available() throws IOException {
            if (!isOpened() || port == null) {
                return 0;
            }
            try {
                return port.getInputBufferBytesCount();
            } catch (jssc.SerialPortException ex) {
                throw new IOException(ex);
            }
        }
    }

    private class JsscOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            SerialPortJssc.this.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            SerialPortJssc.this.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException("buffer");
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return;
            }
            byte[] slice = new byte[len];
            System.arraycopy(b, off, slice, 0, len);
            SerialPortJssc.this.write(slice);
        }
    }
}
