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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Remus
 *
 */
public abstract class AbstractComHandler implements IComHandler, IComControl {

    protected Log log = LogFactory.getLog("ComHandler");
    protected OutputStream out;
    protected InputStream in;
    protected IProtocolHandler protocolHandler;
    protected boolean isStarted = false;
    protected boolean stopRequired = false;
    protected ArrayList<IMessageListener> listeners = new ArrayList<IMessageListener>();
    protected boolean specificReceiveInProgress = false;
    protected int numberOfRetries = 1;
    protected long communicationTimeOut = 2000;

    public int SLEEP_TIME = 500;

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#isStarted()
     */
    public boolean isStarted() {
        return isStarted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#addMessageListener(org.pereni.ctrl.comm.
     * MessageListener)
     */
    public void addMessageListener(IMessageListener listener) {
        if (listener != null)
            listeners.add(listener);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.pereni.ctrl.comm.ComHandler#removeMessageListener(org.pereni.ctrl.comm.
     * MessageListener)
     */
    public void removeMessageListener(IMessageListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.pereni.ctrl.comm.ComHandler#fireNewMessage(org.pereni.ctrl.comm.Message)
     */
    public void fireNewMessage(IMessage message) {
        if (message == null)
            return;

        for (IMessageListener listener : listeners) {
            listener.messageReceived(message);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#setLog(Log)
     */
    public void setLog(Log log) {
        if (log != null)
            this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#getLog()
     */
    public Log getLog() {
        return log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#setProtocolHandler(org.pereni.ctrl.comm.
     * ProtocolHandler)
     */
    public void setProtocolHandler(IProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#getProtocolHandler()
     */
    public IProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#getInputStream()
     */
    public InputStream getInputStream() {
        return in;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return out;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#send(org.pereni.ctrl.comm.Message)
     */
    public IMessage send(IMessage message) throws ComException {

        // System.out.println("DEBUG ABSTRACT MESSAGE" + message);

        if (message != null && getProtocolHandler() != null && isStarted) {

            return getProtocolHandler().write(message);
        } else if (getProtocolHandler() == null) {
            throw new IllegalStateException("Unknown protocol handler, plese set one");
        } else if (!isStarted) {
            throw new RuntimeException("Com handler is not started, unable to send message");
        }

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#receive()
     */
    // public Message receive() throws ComException {
    //     Message result = null;

    //     if (getProtocolHandler() != null && isStarted) {
    //         setSpecificReceiveInProgress(true);
    //         result = getProtocolHandler().read();
    //         setSpecificReceiveInProgress(false);
    //     } else if (getProtocolHandler() == null) {
    //         throw new IllegalStateException("Unknown protocol handler, plese set one");
    //     } else if (!isStarted) {
    //         throw new RuntimeException("Com handler is not started, unable to send message");
    //     }
    //     return result;
    // }

    /**
     * @return Returns the specificReceiveInProgress.
     */
    protected boolean isSpecificReceiveInProgress() {
        return specificReceiveInProgress;
    }

    /**
     * @param specificReceiveInProgress The specificReceiveInProgress to set.
     */
    protected void setSpecificReceiveInProgress(boolean specificReceiveInProgress) {
        this.specificReceiveInProgress = specificReceiveInProgress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#getCommunicationTimeOut()
     */
    public long getCommunicationTimeOut() {
        return communicationTimeOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#getNumberOfRetries()
     */
    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#setCommunicationTimeOut(long)
     */
    public void setCommunicationTimeOut(long milisec) {
        this.communicationTimeOut = milisec;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComControl#setNumberOfRetries(int)
     */
    public void setNumberOfRetries(int retries) {
        this.numberOfRetries = retries;
    }

    // Metodo implementado

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    // Metodo implementado
    public void setInputStream(InputStream in) {
        this.in = in;
    }

}
