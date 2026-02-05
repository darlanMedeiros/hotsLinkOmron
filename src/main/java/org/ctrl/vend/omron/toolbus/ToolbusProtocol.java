/*
 * $Id: ToolbusProtocol.java,v 1.10 2005/11/06 18:49:34 remus Exp $
 *
 *  Copyright [2005] [Remus Pereni http://remus.pereni.org]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ctrl.vend.omron.toolbus;

import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.logging.LogFactory;
import org.ctrl.DataImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.ICommandRegister;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.AbstractProtocolHandler;
import org.ctrl.comm.CRCException;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IMessage;
import org.ctrl.comm.IStatusCode;
import org.ctrl.comm.MaximumNumberOfRetriesReachedException;
import org.ctrl.comm.TimeOutException;
import org.ctrl.vend.omron.toolbus.commands.ToolbusCommand;
import org.ctrl.vend.omron.toolbus.commands.ToolbusCommandRegister;

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.10 $ $Date: 2005/11/06 18:49:34 $
 */
public class ToolbusProtocol extends AbstractProtocolHandler {

	public static final String NAME = "TOOLBUS";
	public static final char DELIMITER = '*';
	public static final char TERMINATOR = '\r';
	public static final char START_MARKER = '@';

	protected IDeviceRegister deviceRegister = DeviceRegisterImp.getInstance();

	/**
	 *
	 */
	public ToolbusProtocol() {
		super();
		setCommandRegister(new ToolbusCommandRegister());
		log = LogFactory.getLog("toolbus");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#getProtocolName()
	 */
	@Override
	public String getProtocolName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#write(org.pereni.ctrl.comm.Message)
	 */
	@Override
	public IMessage write(IMessage message) throws ComException {
		
		if (message != null && message.getTarget() != null && message.getCommandId() != -1
				&& message.getData() != null) {

			//System.out.println("Mensagem"+ message.getData());
			
			message.formatData();			
			

			int[] messageBytes = message.getFormatedData();
			


			//System.out.println(" data inp is "+di.toString());
			
			int retries = 0;
			
			

			while (retries <= getComControl().getNumberOfRetries()) {

				try {
					for (int idx = 0; idx < messageBytes.length; idx++) {
						getComControl().getOutputStream().write(messageBytes[idx]);
					}

					getComControl().getOutputStream().flush();

					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						getLog().warn("Sleepeing thread, interrupted exception in read():", e);
					}

					// read the reply. If we've got one but it is not for the target we sent the
					// command or a reply to the
					// command then fire it and let others receive it.

					if (message.expectsReply()) {

						IMessage reply = read(message);

						while (!message.getTarget().equals(reply.getTarget())
								|| !message.getCommandName().equals(reply.getCommandName())) {
							getLog().debug("Out of order message " + reply.toString() + "\n"
									+ Arrays.toString(reply.getData().toHexArray()));
							reply.parseData();
							getComControl().fireNewMessage(reply);
							reply = read(message);
						}

						if (message instanceof ToolbusCommand) {
							((ToolbusCommand) message).setResponseStatusCode(reply.getResponseStatusCode());
						}
						message.setReply(reply.getReply());
						message.parseData();
					}
					return message;

				} catch (IOException ex) {
					getLog().warn("Exception durring flush.", ex);
				} catch (TimeOutException toex) {
					if (getLog().isDebugEnabled()) {
						getLog().warn("Communication Time Out.", toex);
					} else {
						getLog().warn("Communication Time Out.");
					}
				}

				retries++;
			}
		}

		throw new MaximumNumberOfRetriesReachedException(" Maximum number of retries reached: "
				+ getComControl().getNumberOfRetries() + " for: " + message.toString(), message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#read()
	 */
	@Override
	public synchronized IMessage read(IMessage originalCommand) throws ComException {

		int[] buffer = new int[128];
		int idx = 0;
		long startTime = System.currentTimeMillis();
		boolean terminatorReceived = false;

		try {
			while (getComControl().getInputStream().available() == 0) {

				if (getComControl().getCommunicationTimeOut() != -1
						&& System.currentTimeMillis() - startTime >= getComControl().getCommunicationTimeOut()) {
					throw new TimeOutException();
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					getLog().warn("Sleeping thread, interrupted exception in read():", e);
				}
			}
		} catch (IOException e) {
			getLog().warn("Exception durring read.", e);
		}

		try {

			while (!terminatorReceived) {

				// Read the available bytes.
				while (getComControl().getInputStream().available() > 0) {

					buffer[idx] = getComControl().getInputStream().read();

					if (buffer[idx] == DELIMITER) {

					}

					// if we have TERMINATOR then the message ended.
					if (buffer[idx] == TERMINATOR) { // message ended
						terminatorReceived = true;

						if (isMessageIntact(buffer, idx)) {
							return getBytesAsMessage(buffer, idx);
						} else {
							int[] messageBody = new int[idx];
							System.arraycopy(buffer, 0, messageBody, 0, idx);
							throw new CRCException(messageBody);
						}
					}
					idx++;
				}

				// the message is not complete, whait till last bytes are available
				try {
					while (getComControl().getInputStream().available() == 0) {

						if (getComControl().getCommunicationTimeOut() != -1 && System.currentTimeMillis()
								- startTime >= getComControl().getCommunicationTimeOut()) {
							throw new TimeOutException();
						}

						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							getLog().warn("Sleepeing thread, interrupted exception in read():", e);
						}
					}
				} catch (IOException e) {
					getLog().warn("Exception durring read.", e);
				}

			}
		} catch (IOException ex) {
			getLog().warn("Exception durring read.", ex);
		}
		return null;
	}

	protected boolean isMessageIntact(int[] message, int length) {
		String calculatedCRC = new String(getCRC(message, length)).toUpperCase();
		String receivedCRC = "";

		int end = length;

		if (message[end] == TERMINATOR) {
			receivedCRC = "" + (char) message[end - 3] + (char) message[end - 2];
		} else if (message[end] == DELIMITER) {
			receivedCRC = "" + (char) message[end - 2] + (char) message[end - 1];
		} else {
			getLog().warn("Message doesn't end with the standard delimiter or terminator but with: " + +message[end]
					+ ", " + end);
		}

		boolean match = receivedCRC.equals(calculatedCRC);
		if (!match) {
			getLog().warn("Difference in CRC received/calculated " + receivedCRC + " / " + calculatedCRC + " / "
					+ new String(message, 0, length + 1));
		}
		return match;
	}

	protected ToolbusCommand getBytesAsMessage(int[] messageBody, int length) {
		
		int deviceId = Integer.valueOf(new String("" + (char) messageBody[1] + (char) messageBody[2]));

		String command = "" + (char) messageBody[3] + (char) messageBody[4];

		int[] data = new int[length - 10];
		
		System.arraycopy(messageBody, 7, data, 0, data.length);

		//ToolbusCommand msg = (ToolbusCommand) getCommandRegister().getNewByName(command);
		ToolbusCommand msg = this.getCommand(command);
		
		if (msg == null) {
			getLog().debug("ToolbusCommandRegister não encontrado");
			return null;
		}
		IStatusCode status = ToolbusStatusRegister.get(new String("" + (char) messageBody[5] + (char) messageBody[6]));
		msg.setResponseStatusCode(status);
		msg.setReply((new DataImp(data)));
		msg.setTarget(DeviceRegisterImp.getInstance().lookupById(deviceId));
		msg.setHasReply(true);

		if (getLog().isDebugEnabled()) {
			getLog().debug("Received >>>>>> : " + msg.toString());
		}
		
		

		return msg;
	}



	protected char[] getCRC(int[] messageBytes) {
		return getCRC(messageBytes, messageBytes.length - 1);
	}

	protected char[] getCRC(int[] messageBytes, int length) {
		char result[] = new char[2];
		int end = length;
		int crc = 0;

		if (messageBytes[end] == TERMINATOR) {
			end = end - 4;
		} else if (messageBytes[end] == DELIMITER) {
			end = end - 3;
		}

		// System.err.println("end: " + end + " " + Arrays.toString(messageBytes));
		for (int cnt = 0; cnt <= end; cnt++) {
			crc = messageBytes[cnt] ^ crc;
			// System.err.print(" crc: " + Integer.toHexString(crc) + " '" + (char)
			// messageBytes[cnt] + "'[" + messageBytes[cnt] + "], ");
		}
		// System.err.println("- " + crc + "-" + Integer.toHexString(crc));

		String hex = Integer.toHexString(crc);
		if (hex.length() > 1) {
			result[0] = Character.toUpperCase(hex.charAt(0));
			result[1] = Character.toUpperCase(hex.charAt(1));
		} else {
			result[0] = '0';
			result[1] = Character.toUpperCase(hex.charAt(0));
		}
		return result;
	}

	protected boolean isMessageAToolbusMessage(int[] messageBytes) {
		return messageBytes[0] == '@';
	}

	protected String arrayToString(int[] data, int length) {
		String result = "";
		for (int idx = 0; idx <= length; idx++) {
			result += (char) data[idx];
		}
		return result;
	}
	
	public ToolbusCommand getCommand ( String command) {
		
		ICommandRegister register = this.getCommandRegister();
		
		if(register != null) {
			return (ToolbusCommand) register.getNewByName(command);
			
		}
		
		return null;
//		
//		ToolbusCommand msg = (ToolbusCommand) getCommandRegister().getNewByName(command);
//		
//		return   msg;
	}
}
