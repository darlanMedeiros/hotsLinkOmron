/*
 * $Id: FinsStatusRegister.java,v 1.1 2005/07/27 09:52:02 remus Exp $
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
package org.ctrl.vend.omron.fins;

import java.util.ArrayList;

import org.ctrl.comm.IStatusCode;
import org.ctrl.comm.StatusCodeImp;
import org.ctrl.comm.StatusCodeUnknown;

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.1 $ $Date: 2005/07/27 09:52:02 $
 */
public final class FinsStatusRegister {

    protected static ArrayList<IStatusCode> statusCodeStore = new ArrayList<IStatusCode>();
    protected static ArrayList<String> codeStore = new ArrayList<String>();
    protected static ArrayList<Integer> idStore = new ArrayList<Integer>();

    /**
     * Command Completed Normally
     */
    public static final IStatusCode COMPLETE_NORMALY = new StatusCodeImp(0, "0000", IStatusCode.INFO, "Command Completed Normally");

    static {
        add(COMPLETE_NORMALY);
    }

    /**
     * Adds an status code to the register;
     *
     * @param code The status code to be added;
     */
    protected static void add(IStatusCode code) {
        if (code == null || code.getCode() == null) {
            throw new IllegalStateException("Can not add null status code to the register");
        }

        idStore.add(code.getId());
        codeStore.add(code.getCode());
        statusCodeStore.add(code);
    }

    public static IStatusCode get(int id) {
        int idx = idStore.indexOf(id);
        if (idx != -1) {
            return statusCodeStore.get(idx);
        }

        return StatusCodeUnknown.getInstance();
    }

    public static IStatusCode get(String code) {
        int idx = codeStore.indexOf(code);
        if (idx != -1) {
            return statusCodeStore.get(idx);
        }

        return StatusCodeUnknown.getInstance();
    }
}
