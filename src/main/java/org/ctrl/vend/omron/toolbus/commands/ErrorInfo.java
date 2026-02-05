/*
 * $Id: ErrorInfo.java,v 1.1 2005/07/05 16:21:13 remus Exp $
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
package org.ctrl.vend.omron.toolbus.commands;

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.1 $ $Date: 2005/07/05 16:21:13 $
 */
public class ErrorInfo {

	protected boolean fals = false;
	protected boolean endInstructionMissing = false;
	protected boolean programError = false;
	protected boolean HLUTransmisionError = false;
	protected boolean RTIInstructionError = false;
	protected boolean PCLinkError = false;
	protected boolean IOBusError = false;
	protected boolean JMPInstructionError = false;
	protected boolean memoryError = false;

	/**
	 * 
	 */
	public ErrorInfo() {

	}

	/**
	 * @return Returns the endInstructionMissing.
	 */
	public boolean isEndInstructionMissing() {
		return endInstructionMissing;
	}

	/**
	 * @return Returns the fals.
	 */
	public boolean isFals() {
		return fals;
	}

	/**
	 * @return Returns the hLUTransmisionError.
	 */
	public boolean isHLUTransmisionError() {
		return HLUTransmisionError;
	}

	/**
	 * @return Returns the iOBusError.
	 */
	public boolean isIOBusError() {
		return IOBusError;
	}

	/**
	 * @return Returns the jMPInstructionError.
	 */
	public boolean isJMPInstructionError() {
		return JMPInstructionError;
	}

	/**
	 * @return Returns the memoryError.
	 */
	public boolean isMemoryError() {
		return memoryError;
	}

	/**
	 * @return Returns the pCLinkError.
	 */
	public boolean isPCLinkError() {
		return PCLinkError;
	}

	/**
	 * @return Returns the programError.
	 */
	public boolean isProgramError() {
		return programError;
	}

	/**
	 * @return Returns the rTIInstructionError.
	 */
	public boolean isRTIInstructionError() {
		return RTIInstructionError;
	}

	/**
	 * @param endInstructionMissing The endInstructionMissing to set.
	 */
	void setEndInstructionMissing(boolean endInstructionMissing) {
		this.endInstructionMissing = endInstructionMissing;
	}

	/**
	 * @param fals The fals to set.
	 */
	void setFals(boolean fals) {
		this.fals = fals;
	}

	/**
	 * @param transmisionError The hLUTransmisionError to set.
	 */
	void setHLUTransmisionError(boolean transmisionError) {
		HLUTransmisionError = transmisionError;
	}

	/**
	 * @param busError The iOBusError to set.
	 */
	void setIOBusError(boolean busError) {
		IOBusError = busError;
	}

	/**
	 * @param instructionError The jMPInstructionError to set.
	 */
	void setJMPInstructionError(boolean instructionError) {
		JMPInstructionError = instructionError;
	}

	/**
	 * @param memoryError The memoryError to set.
	 */
	void setMemoryError(boolean memoryError) {
		this.memoryError = memoryError;
	}

	/**
	 * @param linkError The pCLinkError to set.
	 */
	void setPCLinkError(boolean linkError) {
		PCLinkError = linkError;
	}

	/**
	 * @param programError The programError to set.
	 */
	void setProgramError(boolean programError) {
		this.programError = programError;
	}

	/**
	 * @param instructionError The rTIInstructionError to set.
	 */
	void setRTIInstructionError(boolean instructionError) {
		RTIInstructionError = instructionError;
	}

}
