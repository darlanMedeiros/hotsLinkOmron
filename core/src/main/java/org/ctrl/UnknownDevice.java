/*
 * $Id: UnknownDevice.java,v 1.3 2005/06/25 02:54:51 remus Exp $
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
package org.ctrl;

/**
 * @author <a href="http://remus.pereni.org>Remus Pereni</a>
 * 
 */
public class UnknownDevice extends DeviceImp {

    protected String identifier = "";
    
        
    /**
     * 
     */
    public UnknownDevice() {
        super();
        setId(-1);
        setMnemonic("");
        setName("unknown");        
        setDescription("Unknown Device");        
    }

    
    /**
     * 
     */
    public UnknownDevice(String identifier) {
        this();
        setIdentifier(identifier);
    }

    /**
     * @return Returns the identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier The identifier to set.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.AbstractDevice#getDescription()
     */
    @Override
    public String getDescription() {
        return super.getDescription() + " " + getIdentifier();
    }

  

    
    
    
}
