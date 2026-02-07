/*
 * $Id: ErrorRead.java,v 1.1 2005/07/05 16:21:13 remus Exp $
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
 * @version $Revision: 1.1 $ $Date: 2005/07/05 16:21:13 $
 */
public class ErrorRead extends ToolbusCommand {
    public static final String NAME = "MF";
    public static final String DESCRIPTION = "Error Read";        
    
        
    protected int              id = NAME.hashCode();    
    protected boolean  clearError = true;
    
    
    /**
     * 
     */
    public ErrorRead() {
        data = new DataImp("01");
    }

    
    public ErrorRead(IDevice target) {
        this();
        setTarget(target);
    }
    
    public ErrorRead(IDevice target, boolean clearError) {
        this();
        setTarget(target);
        setClearError(clearError);
    }
    
    
    /**
     * @return Returns the clearError.
     */
    public boolean isClearError() {
        return clearError;
    }


    /**
     * @param clearError The clearError to set.
     */
    public void setClearError(boolean clearError) {
        this.clearError = clearError;
        
        if( clearError ) {
            ((DataImp)data).set("01");
        }else {
            ((DataImp)data).set("00");
        }
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


}
