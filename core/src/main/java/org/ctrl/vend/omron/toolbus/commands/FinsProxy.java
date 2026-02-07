/*
 * $Id: FinsProxy.java,v 1.2 2005/07/28 21:56:51 remus Exp $
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
import org.ctrl.vend.omron.fins.command.FinsCommand;
import org.ctrl.vend.omron.fins.command.FinsCommandRegister;


/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.2 $ $Date: 2005/07/28 21:56:51 $
 */
public class FinsProxy extends ToolbusCommand {
    
    public static final String NAME  = "FA";
    public static final String DESCRIPTION = "Execute a fins command";
    
    
    protected FinsCommand finsCommand;
    protected FinsCommandRegister finsRegister = new FinsCommandRegister();
    
    
    
    /**
     * 
     */
    public FinsProxy() {
        setData(new DataImp());
    }
    
    
    /**
     * @param command
     */
    public FinsProxy(IDevice target, FinsCommand command) {
        this();
        setTarget(target);
        setFinsCommand(command);
    }




    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandId()
     */
    public int getCommandId() {
        return 250;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandName()
     */
    public String getCommandName() {
        return NAME;
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.AbstractMessage#getCommandDescription()
     */
    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }

    /**
     * @return Returns the finsCommand.
     */
    public FinsCommand getFinsCommand() {
        return finsCommand;
    }

    /**
     * @param finsCommand The finsCommand to set.
     */
    public void setFinsCommand(FinsCommand finsCommand) {
        this.finsCommand = finsCommand;
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#formatData()
     */
    @Override
    public void formatData() {
        super.formatData();
        if( getFinsCommand() != null ) {
            int[] finsData = getFinsCommand().getFormatedData();
            int[] formattedData = new int[finsData.length + 1];
            System.arraycopy(finsData, 0, formattedData, 1, finsData.length);
            formattedData[0] = (char)'0';
            if( getData() instanceof DataImp ) {
                ((DataImp)getData()).set(formattedData);
            } else {
                setData(new DataImp(formattedData));
            }
        }
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#parseData()
     */
    @Override
    public void parseData() {
        super.parseData();
        //System.err.println("In parse data " + getReply().getLength() + " / " + getReply().toString());
        //String commandName = "" +  (char)getReply().toIntArray()[20] + (char)getReply().toIntArray()[21] + (char)getReply().toIntArray()[22] + (char)getReply().toIntArray()[24];        
        finsCommand.setReply(getReply());
        finsCommand.setTarget(getTarget());
        finsCommand.parseData();
        //setFinsCommand(finsMessage);                       
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#toString()
     */
    @Override
    public String toString() {
        return super.toString() + (getFinsCommand() == null ? "" : "\n\t\t" + getFinsCommand());
    }
    
    
    
    
    

}
