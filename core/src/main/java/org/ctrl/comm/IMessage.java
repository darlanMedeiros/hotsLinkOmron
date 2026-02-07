/*
 * $Id: Message.java,v 1.6 2005/07/05 19:15:45 remus Exp $
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
package org.ctrl.comm;

import org.ctrl.ICommand;
import org.ctrl.IData;
import org.ctrl.IDevice;

/**
 * An interface describing the communication/command between a device and
 * the controller. The exact implementation is protocol dependent.
 * 
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.6 $ $Date: 2005/07/05 19:15:45 $
 */
public interface IMessage extends ICommand {

    /**
     * Sets the device to whom the message is targeted.
     * @param target The device which should receive this message.
     */
    void setTarget(IDevice target);
    
    /**
     * Returns the device to whom the message is targeted.
     * @return The device which should receive this message
     */
    IDevice getTarget();
        
    /**
     * Sets the raw data to be sent to the device.
     * @param data An implementation of row data, the exact type of data is protocol dependent.
     */
    void setData(IData data);
    
    
    /**
     * Returns the raw data which is/was to be sent to the device.
     * @return An implementation of row data, the exact type of data is protocol dependent.
     */
    IData getData();
    
    
    /**
     * Flag indicating if the message received a reply which contains data.
     * @return true if the message got a reply, false otherwise.
     */
    boolean hasReply();
    
    
    /**
     * Returns the raw data which was recived from the device.
     * @return  An implementation of row data, the exact type of data is protocol dependent.
     */
    IData getReply();
    
    /**
     * Sets the raw data which was received from the devices.
     * @param replyRawData  An implementation of row data, the exact type of data is protocol dependent.
     */
    void setReply(IData replyRawData);
    
    
    /**
     * Allows the determination of the result of a message or command.
     * @return an protocol dependent implementation of the status code.
     */
    IStatusCode getResponseStatusCode();
    
    /**
     * Responsible for extracting and conversion of the information
     * from the raw data into the proper information fields of the 
     * specific command. It works on the replyed data.
     */
    void parseData();
    
    
    /**
     * Responsible for extracting the information from the command specific
     * fields and creating the raw data to be sent which is protocol specific.
     * It works on the data to be sent. 
     */
    void formatData();
    
    
    
    /**
     * Allows the protocol to obtain the exact bytes that have to be sent.
     * @return an int array containing the exact bytes that will be sent.
     */
    int[] getFormatedData();
}
