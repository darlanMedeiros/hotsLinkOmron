/*
 * $Id: Command.java,v 1.7 2005/10/24 09:15:51 remus Exp $
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
 * Interface describing a command to be executed by a device.
 * 
 * @author <a href="http://remus.pereni.org>Remus Pereni</a>
 */
public interface ICommand {
    
    
    /**
     * The identifier of the command to be executed. A command can be
     * eiter an byte / integer or a string, depending on the protocol
     * talked by the device. It is the protocol which knows which one 
     * to use, and because a command is strongly related to a protocol
     * it is the protocol related know-how which determines which one 
     * is important and will be used: getCommandId or getCommandName.
     * 
     * @return The id of the command to be executed.
     */
    int getCommandId();

    /**
     * The identifier of the command to be executed. A command can be
     * eiter an byte / integer or a string, depending on the protocol
     * talked by the device. It is the protocol which knows which one 
     * to use, and because a command is strongly related to a protocol
     * it is the protocol related know-how which determines which one 
     * is important and will be used: getCommandId or getCommandName.
     * 
     * @return The name of the command to be executed.
     */    
    String getCommandName();
    
    
    /**
     * A short description of the command.
     * @return The description of the command.
     */
    String getCommandDescription();

    
    /**
     * A way to enable the protocol to confirme the reception of the 
     * command.
     * 
     * @return True if the command was confirmed as received by the device
     * false otherwise.
     */
    boolean isCommandConfirmed();
    
            
    /**
     * A method to allow some commands or all of them to signalize to the
     * protocol handler that it expects a reply. Some protocols automatically
     * send a confirmation of the execution of any commands, others do not, so 
     * we have to ensure that if we are implementing a protocol which do not
     * confirm the execution of an command we can let know the protocol handler
     * if this one in particular expects a reply.
     * 
     * @return True if the executed command expects a reply, false otherwise.
     */
    boolean expectsReply();
    
    
    /**
     * Some protocols have an identifier sent or built in any command so each
     * command is unique and easily identifiable. Unfortunately not all protocols
     * do that, in case we are implementing a protocol which does not identify
     * the response to a command, we can, based on the protocol knowledge let know
     * the protocol handler what kind of reply do we expect after the command.
     *  
     * @return The id which can uniquely identify the expected reply message or type.
     */    
    int getExpectedReplyType();
}
