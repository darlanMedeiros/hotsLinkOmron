/*
 * $Id: AbstractDevice.java,v 1.3 2005/06/25 02:54:51 remus Exp $
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
 * 
 */
package org.ctrl;

/**
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 *
 */
public abstract class AbstractDevice implements IDevice {

    protected int id = -1;
    protected String name = "";    
    protected String mnemonic = "";
    protected String description = "";
    

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#getId()
     */
    
    
    @Override
    public int getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#setId(int)
     */
    @Override
    public void setId(int id) {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#getMnemonic()
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#setMnemonic(java.lang.String)
     */
    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Device#setDescription(java.lang.String)
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
