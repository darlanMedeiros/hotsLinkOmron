package test.demo;

import java.util.Timer;
import java.util.TimerTask;

import org.ctrl.DataImp;
import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.ctrl.comm.serial.SerialPortException;
import org.ctrl.comm.serial.SerialPortFactoryJSerialComm;
import org.ctrl.comm.serial.SerialPortHandlerImp;
import org.ctrl.comm.serial.SerialUtils;
import org.ctrl.extras.MemoryMap;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.area.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.area.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryRead;

public class SimpleTest_TAGvariable {

    // End of variables declaration//GEN-END:variables
    // protected String comPort = "COM3";
    // protected int comSpeed = 57600;
    // protected boolean updateOnMin = true;
    protected SerialPortHandlerImp comHandler = null;
    protected DeviceImp plc = null;
    protected IDeviceRegister deviceRegister;
    // protected FinsProxy readData;
    protected AreaReadDM readData;
    protected MemoryRead dataRead;
    protected AreaWriteDM aw;
    private int counter = 0;
    private MemoryVariable variable = new MemoryVariable("TEMPO", "DM", 5, 2);
    MemoryMap memoryMap = MemoryMap.getInstance();

    private static SerialParameters sp;

    /** Creates new form SimpleTest */
    public SimpleTest_TAGvariable() {
        initSystem();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {

                    new SimpleTest_TAGvariable();

                } catch (Exception e) {

                }

            }
        });
    }

    private void initSystem() {

        memoryMap.addVariable(variable);

        if (comHandler == null) {
            setPort();
            try {
                setCommHandler();
            } catch (SerialPortException e) {

                e.printStackTrace();
            }
        }

        if (comHandler.isStarted()) {

            readData = new AreaReadDM(plc, 100, 2);

            aw = new AreaWriteDM(plc);

            aw.setVariable(variable);

            Timer pingTimer = new Timer();
            pingTimer.schedule(new PingTask(), 1000, 100);

        } else {
            System.out.println("comHandler no isStarted");
            comHandler.stop();
        }

    }

    private void setPort() {

        sp = new SerialParameters();

        sp.setDevice("COM1");
        // these parameters are set by default
        sp.setBaudRate(SerialPortAbstract.BaudRate.BAUD_RATE_9600);
        sp.setDataBits(7);
        sp.setParity(SerialPortAbstract.Parity.EVEN);
        sp.setStopBits(2);
        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());

    }

    private void setCommHandler() throws SerialPortException {

        plc = new DeviceImp(4, "CPM2A", "Test PLC", "PLC for communications test");

        deviceRegister = DeviceRegisterImp.getInstance();
        deviceRegister.addDevice(plc);

        comHandler = new SerialPortHandlerImp(SerialUtils.createSerial(sp));

        ToolbusProtocol toolbusProtocol = new ToolbusProtocol();
        comHandler.setProtocolHandler(toolbusProtocol);

        if (comHandler instanceof IComControl) {
            ((IComControl) comHandler).setCommunicationTimeOut(1000);
        }

        comHandler.start();

    }

    class PingTask extends TimerTask {

        public void run() {

            try {

                // dataRead.clear();
                counter++;
                if (counter > 5) {

                    comHandler.stop();
                    System.exit(0);
                }
                long startT = System.currentTimeMillis();

                // System.out.println("Corrente Time" + startT);

                // Adiciona o valor na memoria.
                aw.setVariable(variable);
                aw.setValue(new int[] { counter, counter + 1 });

                System.out.println("Counter >>> " + counter + " >>>> Counter + 1 >>>" + (counter + 1));

                comHandler.send(aw);

                comHandler.send(readData);

                int[] dataBuff = readData.getReply().toHexArray();

                System.out.println(" took time :: " + (System.currentTimeMillis() - startT));

                DataImp di = new DataImp(dataBuff);
                System.out.println(" data inp is " + di.toString());

                // Cria um buffer na memória do
                // MemoryMap.getInstance().process(readData, MemoryMap.HEX);

                // Le o valor armazenado.

                memoryMap.addValue(variable, dataBuff, 1);

                int dm5[] = memoryMap.getValue(variable);

                int length = dm5.length;

                for (int i = 0; i < length; i++) {

                    System.out.println("D5 >>>>>> " + dm5[i]);

                }

                dm5 = memoryMap.getValue("TEMPO");

                length = dm5.length;

                for (int i = 0; i < length; i++) {

                    System.out.println("D5  TEMPO >>>>>> " + i + " " + dm5[i] + "\n");

                }

                Thread.sleep(1000);

            } catch (Exception e) {

                e.printStackTrace();
                System.out.println("Exception" + e);
                comHandler.stop();
                System.exit(0);

            }

        }
    }

}
