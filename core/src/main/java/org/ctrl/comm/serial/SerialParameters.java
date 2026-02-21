package org.ctrl.comm.serial;

public class SerialParameters {

    private String device;
    private SerialPortAbstract.BaudRate baudRate;
    private int dataBits;
    private int stopBits;
    private SerialPortAbstract.Parity parity;

    private boolean rtsEnabled = false;
    private boolean dtrEnabled = false;

    public SerialParameters() {
        setBaudRate(SerialPortAbstract.BaudRate.BAUD_RATE_9600);
        setDataBits(8);
        setStopBits(1);
        setParity(SerialPortAbstract.Parity.NONE);
    }

    public SerialParameters(String device,
            SerialPortAbstract.BaudRate baudRate,
            int dataBits,
            int stopBits,
            SerialPortAbstract.Parity parity) {
        this(device, baudRate, dataBits, stopBits, parity, false, false);
    }

    public SerialParameters(String device,
            SerialPortAbstract.BaudRate baudRate,
            int dataBits,
            int stopBits,
            SerialPortAbstract.Parity parity,
            boolean rtsEnabled,
            boolean dtrEnabled) {
        this.device = device;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.rtsEnabled = rtsEnabled;
        this.dtrEnabled = dtrEnabled;
    }

    // getters e setters normais...

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getBaudRate() {
        return baudRate.getValue();
    }

    public void setBaudRate(SerialPortAbstract.BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public SerialPortAbstract.Parity getParity() {
        return parity;
    }

    public void setParity(SerialPortAbstract.Parity parity) {
        this.parity = parity;
    }

    public boolean isRtsEnabled() {
        return rtsEnabled;
    }

    public void setRtsEnabled(boolean rtsEnabled) {
        this.rtsEnabled = rtsEnabled;
    }

    public boolean isDtrEnabled() {
        return dtrEnabled;
    }

    public void setDtrEnabled(boolean dtrEnabled) {
        this.dtrEnabled = dtrEnabled;
    }
}