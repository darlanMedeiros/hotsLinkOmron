package org.serial;

import org.ctrl.comm.AbstractComHandler;
import org.ctrl.comm.ISerialComHandler;
import org.ctrl.comm.ISpontaneousEventListener;
import org.ctrl.comm.SerialParameters;

public class SerialPortHandlerPjcImp extends AbstractComHandler implements ISerialComHandler {

	private SerialPort serialPort;
	protected boolean shutDownHookRegistered = false;

	public SerialPortHandlerPjcImp(SerialPort serialPort) {

		super();
		this.serialPort = serialPort;
	}

	@Override
	public String getName() {

		return NAME;
	}

	@Override
	public SerialParameters getSerialComParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize()  {
		getLog().info(NAME + " initialized");

		if (!shutDownHookRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					terminate();
				}

			});
		}

		if (protocolHandler != null) {

			protocolHandler.setComControl(this);
		}

		//this.isStarted = true;

	}

	@Override
	public void run() {
		
		getLog().info(getName() + " thread run");
		
		while (stopRequired != true) {
			try {
				Thread.sleep(SLEEP_TIME);
				if (protocolHandler instanceof ISpontaneousEventListener) {
					((ISpontaneousEventListener) protocolHandler).checkEvent();
				}
			} catch (InterruptedException ex) {
				
				System.out.println("Erro run " + ex);
			}
		}

		getLog().info(getName() + " thread stopped");

	}

	@Override
	public void sendBreak(int millis) {

		this.serialPort.sendBreak(millis);

	}

	@Override
	public void setOutputBufferSize(int size) {
		this.serialPort.setOutputBufferSeize(size);

	}

	@Override
	public void setSerialComParameters(SerialParameters serialComParameters) {

	}

	@Override
	public void start(){
		stopRequired = false;
		try {
			this.serialPort.open();

			// setInputStream(this.serialPort.getInputStream());
			// setOutputStream(this.serialPort.getOutputSream());

			// connection.serialPort.addEventListener(reader);
			Thread thread = new Thread(this);
			thread.start();

			out = serialPort.getOutputSream();
			in = serialPort.getInputStream();
			if (protocolHandler != null) {
				protocolHandler.setComControl(this);
			}
			getLog().info("Serial Comunication Handler Started");
			
			this.isStarted = true;
			
		} catch (SerialPortException ex) {
			getLog().error("Unable to open connection", ex);			
			
		}

	}

	@Override
	public void stop() {
		stopRequired = true;
		this.serialPort.close();
		getLog().info("Serial Comunication Handler Stoped");

	}

	protected void terminate() {
		stop();
	}

}
