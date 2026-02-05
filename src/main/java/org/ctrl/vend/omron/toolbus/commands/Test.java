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


import org.ctrl.DataImp;
import org.ctrl.IDevice;
import org.ctrl.comm.IStatusCode;
import org.ctrl.vend.omron.toolbus.ToolbusStatusRegister;


/**
 * @author Remus
 * 
 */
public class Test extends ToolbusCommand {

    public static final String NAME = "TS";

    public static final String DESCRIPTION = "Test Command";

    protected String testText = "";

    /**
     * 
     */
    public Test() {
        super();
        data = new DataImp();
    }

    public Test(IDevice device, String testText) {
        this();
        setTarget(device);
        setTestText(testText);
    }

    public Test(String testText) {
        this();
        setTestText(testText);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.comm.AbstractMessage#getCommandDescription()
     */
    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.Command#getCommandId()
     */
    public int getCommandId() {
        return 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pereni.ctrl.Command#getCommandName()
     */
    public String getCommandName() {
        return NAME;
    }

    /**
     * @return Returns the testText.
     */
    public String getTestText() {
        return testText;
    }

    /**
     * @param testText
     *            The testText to set.
     */
    public void setTestText(String testText) {
        this.testText = testText;
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#formatData()
     */
    @Override
    public void formatData() {
        super.formatData();
        ((DataImp)data).set(testText);     
        System.err.println("Am terminat format Data");
    }

    
    /* (non-Javadoc)
     * @see org.pereni.ctrl.vend.omron.toolbus.commands.ToolbusCommand#parseData(int[])
     */
    @Override
    public void parseData() {
        
        int[] messageBody = getReply().toHexArray();
        
        if( messageBody.length == 2) {
                IStatusCode  status = ToolbusStatusRegister.get( new String("" + (char)messageBody[0] + (char)messageBody[1]) );
            
                if( status.getId() != -1) {
                    System.err.println(" Reply status: " + status.toString());
                    setResponseStatusCode(status);
                } else {
                    testText = new String("" + (char)messageBody[0] + (char)messageBody[1]);
                    System.err.println("Raw status is: " + testText);
                }    
        } else {
            testText = "";
            for( int data : messageBody) {
                testText += (char) data;
            }

            System.err.println("Test Text is :" + testText);
        }
        
    }

    
}
