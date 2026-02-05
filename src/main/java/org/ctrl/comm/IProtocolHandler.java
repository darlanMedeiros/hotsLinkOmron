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

import org.apache.commons.logging.Log;
import org.ctrl.ICommandRegister;


/**
 * @author Remus
 *
 */
public interface IProtocolHandler {

    String getProtocolName();
    
    void setLog(Log log);
    
    Log getLog();
        
    void setComControl(IComControl comControl);
    
    IComControl getComControl();
    
    IMessage write(IMessage message) throws ComException;
    
    IMessage read(IMessage originalCommand) throws ComException;
    
    ICommandRegister getCommandRegister();
}
