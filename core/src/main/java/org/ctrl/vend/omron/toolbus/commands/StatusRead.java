/*
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

import org.ctrl.DataImp;
import org.ctrl.IDevice;

/**
 * @author Remus
 *
 */
public final class StatusRead extends ToolbusCommand {

    public static final String NAME = "MS";
    public static final String DESCRIPTION = "Status Read";

    public static final int MODE_PROGRAM = 0;
    public static final int MODE_NONE = 1;
    public static final int MODE_RUN = 2;
    public static final int MODE_MONITOR = 3;

    protected int id = NAME.hashCode();
    protected String message = "";
    protected long statusData;
    protected int programAreaSize;
    protected int duplexSystem;
    protected int mode;

    protected boolean remoteIOWaitingForPowerApplication = false;
    protected boolean forcedSetResetInProgress = false;
    protected boolean errorDiagnosisInProgress = false;
    protected boolean startSwitchOf = false;
    protected boolean isDuplex = false;

    /**
     *
     */
    public StatusRead() {
        data = new DataImp(0);
    }

    public StatusRead(IDevice target) {
        this();
        setTarget(target);
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.AbstractMessage#getCommandDescription()
     */
    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandId()
     */
    public int getCommandId() {
        return id;
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandName()
     */
    public String getCommandName() {
        return NAME;
    }

    /**
     * @return Returns the message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return Returns the programAreaSize.
     */
    public int getProgramAreaSize() {
        return programAreaSize;
    }

    /**
     * @return Returns the duplexSystem.
     */
    public int getDuplexSystem() {
        return duplexSystem;
    }

    /**
     * @return Returns the errorDiagnosisInProgress.
     */
    public boolean isErrorDiagnosisInProgress() {
        return errorDiagnosisInProgress;
    }

    /**
     * @return Returns the forcedSetResetInProgress.
     */
    public boolean isForcedSetResetInProgress() {
        return forcedSetResetInProgress;
    }

    /**
     * @return Returns the isDuplex.
     */
    public boolean isDuplex() {
        return isDuplex;
    }

    /**
     * @return Returns the mode.
     */
    public int getMode() {
        return mode;
    }

    /**
     * @return Returns the remoteIOWaitingForPowerApplication.
     */
    public boolean isRemoteIOWaitingForPowerApplication() {
        return remoteIOWaitingForPowerApplication;
    }

    /**
     * @return Returns the startSwitchOf.
     */
    public boolean isStartSwitchOf() {
        return startSwitchOf;
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#parseData()
     */
    @Override
    public void parseData() {
        super.parseData();
        statusData = Long.decode("0x" + new String(getReply().toHexArray(), 0, 4));
        programAreaSize = (int) statusData & 0x007f;
        duplexSystem = (int) statusData & 0x0080;

        int msb = (int) statusData >> 8;
        mode = msb & 0x03;
        remoteIOWaitingForPowerApplication = (msb & 4) == 1;
        forcedSetResetInProgress = (msb & 8) == 1;
        errorDiagnosisInProgress = (msb & 16) == 1;
        startSwitchOf = (msb & 32) == 1;
        isDuplex = (msb & 64) == 1;

        // DBG:
        System.err.println("size " + programAreaSize);
        System.err.println("duplex " + duplexSystem);
        System.err.println(Integer.toHexString(msb) + " / " + Integer.toBinaryString(msb));        
        System.err.println("mode " + mode);
        System.err.println("remote " + remoteIOWaitingForPowerApplication);
        System.err.println("forced " + forcedSetResetInProgress);
        System.err.println("diagnosis " + errorDiagnosisInProgress);
        System.err.println("startSwitch " + startSwitchOf);
        System.err.println("isDuplex " + isDuplex);
       //  */
    }

}
