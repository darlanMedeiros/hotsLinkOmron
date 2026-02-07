/*
 * $Id: StatusCode.java,v 1.3 2005/10/24 13:54:55 remus Exp $
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
 * An interface describing how the returned status of a message or command
 * should be reprezented. The actual implementations of this are protocol dependent.
 * 
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.3 $ $Date: 2005/10/24 13:54:55 $
 */
public interface IStatusCode {
    
    final int UNKNOWN = 0;
    final int   ERROR = 1;
    final int WARNING = 2;
    final int    INFO = 3;
    
    
    final static String CODE_TIME_OUT = "TIME OUT";
    final static String CODE_UNKNOWN = "UNKNOWN";
    
    /**
     * Because each protocol can have integer or string reprezentations of an 
     * status code this interface impose that each status code have an integer
     * or String identifier of the status code.
     * 
     * @return the id of the status code if it has one, the hash code of the 
     * getCode value otherwise (which should be unique this way).
     */
    int getId();
    
    
    /**
     * Because each protocol can have integer or string reprezentations of an 
     * status code this interface impose that each status code have an integer
     * or String identifier of the status code.
     * 
     * @return the code of the status code if it has one, the String version of
     * the id otherwise
     */
    String getCode();
    
    
    
    /**
     * Allows to mark a status code with a severity.
     * @returns an integer indicating the severity of the status code.
     * @see {StatusCode.INFO}
     */
    int getSeverity();
    
    
    /**
     * A human understandable description of the status code.
     * @return a String description of the status code.
     */
    String getDescription();

}
