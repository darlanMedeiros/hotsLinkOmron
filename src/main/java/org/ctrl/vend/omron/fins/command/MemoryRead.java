/*
 * $Id: MemmoryRead.java,v 1.2 2005/07/27 13:32:37 remus Exp $
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
package org.ctrl.vend.omron.fins.command;

import org.ctrl.DataImp;

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.2 $ $Date: 2005/07/27 13:32:37 $
 */
public class MemoryRead extends FinsCommand {
    
    
    public static final String DM_BIT = "02";
    public static final String DM_WORD = "82";
    

    public static final int      ID = 101;
    public static final String NAME = "0101";
    public static final String DESCRIPTION = "Memmory Area Read";
    
    
    protected int startAddress;
    protected int length=1;
    protected String memmoryArrea;
    
    
    /**
     * 
     */
    public MemoryRead() {
        setData(new DataImp());
    }    
        
    /**
     * @param length
     * @param arrea
     * @param address
     */
    public MemoryRead(String arrea, int address, int length) {
        this();
        this.length = length;
        memmoryArrea = arrea;
        setStartAddress(address);
    }






    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandId()
     */
    public int getCommandId() {
        return ID;
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
     * @return Returns the length.
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length The length to set.
     */
    public void setLength(int length) {
        this.length = length;
        ((DataImp)getData()).set(getMemmoryArrea() + getFormatedHex(getStartAddress()) + "00" + getFormatedHex(getLength()));
    }
    

    /**
     * @return Returns the memmoryArrea.
     */
    public String getMemmoryArrea() {
        return memmoryArrea;
    }

    /**
     * @param memmoryArrea The memmoryArrea to set.
     */
    public void setMemmoryArrea(String memmoryArrea) {
        this.memmoryArrea = memmoryArrea;
        ((DataImp)getData()).set(getMemmoryArrea() + getFormatedHex(getStartAddress()) + "00" + getFormatedHex(getLength()));
    }

    /**
     * @return Returns the startAddress.
     */
    public int getStartAddress() {
        return startAddress;
    }

    /**
     * @param startAddress The startAddress to set.
     */
    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
        ((DataImp)getData()).set(getMemmoryArrea() + getFormatedHex(getStartAddress()) + "00" + getFormatedHex(getLength()));
    }
    

    protected String getFormatedHex(int address) {
        
        String result = Integer.toHexString(address).toUpperCase();
        
        switch (result.length()) {
            case 1: return "000" + result;
            case 2: return "00" + result;
            case 3: return "0" + result;
            default:return result;
        }
       
    }

}
