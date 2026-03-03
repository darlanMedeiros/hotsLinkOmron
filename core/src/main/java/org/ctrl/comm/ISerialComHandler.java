
package org.ctrl.comm;

import org.ctrl.comm.serial.SerialParameters;
import org.ctrl.comm.serial.SerialPortException;


public interface ISerialComHandler extends Runnable {

    String NAME = "Serial Communication Handler";

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#getName()
     */
    String getName();

    /**
     * @return Returns the serialComParameters.
     */
    SerialParameters getSerialComParameters();

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#initialize()
     */
    void initialize();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    void run();

    void sendBreak(int millis);

    void setOutputBufferSize(int size);

    /**
     * @param serialComParameters The serialComParameters to set.
     */
    void setSerialComParameters(SerialParameters serialComParameters);

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#start()
     */
    void start() throws SerialPortException;

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#stop()
     */
    void stop();

}
