package org.ctrl.comm.serial;

import java.util.ArrayList;
import java.util.List;

public class SerialPortFactoryJSerialComm extends SerialPortAbstractFactory {

    @Override
    public SerialPortAbstract createSerialImpl(SerialParameters sp) {
        return new SerialPortJSerialComm(sp);
    }

    @Override
    public List<String> getPortIdentifiersImpl() {
        List<String> ports = new ArrayList<String>();
        com.fazecast.jSerialComm.SerialPort[] commPorts = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort p : commPorts) {
            ports.add(p.getSystemPortName());
        }
        return ports;
    }

    @Override
    public String getVersion() {
        return "jSerialComm";
    }
}
