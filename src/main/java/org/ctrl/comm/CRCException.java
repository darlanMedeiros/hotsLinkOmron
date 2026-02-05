/*
 * $Id: CRCException.java,v 1.1 2005/07/05 19:15:45 remus Exp $
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

/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.1 $ $Date: 2005/07/05 19:15:45 $
 */
public class CRCException extends ComException {

    int[] messageBody;
    
    /**
     * 
     */
    private static final long serialVersionUID = 7717877878756658700L;

    /**
     * 
     */
    public CRCException() {
        super();
    }

    
    
    
    /**
     * @param body
     */
    public CRCException(int[] body) {
        super();
        messageBody = body;
    }




    /**
     * @param message
     */
    public CRCException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public CRCException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public CRCException(Throwable cause) {
        super(cause);
    }

    /**
     * @return Returns the messageBody.
     */
    public int[] getMessageBody() {
        return messageBody;
    }

    /**
     * @param messageBody The messageBody to set.
     */
    public void setMessageBody(int[] messageBody) {
        this.messageBody = messageBody;
    }

    
}
