package org.ctrl.comm.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class SerialPortAbstract {

    final private SerialParameters serialParameters;
    private int readTimeout = Modbus.MAX_RESPONSE_TIMEOUT;

    public SerialPortAbstract(SerialParameters sp) {
        this.serialParameters = sp;
    }

    public SerialParameters getSerialParameters() {
        return serialParameters;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    abstract public void setRTS(boolean enabled);

    abstract public void setDTR(boolean enabled);

    abstract public boolean isRTS();

    abstract public boolean isDTR();

    abstract public void write(int b) throws IOException;

    abstract public void write(byte[] bytes) throws IOException;

    abstract public void open() throws SerialPortException;

    abstract public int read() throws IOException;

    abstract public int read(byte[] b, int off, int len) throws IOException;

    abstract public void close();

    abstract public boolean isOpened();

    abstract public OutputStream getOutputStream();

    /**
     * @deprecated Use {@link #getOutputStream()}.
     */
    @Deprecated
    public OutputStream getOutputSream() {
        return getOutputStream();
    }

    abstract public InputStream inputStream();

    abstract public InputStream getInputStream();

    public enum Parity {
        NONE(0),
        ODD(1),
        EVEN(2),
        MARK(3),
        SPACE(4);

        private final int value;

        Parity(int value) {
            this.value = value;
        }

        static public Parity getParity(Integer value) {
            for (Parity par : Parity.values()) {
                if (par.value == value) {
                    return par;
                }
            }
            throw new IllegalArgumentException("Illegal parity value:" + value);
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum BaudRate {
        BAUD_RATE_4800(4800),
        BAUD_RATE_9600(9600),
        BAUD_RATE_14400(14400),
        BAUD_RATE_19200(19200),
        BAUD_RATE_38400(38400),
        BAUD_RATE_57600(57600),
        BAUD_RATE_115200(115200);

        private final int value;

        BaudRate(int value) {
            this.value = value;
        }

        static public BaudRate getBaudRate(int value) {
            for (BaudRate br : BaudRate.values()) {
                if (br.value == value) {
                    return br;
                }
            }
            throw new IllegalArgumentException("Illegal baud rate value:" + value);
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public void sendBreak(int millis) {
        throw new UnsupportedOperationException("sendBreak is not supported by this serial implementation");
    }

    public void setOutputBufferSeize(int size) {
        throw new UnsupportedOperationException("setOutputBufferSeize is not supported by this serial implementation");
    }

    public void setSerialComParameters(SerialParameters serialComParameters) {
        throw new UnsupportedOperationException("setSerialComParameters is not supported by this serial implementation");
    }

}
