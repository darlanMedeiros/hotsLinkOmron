package test.demo;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadTC;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortException;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

public class TestTC {

	// </editor-fold>//GEN-END:initComponents

	/**
	 * @param args the command line arguments
	 */
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

					if (i < 500) {

						initSystem();

					} else {

						comHandler.stop();

					}

				} else {
					System.out.println("comHandler no isStarted");
					comHandler.stop();
				}

				// comHandler.stop();

			}

			private void setCommHandler() throws SerialPortException {

				plc = new DeviceImp(0, "CPM2A", "Test PLC", "PLC for communications test");

				deviceRegister = DeviceRegisterImp.getInstance();
				deviceRegister.addDevice(plc);

				comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));

				ToolbusProtocol toolbusProtocol = new ToolbusProtocol();
				comHandler.setProtocolHandler(toolbusProtocol);

				if (comHandler instanceof IComControl) {
					((IComControl) comHandler).setCommunicationTimeOut(1000);
				}

				comHandler.start();

				readTC0000 = new AreaReadTC(plc, endereco, 1);
				readTC0001 = new AreaReadTC(plc, 1, 1);

			}

			private void setPort() {

				sp = new SerialParameters();

				sp.setDevice("COM2");
				// these parameters are set by default
				sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
				sp.setDataBits(7);
				sp.setParity(SerialPort.Parity.EVEN);
				sp.setStopBits(2);

				SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());

			}

			public void initSystem() {

				try {

					// Inicializa a tread

					comHandler.send(readTC0000);
					comHandler.send(readTC0001);

					System.out.println("Test - Mensagaem recebida TC 00>>>>>>>>>>>>>>>>>> " + readTC0000.getReply());
					// System.out.println("Test - Mensagaem recebida TC 01>>>>>>>>>>>>>>>>>> " +
					// readTC0001.getReply());

				} catch (Exception e) {

					comHandler.stop();

					e.printStackTrace();

				}

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				if (readTC0000.getReply().toInteger() < 2) {

					comHandler.stop();

				} else {
					run();
				}

			}

		});
	}

	private static SerialParameters sp;
	protected static SerialPortHandlerPjcImp comHandler = null;
	protected static IDevice plc = null;
	protected static IDeviceRegister deviceRegister;
	protected static int endereco = 0;

	protected static AreaReadTC readTC0000;
	protected static AreaReadTC readTC0001;

	static int i = 0;

}
