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
import org.apache.commons.logging.LogFactory;
import org.ctrl.IData;
import org.ctrl.IDevice;
import org.ctrl.extras.MemoryVariable;

/**
 * @author Remus
 * 
 */

public abstract class AbstractMessage implements IMessage {

    protected IStatusCode responseStatusCode = null;

    protected IDevice targetDevice;

    protected IData reply = null;

    protected IData data = null;

    protected boolean hasReply = false;

    protected Log log;

    protected MemoryVariable variable = null;

    public static int WORD = 1;
    public static int DWORD = 2;
    public static int QWORD = 4;

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#setTarget(org.pereni.ctrl.Device)
     */
    public void setTarget(IDevice target) {
        targetDevice = target;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#getTarget()
     */
    public IDevice getTarget() {
        return targetDevice;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#setData(org.pereni.ctrl.Data)
     */
    public void setData(IData data) {
        this.data = data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#getData()
     */
    public IData getData() {
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#hasReply()
     */
    public boolean hasReply() {
        return hasReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.Command#getCommandDescription()
     */
    @Override
    public String getCommandDescription() {
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#getReply()
     */
    @Override
    public IData getReply() {
        if (variable != null) {
            variable.setData(reply);
        }
        return reply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#setReply(org.pereni.ctrl.Data)
     */
    @Override
    public void setReply(IData replyRawData) {

        reply = replyRawData;
        if (replyRawData != null) {
            setHasReply(true);
            if (variable != null) {
                variable.setData(reply);
            }
        }

    }

    /**
     * @param hasReply The hasReply to set.
     */
    public void setHasReply(boolean hasReply) {
        this.hasReply = hasReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.Command#getExpectedReplyType()
     */
    public int getExpectedReplyType() {
        return -1;
    }

    /**
     * @return Returns the log.
     */
    public Log getLog() {
        if (log != null)
            return log;

        log = LogFactory.getLog("message." + getCommandName());
        return log;
    }

    /**
     * @param log The log to set.
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.Message#getResponseStatusCode()
     */
    public IStatusCode getResponseStatusCode() {
        return responseStatusCode;
    }

    /**
     * @param responseStatusCode The responseStatusCode to set.
     */
    public void setResponseStatusCode(IStatusCode responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    public MemoryVariable getVariable() {
        if ((variable) != null) {
            return variable;
        }
        return null;
    }

    public void setVariable(MemoryVariable variable) {
        
        this.variable = variable;
    }

}
