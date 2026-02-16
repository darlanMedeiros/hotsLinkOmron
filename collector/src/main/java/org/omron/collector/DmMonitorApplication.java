package org.omron.collector;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryJSerialComm;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DmMonitorApplication {

    private static final int START_ADDR = 0;
    private static final int END_ADDR = 10;
    //private static final int VALUE_MODE = MemoryWrite.HEX;
    // Define o tamanho do chunk para leitura. Pode ser ajustado conforme
    // necessário.
    // Valorer acima de 10 estão causando bug no windows, provavelmente por causa do
    // buffer da porta serial. Testar com valores maiores em Linux.

    private static final int CHUNK_SIZE = 1;
    private static final int HISTORY_RETENTION_DAYS = 14;
    private static final int HISTORY_PRUNE_INTERVAL_CYCLES = 300;
    private static final int ERROR_RETRY_DELAY_MS = 1500;
    private static final int MAX_ERROR_RETRY_DELAY_MS = 15000;

    public static void main(String[] args) {

        SerialPortHandlerPjcImp comHandler = null;
        AnnotationConfigApplicationContext ctx = null;

        try {

            // =============================
            // CONFIGURAÇÃO SERIAL
            // =============================
            SerialParameters sp = new SerialParameters();
            sp.setDevice("COM2");
            sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
            sp.setDataBits(7);
            sp.setStopBits(2);
            sp.setParity(SerialPort.Parity.EVEN);

            int nodeId = 0;
            int timeoutMs = 10000;
            int delayMs = 1000;

            // =============================
            // SPRING CONTEXT (BANCO)
            // =============================
            ctx = new AnnotationConfigApplicationContext(DbConfig.class);
            DmValueService service = ctx.getBean(DmValueService.class);

            // =============================
            // SERIAL + PROTOCOLO
            // =============================
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));

            ToolbusProtocol protocol = new ToolbusProtocol();
            comHandler.setProtocolHandler(protocol);

            if (comHandler instanceof IComControl) {
                ((IComControl) comHandler).setCommunicationTimeOut(timeoutMs);
            }

            comHandler.initialize();
            comHandler.start();

            // =============================
            // DEVICE
            // =============================
            IDevice plc = new DeviceImp(nodeId, "PLC", "PLC", "Omron PLC");
            IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
            deviceRegister.addDevice(plc);

            DeviceInfo deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());

            // =============================
            // MONITORAMENTO CONTÍNUO
            // =============================
            int size = END_ADDR - START_ADDR + 1;
            int[] lastValues = new int[size];
            boolean firstRead = true;

            System.out.println("Starting DM Monitor 0..100");

            int cycleCount = 0;
            int consecutiveErrors = 0;

            while (true) {
                try {
                    int addr = START_ADDR;

                    while (addr <= END_ADDR) {

                        int remaining = END_ADDR - addr + 1;
                        int length = Math.min(CHUNK_SIZE, remaining);

                        AreaReadDM read = new AreaReadDM(plc, addr, length);
                        comHandler.send(read);

                        int[] values = parseReply(read.getReply(), length);

                        if (values != null) {

                            for (int i = 0; i < values.length; i++) {

                                int absoluteAddr = addr + i;

                                int index = absoluteAddr - START_ADDR;

                                if (firstRead || lastValues[index] != values[i]) {

                                    service.saveRange(deviceInfo, absoluteAddr, new int[] { values[i] });

                                    System.out.println("CHANGE DM "
                                            + absoluteAddr
                                            + " old=" + lastValues[index]
                                            + " new=" + values[i]);

                                    lastValues[index] = values[i];
                                }
                            }
                        }

                        addr += length;
                    }

                    firstRead = false;
                    cycleCount++;
                    consecutiveErrors = 0;
                    if (cycleCount % HISTORY_PRUNE_INTERVAL_CYCLES == 0) {
                        int deletedRows = service.pruneHistoryOlderThanDays(HISTORY_RETENTION_DAYS);
                        if (deletedRows > 0) {
                            System.out.println("Cleanup memory_value: removed " + deletedRows + " rows.");
                        }
                    }
                    Thread.sleep(delayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception loopEx) {
                    consecutiveErrors++;
                    int factor = 1 << Math.min(consecutiveErrors - 1, 3);
                    int backoffMs = Math.min(MAX_ERROR_RETRY_DELAY_MS, ERROR_RETRY_DELAY_MS * factor);
                    System.err.println("Collector loop error: " + loopEx.getMessage() + ". Retrying in "
                            + backoffMs + " ms.");
                    Thread.sleep(backoffMs);
                }
            }

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

    // =====================================
    // PARSER DA RESPOSTA
    // =====================================
    private static int[] parseReply(org.ctrl.IData reply, int length) {

        if (reply == null)
            return null;

        int[] dataBuff = reply.toHexArray();
        if (dataBuff == null)
            return null;

        int[] out = new int[length];

        for (int i = 0; i < length; i++) {

            String val = "";

            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (idx < dataBuff.length) {
                    val += (char) dataBuff[idx];
                }
            }

            try {
                out[i] = Integer.parseInt(val.trim(), 16);
            } catch (Exception e) {
                out[i] = 0;
            }
        }

        return out;
    }
}
