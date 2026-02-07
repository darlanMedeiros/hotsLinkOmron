package demo;

import java.util.Timer;
import java.util.TimerTask;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.extras.EnumLenght;
import org.ctrl.extras.MemoryMap;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.memory.MemoryRead;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortException;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

public class TestDM {

   
    protected SerialPortHandlerPjcImp comHandler = null;
    protected DeviceImp plc = null;
    protected IDeviceRegister deviceRegister;   
    protected AreaReadDM readData;
    protected AreaReadDM readData1;
    protected AreaReadDM readData2;
    protected MemoryRead dataRead;
    protected AreaWriteDM writeData;
    private int counter = 0;
    private MemoryVariable variable = new MemoryVariable("TEMPO", "DM", 1000, EnumLenght.DWORD.getLenght());
    MemoryMap memoryMap = MemoryMap.getInstance();
    private int endereco = 1000;
    private int[]value = new int []  {1,1,1,1};     

               


    private static SerialParameters sp;

    /** Creates new form SimpleTest */
    public TestDM() {
        initSystem();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {

                    new TestDM();

                } catch (Exception e) {

                }

            }
        });
    }

    private void initSystem() {
//       variable.setValue(123456, 0);
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

            readData = new AreaReadDM(plc, variable);
            readData1 = new AreaReadDM(plc, endereco, EnumLenght.DWORD.getLenght()); 
            readData2 = new AreaReadDM(plc, endereco, EnumLenght.QWORD.getLenght());  
            writeData = new AreaWriteDM(plc,variable,value, MemoryWrite.BCD);                
                        
            Timer pingTimer = new Timer();
            pingTimer.schedule(new PingTask(), 1000, 100);

        } else {
            System.out.println("comHandler no isStarted");
            comHandler.stop();
        }

    }
// DEFINE THE SERIAL PORT PARAMETERS
    private void setPort() {

        sp = new SerialParameters();

        sp.setDevice("COM2");
        // these parameters are set by default
        sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
        sp.setDataBits(7);
        sp.setParity(SerialPort.Parity.EVEN);
        sp.setStopBits(2);
        // end of default parameters
        SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());

    }
// DEFINE THE COMMUNICATION HANDLER (manipulador de comunicação)

    private void setCommHandler() throws SerialPortException {

        plc = new DeviceImp(0, "CPM2A", "Test PLC", "PLC for communications test");

        deviceRegister = DeviceRegisterImp.getInstance();
        deviceRegister.addDevice(plc);
// CREATE THE COMMUNICATION HANDLER
        comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));
// SET THE PROTOCOL HANDLER
        ToolbusProtocol toolbusProtocol = new ToolbusProtocol();
        comHandler.setProtocolHandler(toolbusProtocol);
 // SET THE TIMEOUT FOR COMMUNICATION            
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
                if (counter > 3) {                  

                    comHandler.stop();
                    System.exit(0);
                }

                comHandler.send(readData);   
                comHandler.send(writeData);
                comHandler.send(readData1);
                comHandler.send(readData2);


                System.out.println("Write mensagem" + writeData.getResponseStatusCode());
                System.out.println("Write mensagem" + readData.getResponseStatusCode());

                //int[] dataBuff = readData.getReply().toHexArray();
                //System.out.println(" took time :: " + (System.currentTimeMillis() - startT));

               System.out.println(" data inp is    " + readData.getReply().toString());
               System.out.println(" data inp is    " + readData.getReply().toInteger());
               System.out.println(" data inp is 1  " + readData1.getReply().toString());
               System.out.println(" data inp is 1  " + readData1.getReply().toInteger());
               System.out.println(" data inp is 2  " + readData2.getReply().toString());
               System.out.println(" data inp is 2  " + readData2.getReply().toInteger());
                System.out.println(" Variable data " + variable.getStringValue());
                System.out.println(" Variable data " + variable.getIntValue());

                //System.out.println(readData.getReply().toInteger());
                //System.out.println(utils.convertStringHexDoubleToDec(readData.getReply().toString()));

                // Cria um buffer na memória do
                // MemoryMap.getInstance().process(readData, MemoryMap.HEX);

                // Le o valor armazenado.   
                Thread.sleep(1000);
                //value[0]--;
                //writeData.setValue(value);

            } catch (Exception e) {

                e.printStackTrace();
                System.out.println("Exception" + e);
                comHandler.stop();
                System.exit(0);

            }

        }
    }

}
