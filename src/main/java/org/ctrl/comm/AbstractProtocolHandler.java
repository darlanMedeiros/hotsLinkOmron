/**
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
package org.ctrl.comm;

import org.apache.commons.logging.Log;
import org.ctrl.ICommandRegister;

/**
 * @author Remus
 *
 */
public abstract class AbstractProtocolHandler implements IProtocolHandler {

	protected Log log = null;
	protected IComControl comControl = null;
	protected ICommandRegister commandRegister = null;
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#setLog(Log)
	 */
	public void setLog(Log log) {
		if (log != null)
			this.log = log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#getLog()
	 */
	public Log getLog() {
		return log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#setComControl(org.pereni.ctrl.comm.
	 * ComControl)
	 */
	public void setComControl(IComControl comControl) {
						
		if (comControl != null) {
			this.comControl = comControl;
		} else {
			throw new IllegalStateException("ComControl in Protocol Hanlder is null");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pereni.ctrl.comm.ProtocolHandler#getComControl()
	 */
	public IComControl getComControl() {
		return comControl;
	}

	/**
	 * @return Returns the commandRegister.
	 */
	public ICommandRegister getCommandRegister() {
		return commandRegister;
	}

	/**
	 * @param commandRegister The commandRegister to set.
	 */
	protected void setCommandRegister(ICommandRegister commandRegister) {
		this.commandRegister = commandRegister;
	}

}
