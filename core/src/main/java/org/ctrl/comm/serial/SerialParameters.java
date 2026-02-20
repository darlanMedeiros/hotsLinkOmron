package org.ctrl.comm.serial;


public class SerialParameters {
    private String device = null;
    private SerialPort.BaudRate baudRate;
    private int dataBits;
    private int stopBits;
    private SerialPort.Parity parity;

    public SerialParameters() {
        setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
        setDataBits(8);
        setStopBits(1);
        setParity(SerialPort.Parity.NONE);
    }

    /**
     * @param device   the name(path) of the serial port
     * @param baudRate baud rate
     * @param dataBits the number of data bits
     * @param stopBits the number of stop bits(1,2)
     * @param parity   parity check (NONE, EVEN, ODD, MARK, SPACE)
     */
    public SerialParameters(String device, SerialPort.BaudRate baudRate, int dataBits, int stopBits, SerialPort.Parity parity) {
        setDevice(device);
        setBaudRate(baudRate);
        setDataBits(dataBits);
        setStopBits(stopBits);
        setParity(parity);
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getBaudRate() {
        return baudRate.getValue();
    }

    public void setBaudRate(SerialPort.BaudRate baudRate) {
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

    public SerialPort.Parity getParity() {
        return parity;
    }

    public void setParity(SerialPort.Parity parity) {
        this.parity = parity;
    }

	
}
