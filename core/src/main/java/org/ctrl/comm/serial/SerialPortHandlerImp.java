package org.ctrl.comm.serial;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ctrl.comm.AbstractComHandler;
import org.ctrl.comm.ISerialComHandler;
import org.ctrl.comm.ISpontaneousEventListener;

public class SerialPortHandlerImp extends AbstractComHandler implements ISerialComHandler {

	private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
	private static final Set<SerialPortHandlerImp> ACTIVE_HANDLERS = ConcurrentHashMap.newKeySet();

	private SerialPortAbstract serialPort;
	private SerialParameters serialComParameters;
	private Thread workerThread;

	public SerialPortHandlerImp(SerialPortAbstract serialPort) {

		super();
		this.serialPort = serialPort;

	}

	@Override
	public String getName() {

		return NAME;
	}

	@Override
	public SerialParameters getSerialComParameters() {
		return serialComParameters;
	}

	@Override
	public void initialize() {
		getLog().info(NAME + " initialized");

		if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					for (SerialPortHandlerImp handler : ACTIVE_HANDLERS) {
						try {
							handler.terminate();
						} catch (Exception ignored) {
							// Shutdown path should not fail the JVM exit.
						}
					}
				}

			});
		}

		if (protocolHandler != null) {

			protocolHandler.setComControl(this);
		}

		// this.isStarted = true;

	}

	@Override
	public void run() {

		try {

			while (!stopRequired && !Thread.currentThread().isInterrupted()) {

				if (in != null && in.available() > 0) {
					int data = in.read();
					// processa
				}

				Thread.sleep(2); // 🔥 evita loop 100% CPU
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			getLog().warn("Serial thread error: " + e.getMessage());

		} finally {
			getLog().info("Serial Communication Handler thread stopped");
		}
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
		this.serialComParameters = serialComParameters;
	}

	@Override
	public void start() throws SerialPortException {

		stopRequired = false;

		try {
			this.serialPort.open();

			workerThread = new Thread(this, NAME + "-worker");
			workerThread.setDaemon(true);
			workerThread.start();

			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

			if (protocolHandler != null) {
				protocolHandler.setComControl(this);
			}

			getLog().info("Serial Comunication Handler Started");

			this.isStarted = true;
			ACTIVE_HANDLERS.add(this);

		} catch (SerialPortException ex) {
			getLog().error("Unable to open connection", ex);

			throw ex; // 🔥 ESSA LINHA RESOLVE TUDO
		}
	}

	@Override
	public synchronized void stop() {

		if (!isStarted) {
			return;
		}

		stopRequired = true;

		try {
			// 🔥 Força desbloquear read()
			if (serialPort != null) {
				serialPort.setReadTimeout(1);
			}

			// 🔥 Interrompe a thread caso esteja dormindo
			if (workerThread != null) {
				workerThread.interrupt();
				workerThread.join(1500);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		try {
			if (serialPort != null) {
				serialPort.close();
				Thread.sleep(200); // 🔥 CH340 precisa disso
			}
		} catch (Exception e) {
			getLog().warn("Error closing serial port: " + e.getMessage());
		}

		isStarted = false;
		ACTIVE_HANDLERS.remove(this);

		getLog().info("Serial Communication Handler stopped");
	}

	protected void terminate() {
		stop();
	}

}
