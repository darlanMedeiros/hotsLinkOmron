package org.ctrl.comm.serial;

import java.util.*;


public class SerialUtils {

    static private Set<ValidatorSerialPortFactory> validatorSet = new TreeSet<ValidatorSerialPortFactory>();

    static {
        registerSerialPortFactory("com.fazecast.jSerialComm.SerialPort", "org.serial.SerialPortFactoryJSerialComm");

    }

    static private SerialPortAbstractFactory factory = null;

    static public void registerSerialPortFactory(String connectorClassname, String factoryClassname) {
        if (!validatorSet.add(new ValidatorSerialPortFactory(connectorClassname, factoryClassname))) {
            Modbus.log().warning("The factory is already registered, skipping: " + factoryClassname);
        }
    }

    static public void trySelectConnector() throws SerialPortException {
        Iterator<ValidatorSerialPortFactory> iterator = validatorSet.iterator();
        while (iterator.hasNext() && getSerialPortFactory() == null) {
            ValidatorSerialPortFactory validator = iterator.next();
            if (validator.validate()) {
                try {
                    setSerialPortFactory(validator.getFactory());
                } catch (ClassNotFoundException e) {
                    Modbus.log().warning("Cannot set a serial port factory " + validator.getFactoryClassname());
                }
            }
        }
        if (getSerialPortFactory() == null) {
            throw new SerialPortException("There are no available connectors");
        }
    }

    static public SerialPort createSerial(SerialParameters sp) throws SerialPortException {
        return getSerialPortFactory().createSerial(sp);
    }

    static public List<String> getPortIdentifiers() throws SerialPortException {
        return getSerialPortFactory().getPortIdentifiers();
    }

    static public String getConnectorVersion() {
        return getSerialPortFactory().getVersion();
    }

    /**
     * @param factory - a concrete serial port factory instance
     * @since 1.2.1
     */
    static public void setSerialPortFactory(SerialPortAbstractFactory factory) {
        SerialUtils.factory = factory;
    }

    /**
     * 
     * @return return Serial Port implementation
     */

    static public SerialPortAbstractFactory getSerialPortFactory() {
        if (SerialUtils.factory == null) {
            SerialUtils.factory = new SerialPortFactoryJSerialComm();
        }
        return SerialUtils.factory;
    }
}
