/*
 * $Id: ToolbusCommand.java,v 1.8 2005/10/24 13:56:13 remus Exp $
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
package org.ctrl.vend.omron.toolbus.commands;

import java.util.Arrays;

import org.ctrl.comm.AbstractMessage;

/**
 * An abstract implementation of the Message which should ease the
 * implementation of the Toolbus related commands/messages
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.8 $ $Date: 2005/10/24 13:56:13 $
 */
public abstract class ToolbusCommand extends AbstractMessage {

    public static final char DELIMITER = '*';
    public static final char TERMINATOR = '\r';
    public static final char START_MARKER = '@';

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#formatData()
     */
    public void formatData() {

    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#parseData(int[])
     */
    public void parseData() {

    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#isCommandConfirmed()
     */
    public boolean isCommandConfirmed() {
        return true;
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#getFormatedData()
     */
    public int[] getFormatedData() {
        int[] result = new int[1 + 2 + 2 + getData().getLength() + 2 + 2];
        result[0] = START_MARKER;
        if (getTarget().getId() > 9) {
            result[1] = ("" + getTarget().getId()).charAt(0);
            result[2] = ("" + getTarget().getId()).charAt(1);
        } else {
            result[1] = '0';
            result[2] = ("" + getTarget().getId()).charAt(0);
        }

        result[3] = getCommandName().charAt(0);
        result[4] = getCommandName().charAt(1);

        if (getData().getLength() > 0) {
            System.arraycopy(getData().toHexArray(), 0, result, 5, getData().getLength());
        }

        result[result.length - 2] = DELIMITER;
        result[result.length - 1] = TERMINATOR;

        char[] crc = getCRC(result);
        result[result.length - 3] = crc[1];
        result[result.length - 4] = crc[0];

        return result;
    }

    protected char[] getCRC(int[] messageBytes) {
        return getCRC(messageBytes, messageBytes.length - 1);
    }

    protected char[] getCRC(int[] messageBytes, int length) {
        char result[] = new char[2];
        int end = length;
        int crc = 0;

        if (messageBytes[end] == TERMINATOR) {
            end = end - 4;
        } else if (messageBytes[end] == DELIMITER) {
            end = end - 3;
        }

        //System.err.println("end: " + end + " " + Arrays.toString(messageBytes));
        for (int cnt = 0; cnt <= end; cnt++) {
            crc = messageBytes[cnt] ^ crc;
            // System.err.print(" crc: " + Integer.toHexString(crc) + " '" + (char) messageBytes[cnt] + "'[" + messageBytes[cnt] + "],  ");
        }
        //System.err.println("- " + crc + "-" + Integer.toHexString(crc));

        String hex = Integer.toHexString(crc);
        if (hex.length() > 1) {
            result[0] = Character.toUpperCase(hex.charAt(0));
            result[1] = Character.toUpperCase(hex.charAt(1));
        } else {
            result[0] = '0';
            result[1] = Character.toUpperCase(hex.charAt(0));
        }
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return " dev:" + getTarget().toString() + " cmd: " + getCommandName() + " val: " + Arrays.toString(getData().toHexArray()) + " / " + getData().toString();
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#expectsReply()
     */
    public boolean expectsReply() {
        return true;
    }

}
