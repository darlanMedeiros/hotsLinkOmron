/*
 * $Id: StatusWrite.java,v 1.2 2005/07/28 21:56:52 remus Exp $
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

import org.ctrl.DataImp;
import org.ctrl.IDevice;

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.2 $ $Date: 2005/07/28 21:56:52 $
 */
public class StatusWrite extends ToolbusCommand {
    
    public static final String NAME = "SC";
    public static final String DESCRIPTION = "Status Write";        
    
    
    public static final int MODE_PROGRAM = 0;
    public static final int MODE_RUN = 3;
    public static final int MODE_MONITOR = 2;
    
        
    protected int     id = NAME.hashCode();    
    protected int status = 0;
    
    
    /**
     * 
     */
    public StatusWrite() {
        data = new DataImp("00");
    }

    
    public StatusWrite(IDevice target) {
        this();
        setTarget(target);
    }
    
    public StatusWrite(IDevice target, int newStatus) {
        this();
        setTarget(target);
        setStatus(newStatus);
    }
                

    /**
     * @return Returns the status.
     */
    public int getStatus() {
        return status;
    }


    /**
     * @param status The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
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


    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#formatData()
     */
    @Override
    public void formatData() {
        ((DataImp)data).set("0" + status);
    }
    
    

}
