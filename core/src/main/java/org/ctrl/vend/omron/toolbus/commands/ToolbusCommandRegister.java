/*
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

import org.ctrl.CommandRegisterImp;

/**
 * @author Remus
 *
 */
public class ToolbusCommandRegister extends CommandRegisterImp {
  
  /**
   *Cria um array das classes
   */
  public ToolbusCommandRegister() {
    addCommand(new Test());
    addCommand(new StatusRead());
    addCommand(new AreaReadFC());
    addCommand(new AreaReadDM());
    addCommand(new AreaReadHR());
    addCommand(new AreaReadRR());
    addCommand(new PCModelRead());
    addCommand(new Abort());
    addCommand(new ErrorRead());
    addCommand(new StatusWrite());
    addCommand(new FinsProxy());
    addCommand(new AreaWriteDM());
    addCommand(new AreaWriteHR());
    addCommand(new AreaWriteWR());
    addCommand(new AreaReadTC());
  }
  
}
