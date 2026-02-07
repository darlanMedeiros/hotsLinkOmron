/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ctrl.comm;

/**
 *
 * @author darla
 */
public interface ISerialComHandler extends Runnable {

    String NAME = "Serial Communication Handler";

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.ComHandler#getName()
     */
    String getName();

    /**
     * @return Returns the serialComParameters.
     */
    SerialParameters getSerialComParameters();

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.ComHandler#initialize()
     */
    void initialize();

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    void run();

    void sendBreak(int millis);

    void setOutputBufferSize(int size);

    /**
     * @param serialComParameters The serialComParameters to set.
     */
    void setSerialComParameters(SerialParameters serialComParameters);

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.ComHandler#start()
     */
    void start();

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.ComHandler#stop()
     */
    void stop();
    
}
