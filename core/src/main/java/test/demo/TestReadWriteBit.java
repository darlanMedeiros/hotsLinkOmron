package test.demo;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadWriteBit;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

public class TestReadWriteBit {

    public static void main(String[] args) {
        SerialPortHandlerPjcImp comHandler = null;
        try {
            SerialParameters sp = buildSerialParams(args);
            int nodeId = getIntArg(args, 5, 0);
            int timeoutMs = getIntArg(args, 6, 10000);
            String addressBit = getArg(args, 7, "10.00");

            SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());
            comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));
            ToolbusProtocol protocol = new ToolbusProtocol();
            comHandler.setProtocolHandler(protocol);
            if (comHandler instanceof IComControl) {
                ((IComControl) comHandler).setCommunicationTimeOut(timeoutMs);
            }
            comHandler.initialize();
            comHandler.start();

            IDevice plc = new DeviceImp(nodeId, "PLC", "PLC", "Omron PLC");
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);

            AreaReadWriteBit rwBit = new AreaReadWriteBit();
            int readValue = rwBit.readBitRR(comHandler, plc, addressBit);
            System.out.println("READ " + addressBit + " -> " + readValue);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (comHandler != null) {
                comHandler.stop();
            }
        }
    }

    private static SerialParameters buildSerialParams(String[] args) {
        String port = getArg(args, 0, "COM2");
        int baud = getIntArg(args, 1, 9600);
        int dataBits = getIntArg(args, 2, 7);
        int stopBits = getIntArg(args, 3, 2);
        String parityText = getArg(args, 4, "EVEN");

        SerialParameters sp = new SerialParameters();
        sp.setDevice(port);
        sp.setBaudRate(SerialPort.BaudRate.getBaudRate(baud));
        sp.setDataBits(dataBits);
        sp.setStopBits(stopBits);
        sp.setParity(SerialPort.Parity.valueOf(parityText));
        return sp;
    }

    private static String getArg(String[] args, int index, String fallback) {
        if (args == null || args.length <= index || args[index] == null || args[index].trim().isEmpty()) {
            return fallback;
        }
        return args[index].trim();
    }

    private static int getIntArg(String[] args, int index, int fallback) {
        String value = getArg(args, index, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
