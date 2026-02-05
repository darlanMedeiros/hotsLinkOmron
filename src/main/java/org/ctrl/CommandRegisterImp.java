/*
 * $Id: CommandRegisterImp.java,v 1.3 2005/06/25 02:54:51 remus Exp $
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

import java.util.ArrayList;

import org.ctrl.comm.IMessage;



/**
 * @author <a href="http://remus.pereni.org>Remus Pereni</a>
 *
 */
public class CommandRegisterImp implements ICommandRegister {

    protected ArrayList<Integer>   commandIds = new ArrayList<Integer>();
    protected ArrayList<String>  commandNames = new ArrayList<String>();
    protected ArrayList<Class> commandClasses = new ArrayList<Class>();
    
    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.CommandRegister#addCommand(int, java.lang.String, java.lang.Class)
     */
    public void addCommand(int id, String name, Class commandClass) {
        if( name == null || commandClass == null ) return;
        commandIds.add(id);
        commandNames.add(name);
        commandClasses.add(commandClass);
    }
    

    /* (non-Javadoc)
     * @see org.pereni.ctrl.CommandRegister#addCommand(org.pereni.ctrl.comm.Message)
     */
    public void addCommand(IMessage message) {
        if( message == null || message.getCommandName() == null ) return;
        
        commandIds.add(message.getCommandId());
        commandNames.add(message.getCommandName());
        commandClasses.add(message.getClass());
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.CommandRegister#getNewById(int)
     */
    public IMessage getNewById(int id) {
        int idx = commandIds.indexOf(id);
        
        if( idx != -1 ) {
            return getNewByClass(commandClasses.get(idx));
        }
        
        return null;    
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.CommandRegister#getNewByName(java.lang.String)
     */
    public IMessage getNewByName(String name) {
        int idx = commandNames.indexOf(name);
        
        if( idx != -1 ) {
            return getNewByClass(commandClasses.get(idx));
        }
        
        return null;
    }
    
    
    @SuppressWarnings("deprecation")
	public IMessage getNewByClass(Class<?> commandClass ) {
        if( commandClass == null ) return null;
        
        IMessage result = null;
        
        try {
            
            result = (IMessage) commandClass.newInstance();
            
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    

}
