package org.ctrl.comm.serial;

public class SerialPortException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SerialPortException() {

    }

    public SerialPortException(Exception cause) {
        super(cause);
    }

    public SerialPortException(String msg) {
        super(msg);
    }

    public SerialPortException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
