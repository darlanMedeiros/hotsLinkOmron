package org.ctrl.comm.serial;



import java.util.List;

abstract public class SerialPortAbstractFactory {

    protected SerialPortAbstractFactory() {
    }

    final public String getUnavailableString() {
        return "Connector is missing";
    }

    abstract public SerialPortAbstract createSerialImpl(SerialParameters sp) throws SerialPortException;
    abstract public List<String> getPortIdentifiersImpl() throws SerialPortException;

    final SerialPortAbstract createSerial(SerialParameters sp) throws SerialPortException {
        return createSerialImpl(sp);
    }

    final List<String> getPortIdentifiers() throws SerialPortException {
        return getPortIdentifiersImpl();
    }

    String getVersion() {
        return "The version number is unavailable.";
    }
}
