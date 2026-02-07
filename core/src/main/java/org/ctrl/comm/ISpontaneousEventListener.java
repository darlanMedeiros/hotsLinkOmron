/*
 * $Id: SpontaneousEventListener.java,v 1.1 2005/10/24 09:19:08 remus Exp $
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
 * @version $Revision: 1.1 $ $Date: 2005/10/24 09:19:08 $
 */
public interface ISpontaneousEventListener {
  
    
    
    /**
     * Se lanseaza de catre ComHandler in bucla treadului run. Astfel
     * daca vreunul din dispozitive transmite date spontan atuncea
     * ii permite teoretic sa sesizeze protocol handlerul asupra acestui
     * fapt si sa-i permita sa reactioneze dupa cum crede de cuvinta.
     */
    public void checkEvent();

}
