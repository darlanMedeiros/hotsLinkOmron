package test.demo.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.comm.serial.SerialPortFactoryJSerialComm;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.comm.serial.SerialUtils;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.service.DmValueService;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import test.demo.TestDmDbWrite;

class DmReadThreadTest {

    @Test
    void deveExecutarLeituraContinuaEmThread() throws Exception {

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean executed = new AtomicBoolean(false);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DbConfig.class);

        DmValueService service = ctx.getBean(DmValueService.class);

        // ===== Serial =====
        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());

        SerialParameters sp = new SerialParameters();
        sp.setDevice("COM1"); // ajuste se necessário
        sp.setBaudRate(SerialPortAbstract.BaudRate.BAUD_RATE_9600);
        sp.setDataBits(7);
        sp.setStopBits(2);
        sp.setParity(SerialPortAbstract.Parity.EVEN);

        SerialPortHandlerImp comHandler = new SerialPortHandlerImp(SerialUtils.createSerial(sp));

        ToolbusProtocol protocol = new ToolbusProtocol();
        comHandler.setProtocolHandler(protocol);

        if (comHandler instanceof IComControl) {
            ((IComControl) comHandler).setCommunicationTimeOut(5000);
        }

        comHandler.initialize();
        comHandler.start();

        // ===== Device =====
        IDevice plc = new DeviceImp(4, "PLC", "PLC", "Omron PLC");
        IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();
        deviceRegister.addDevice(plc);

        DeviceInfo deviceInfo = new DeviceInfo("PLC", plc.getName(), plc.getDescription());

        // ===== Thread de leitura contínua =====
        Thread leituraThread = new Thread(() -> {
            int addr = 0;

            while (running.get()) {
                try {
                    AreaReadDM read = new AreaReadDM(plc, addr, 1);
                    comHandler.send(read);

                    int[] values = TestDmDbWrite
                            .parseReply(read.getReply(), 1, 0);

                    if (values != null) {
                        service.saveRange(deviceInfo, addr, values);
                        executed.set(true);
                        System.out.println("READ DM " + addr + " = " + values[0]);
                    }

                    addr = (addr >= 100) ? 0 : addr + 1;
                    Thread.sleep(100);

                } catch (Exception e) {
                    System.out.println("Erro leitura DM: " + e.getMessage());
                }
            }
        }, "dm-read-thread");

        leituraThread.start();

        // deixa rodar um pouco
        Thread.sleep(10000);

        // encerra
        running.set(false);
        leituraThread.join(2000);

        comHandler.stop();
        ctx.close();

        assertTrue(executed.get(), "A thread de leitura deve executar ao menos uma vez");
    }
}
