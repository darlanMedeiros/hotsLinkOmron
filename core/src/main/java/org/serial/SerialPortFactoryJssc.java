package org.serial;

import java.util.Arrays;
import java.util.List;

import jssc.SerialPortList;

public class SerialPortFactoryJssc extends SerialPortAbstractFactory {

    @Override
    public SerialPort createSerialImpl(SerialParameters sp) {
        return new SerialPortJssc(sp);
    }

    @Override
    public List<String> getPortIdentifiersImpl() {
        return Arrays.asList(SerialPortList.getPortNames());
    }

    @Override
    public String getVersion() {
        return "jssc 2.8.0";
    }
}
