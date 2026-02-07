package test.demo;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.concurrent.ThreadLocalRandom;

public class TestDmDbWrite {

    private static final int START_ADDR = 0;
    private static final int END_ADDR = 100;
    private static final int DEFAULT_CHUNK = 1;
    private static final int DEFAULT_TIMEOUT_MS = 10000;
    private static final int DEFAULT_DELAY_MS = 50;
    private static final int VALUE_MODE = MemoryWrite.HEX;
    private static final int RANDOM_MIN = 0;
    private static final int RANDOM_MAX = 100;

    public static void main(String[] args) {
        SerialPortHandlerPjcImp comHandler = null;
        AnnotationConfigApplicationContext ctx = null;

        try {
            SerialParameters sp = buildSerialParams(args);
            int nodeId = getIntArg(args, 5, 0);
            int timeoutMs = getIntArg(args, 6, DEFAULT_TIMEOUT_MS);
            int chunkSize = getIntArg(args, 7, DEFAULT_CHUNK);
            int delayMs = getIntArg(args, 8, DEFAULT_DELAY_MS);

            ctx = new AnnotationConfigApplicationContext(DbConfig.class);
            DmValueService service = ctx.getBean(DmValueService.class);

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
            DeviceInfo deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());

            int addr = START_ADDR;
            while (addr <= END_ADDR) {
                int randomValue = ThreadLocalRandom.current().nextInt(RANDOM_MIN, RANDOM_MAX + 1);
                int[] value = new int[] { randomValue };
                AreaWriteDM write = new AreaWriteDM(plc, addr, value, VALUE_MODE);
                try {
                    comHandler.send(write);
                    System.out.println("WRITE DM addr=" + addr + " value=" + randomValue
                            + " status: " + write.getResponseStatusCode());
                } catch (Exception ex) {
                    System.out.println("Write failed at addr=" + addr + " : " + ex.getMessage());
                }
                addr++;
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            }

            addr = START_ADDR;
            while (addr <= END_ADDR) {
                int remaining = END_ADDR - addr + 1;
                int length = Math.min(chunkSize, remaining);
                AreaReadDM read = new AreaReadDM(plc, addr, length);
                try {
                    comHandler.send(read);
                    int[] values = parseReply(read.getReply(), length, VALUE_MODE);
                    if (values != null) {
                        System.out.println("Read DM " + addr + " .. " + (addr + length - 1) + " : "
                                + java.util.Arrays.toString(values));
                        service.saveRange(deviceInfo, addr, values);
                    }
                } catch (Exception ex) {
                    System.out.println("Read failed at addr=" + addr + " len=" + length + " : " + ex.getMessage());
                }
                addr += length;
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            }

            System.out.println("Saved DM " + START_ADDR + ".." + END_ADDR + " to database.");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (comHandler != null) {
                comHandler.stop();
            }
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    private static int[] parseReply(IData reply, int length, int mode) {
        if (reply == null) {
            return null;
        }
        int[] dataBuff = reply.toHexArray();
        if (dataBuff == null) {
            return null;
        }
        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            String val = "";
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (idx < dataBuff.length) {
                    val = val + (char) dataBuff[idx];
                }
            }
            if (mode == MemoryWrite.BCD) {
                try {
                    out[i] = Integer.parseInt(val.trim());
                } catch (NumberFormatException ex) {
                    out[i] = 0;
                }
            } else {
                try {
                    out[i] = Integer.parseInt(val.trim(), 16);
                } catch (NumberFormatException ex) {
                    out[i] = 0;
                }
            }
        }
        return out;
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
