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
package org.ctrl.comm;

import java.io.IOException;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

/**
 * @author Remus
 *
 */
public class SerialComHandlerImp extends AbstractComHandler implements ISerialComHandler {

    protected SerialParameters serialComParameters = null;
    protected CommPortIdentifier portId = null;
    protected SerialPort serialPort = null;
    protected boolean shutDownHookRegistered = false;
    protected String serialConName;

    /**
     * @param parameters
     */
    public SerialComHandlerImp(SerialParameters parameters, String serialConName) {
        super();
        serialComParameters = parameters;
        this.serialConName = serialConName;
    }

    /**
     * @param parameters
     * @param handler
     */
    public SerialComHandlerImp(SerialParameters parameters, IProtocolHandler handler) {
        super();
        serialComParameters = parameters;
        protocolHandler = handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#initialize()
     */
    @Override
    public void initialize() {
        getLog().info(NAME + " initialized");

        if (!shutDownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    terminate();
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#start()
     */
    @Override
    public void start() {
        stopRequired = false;
        try {
            openConnection();
            // connection.serialPort.addEventListener(reader);d
            Thread thread = new Thread(this);
            thread.start();
        } catch (ComException ex) {
            getLog().error("Unable to open connection", ex);
            // changed this as system.exit is probably too rigorous
            // System.exit(1);
            return;
        }
        getLog().info("Serial Comunication Handler Started");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#stop()
     */
    @Override
    public void stop() {
        stopRequired = true;
        closeConnection();
        getLog().info("Serial Comunication Handler Stoped");
    }

    protected void terminate() {
        stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.ComHandler#getName()
     */
    @Override
    public String getName() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        getLog().info(getName() + " thread started");
        while (stopRequired != true) {
            try {
                Thread.sleep(SLEEP_TIME);
                if (protocolHandler instanceof ISpontaneousEventListener) {
                    ((ISpontaneousEventListener) protocolHandler).checkEvent();
                }
            } catch (InterruptedException ex) {
                throw new ComException("InterruptedException" + ex);

            }
        }

        getLog().info(getName() + " thread stopped");

    }

    /**
     * @return Returns the serialComParameters.
     */
    @Override
    public SerialParameters getSerialComParameters() {
        return serialComParameters;
    }

    /**
     * @param serialComParameters The serialComParameters to set.
     */
    @Override
    public void setSerialComParameters(SerialParameters serialComParameters) {
        this.serialComParameters = serialComParameters;
    }

    /**
     * Attempts to open a serial connection and streams using the parameters in the
     * SerialParameters object. If it is unsuccesfull at any step it returns the
     * port to a closed state, throws a <code>SerialConnectionException</code>, and
     * returns.
     *
     * Gives a timeout of 30 seconds on the portOpen to allow other applications to
     * reliquish the port if have it open and no longer need it.
     */
    protected void openConnection() throws ComException {

        // Obtain a CommPortIdentifier object for the port you want to open.
        try {
            portId = CommPortIdentifier.getPortIdentifier(serialComParameters.getPortName());
        } catch (NoSuchPortException ex) {
            getLog().info(" no such port exception for com port " + serialComParameters.getPortName());
            // ex.printStackTrace();
            throw new ComException(ex.getMessage());
        }

        // Open the port represented by the CommPortIdentifier object. Give
        // the open call a relatively long timeout of 30 seconds to allow
        // a different application to reliquish the port if the user
        // wants to.
        try {
            serialPort = (SerialPort) portId.open(serialConName, 3000);
        } catch (PortInUseException e) {
            throw new ComException(e.getMessage());
        }

        // Set the parameters of the connection. If they won't set, close the
        // port before throwing an exception.
        try {
            setConnectionParameters();
        } catch (ComException e) {
            serialPort.close();
            throw e;
        }

        // Open the input and output streams for the connection. If they won't
        // open, close the port before throwing an exception.
        try {
            out = serialPort.getOutputStream();
            in = serialPort.getInputStream();
            if (protocolHandler != null) {
                protocolHandler.setComControl(this);
            }
        } catch (IOException e) {
            serialPort.close();
            throw new ComException("Error opening i/o streams");
        }

        isStarted = true;
    }

    @Override
    public void sendBreak(int millis) {
        serialPort.sendBreak(millis);
    }

    @Override
    public void setOutputBufferSize(int size) {
        serialPort.setOutputBufferSize(size);
    }

    /**
     * Sets the connection parameters to the setting in the parameters object. If
     * set fails return the parameters object to origional settings and throw
     * exception.
     */
    protected void setConnectionParameters() throws ComException {

        // Save state of parameters before trying a set.
        int oldBaudRate = serialPort.getBaudRate();
        int oldDatabits = serialPort.getDataBits();
        int oldStopbits = serialPort.getStopBits();
        int oldParity = serialPort.getParity();
        int oldFlowControl = serialPort.getFlowControlMode();

        // Set connection parameters, if set fails return parameters object
        // to original state.
        try {
            serialPort.setSerialPortParams(serialComParameters.getBaudRate(), serialComParameters.getDatabits(),
                    serialComParameters.getStopbits(), serialComParameters.getParity());
        } catch (UnsupportedCommOperationException e) {
            e.printStackTrace();
            serialComParameters.setBaudRate(oldBaudRate);
            serialComParameters.setDatabits(oldDatabits);
            serialComParameters.setStopbits(oldStopbits);
            serialComParameters.setParity(oldParity);
            serialComParameters.setFlowControlIn(oldFlowControl);
            throw new ComException("Unsupported parameter");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Set flow control.
        try {
            serialPort.setFlowControlMode(
                    serialComParameters.getFlowControlIn() | serialComParameters.getFlowControlOut());
        } catch (UnsupportedCommOperationException e) {
            e.printStackTrace();
            throw new ComException("Unsupported parameter");
        }
    }

    /**
     * Close the port and clean up associated elements.
     */
    protected void closeConnection() {
        // If port is alread closed just return.
        if (!isStarted) {
            return;
        }

        // Check to make sure sPort has reference to avoid a NPE.
        if (serialPort != null) {
            try {
                // close the i/o streams.
                out.close();
                in.close();
            } catch (IOException e) {
                System.err.println(e);
            }

            // Close the port.
            serialPort.close();

            // Remove the ownership listener.
            // portId.removePortOwnershipListener(this);
        }

        isStarted = false;

    }

}
