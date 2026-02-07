package test.demo;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.utils.OmronUtils;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.ToolbusWordBit;
import org.ctrl.vend.omron.toolbus.commands.AreaReadRR;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteWR;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortException;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

// Teste de leitura e escrita de IOs RR e WR
public class TestIOReadWrite {

	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {

				if (comHandler == null) {
					setPort();
					try {
						setCommHandler();
					} catch (SerialPortException e) {

						e.printStackTrace();
					}
				}

				if (comHandler.isStarted()) {
					i++;

					if (i < 3) {

						initSystem();

					} else {

						comHandler.stop();
						;

					}

				} else {
					System.out.println("comHandler no isStarted");
					comHandler.stop();
				}

				// comHandler.stop();

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

				// SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());

			}

			// DEFINE THE COMMUNICATION HANDLER (manipulador de comunicação)
			private void setCommHandler() throws SerialPortException {
				// CREATE THE DEVICE (PLC)
				plc = new DeviceImp(0, "CPM2A", "Test PLC", "PLC for communications test");
				// CREATE THE DEVICE REGISTER
				deviceRegister = DeviceRegisterImp.getInstance();
				// Create a device list
				deviceRegister.addDevice(plc);
				// Create a standard serial port
				comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));
				// Create the protocol
				ToolbusProtocol toolbusProtocol = new ToolbusProtocol();

				// Send protocol to comHandler "Handler = manipulador"
				comHandler.setProtocolHandler(toolbusProtocol);

				if (comHandler instanceof IComControl) {
					((IComControl) comHandler).setCommunicationTimeOut(10000);
				}

				comHandler.start();

			}

			private void initSystem() {

				int[] value = new int[1];

				value[0] = 2;

				// CREATE THE TASK THAT WILL BE EXECUTED PERIODICALLY
				writeWR = new AreaWriteWR(plc, endereco, value, MemoryWrite.BCD);
				// CREATE THE READ COMMAND
				readRR = new AreaReadRR(plc, endereco, 1);
				// SEND THE WRITE COMMAND ESCRITA WR
				comHandler.send(writeWR);
				// PRINT THE STATUS OF THE WRITE COMMAND
				System.out.println("Status da Write endereço " + endereco + " : " + writeWR.getResponseStatusCode());
				// WAIT 2 SECONDS
				// SEND THE READ COMMAND LEITURA RR
				comHandler.send(readRR);
				// PRINT THE VALUE READ FROM RR
				System.out.println(
						"Valor rebido do HR" + endereco + " : "
								+ utils.convertToHexDec16(readRR.getReply().toString()));

				System.out.println("Teste bits canal RR 0 >>>>>> " + endereco + ":"
						+ utils.getFormatedBinary(readRR.getReply().toString()));

				worldBit.setWorldBits(readRR.getAddress(), readRR.getReply().toString());

				System.out.println(worldBit.getBitToWorld());

				System.out.println(Integer.parseInt(worldBit.getBitToWorld()));

				System.out.println(Integer.parseInt(worldBit.getBitToWorld(), 2));

				worldBit.setBit(0, true);
				worldBit.setBit(2, true);
				worldBit.setBit(3, true);

				int[] valueNew = new int[1];

				valueNew[0] = Integer.parseInt(worldBit.getBitToWorld(), 2);

				value[0] = valueNew[0] | value[0];

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				writeWR.setValue(endereco, value, MemoryWrite.BCD);

				comHandler.send(writeWR);

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				run();

			}
		});
	}
	// DEFINE THE SERIAL PORT PARAMETERS
	private static SerialParameters sp;
	// DEFINE THE COMMUNICATION HANDLER (manipulador de comunicação)
	protected static SerialPortHandlerPjcImp comHandler = null;
	// CREATE THE DEVICE (PLC)			
	protected static IDevice plc = null;
	// CREATE THE DEVICE REGISTER
	protected static IDeviceRegister deviceRegister;
	// CREATE THE TASK THAT WILL BE EXECUTED PERIODICALLY
	protected static AreaReadRR readRR;
	// DEFINE THE ADDRESS TO BE READ AND WRITTEN
	protected static int endereco = 10;
	// CREATE THE WRITE COMMAND
	protected static AreaWriteWR writeWR;
	// UTILITIES
	protected static OmronUtils utils = new OmronUtils();
	// TOOLBUS WORD BIT FOR BIT MANIPULATION
	protected static ToolbusWordBit worldBit = new ToolbusWordBit();
	// COUNTER
	static int i = 0;

}
