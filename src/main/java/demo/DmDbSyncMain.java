package demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.service.DmValueService;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Poll DM area 0000-1000 and persist changes to Postgres.
 */
public class DmDbSyncMain {

    private static final int START_ADDR = 0;
    private static final int END_ADDR = 1000;
    private static final int DEFAULT_CHUNK = 100;
    private static final int DEFAULT_POLL_MS = 1000;
    private static final int DEFAULT_TIMEOUT_MS = 10000;

    public static void main(String[] args) {
        SerialPortHandlerPjcImp comHandler = null;
        AnnotationConfigApplicationContext ctx = null;

        try {
            SerialParameters sp = buildSerialParams(args);
            int nodeId = getIntArg(args, 5, 0);
            int timeoutMs = getIntArg(args, 6, DEFAULT_TIMEOUT_MS);
            int pollMs = getIntArg(args, 7, DEFAULT_POLL_MS);
            int chunkSize = getIntArg(args, 8, DEFAULT_CHUNK);

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

            int[] lastValues = new int[END_ADDR - START_ADDR + 1];
            for (int i = 0; i < lastValues.length; i++) {
                lastValues[i] = Integer.MIN_VALUE;
            }

            final SerialPortHandlerPjcImp shutdownHandler = comHandler;
            final AnnotationConfigApplicationContext shutdownCtx = ctx;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (shutdownHandler != null) {
                    shutdownHandler.stop();
                }
                if (shutdownCtx != null) {
                    shutdownCtx.close();
                }
            }));

            while (true) {
                pollOnce(comHandler, plc, service, lastValues, chunkSize);
                Thread.sleep(pollMs);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (comHandler != null) {
                comHandler.stop();
            }
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    private static void pollOnce(SerialPortHandlerPjcImp comHandler,
                                 IDevice plc,
                                 DmValueService service,
                                 int[] lastValues,
                                 int chunkSize) {
        int addr = START_ADDR;
        while (addr <= END_ADDR) {
            int remaining = END_ADDR - addr + 1;
            int length = Math.min(chunkSize, remaining);
            AreaReadDM read = new AreaReadDM(plc, addr, length);
            comHandler.send(read);

            int[] values = parseReply(read.getReply(), length, MemoryWrite.HEX);
            if (values != null) {
                List<DmValue> changed = new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    int currentAddr = addr + i;
                    int index = currentAddr - START_ADDR;
                    int currentVal = values[i];
                    if (lastValues[index] != currentVal) {
                        lastValues[index] = currentVal;
                        changed.add(new DmValue(currentAddr, currentVal, Instant.now()));
                    }
                }
                if (!changed.isEmpty()) {
                    service.saveBatch(changed);
                }
            }

            addr += length;
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
