/*
 * $Id: DeviceImp.java,v 1.6 2005/07/05 19:15:45 remus Exp $
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

import javax.lang.model.SourceVersion;

/**
 * @author <a href="http://remus.pereni.org>Remus Pereni</a>
 *
 */
public class DeviceImp extends AbstractDevice {

    /**
     * 
     */
    public DeviceImp() {
        super();
    }

    /**
     * 
     *
     * @param id
     */
    public DeviceImp(int id, String mnemonic, String name, String description) {
        setId(id);
        setMnemonic(mnemonic);
        setName(name);
        setDescription(description);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IDevice))
            return false;

        return ((IDevice) obj).getId() == getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getName() + " id: " + getId();
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

}
