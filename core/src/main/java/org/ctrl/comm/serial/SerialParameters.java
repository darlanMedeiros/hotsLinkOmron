package org.ctrl.comm.serial;

import java.util.Objects;

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
        if (device == null || device.trim().isEmpty()) {
            throw new IllegalArgumentException("Device must not be null or empty");
        }
        this.device = device.trim();
    }

    public int getBaudRate() {
        if (baudRate == null) {
            throw new IllegalStateException("Baud rate is not configured");
        }
        return baudRate.getValue();
    }

    public void setBaudRate(SerialPortAbstract.BaudRate baudRate) {
        this.baudRate = Objects.requireNonNull(baudRate, "Baud rate must not be null");
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        if (dataBits < 5 || dataBits > 8) {
            throw new IllegalArgumentException("Data bits must be between 5 and 8");
        }
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        if (stopBits != 1 && stopBits != 2) {
            throw new IllegalArgumentException("Stop bits must be 1 or 2");
        }
        this.stopBits = stopBits;
    }

    public SerialPortAbstract.Parity getParity() {
        return parity;
    }

    public void setParity(SerialPortAbstract.Parity parity) {
        this.parity = Objects.requireNonNull(parity, "Parity must not be null");
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
