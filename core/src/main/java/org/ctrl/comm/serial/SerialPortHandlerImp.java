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
		this.serialComParameters = serialComParameters;
	}

	@Override
	public void start() {
		stopRequired = false;
		try {
			this.serialPort.open();

			// setInputStream(this.serialPort.getInputStream());
			// setOutputStream(this.serialPort.getOutputStream());

			// connection.serialPort.addEventListener(reader);
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
			this.isStarted = false;
			ACTIVE_HANDLERS.remove(this);
			getLog().error("Unable to open connection", ex);
			throw new IllegalStateException("Unable to open serial connection", ex);
		}

	}

	@Override
	public void stop() {
		stopRequired = true;
		ACTIVE_HANDLERS.remove(this);
		if (workerThread != null) {
			workerThread.interrupt();
			workerThread = null;
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException ignored) {
				// best-effort cleanup
			} finally {
				out = null;
			}
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException ignored) {
				// best-effort cleanup
			} finally {
				in = null;
			}
		}
		if (this.serialPort != null) {
			this.serialPort.close();
		}
		this.isStarted = false;
		getLog().info("Serial Comunication Handler Stoped");

	}

	protected void terminate() {
		stop();
	}

}
