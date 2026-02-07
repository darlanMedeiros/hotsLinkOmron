/*
 *  Copyright [2006] [Remus Pereni http://remus.pereni.org]
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

import org.ctrl.IDevice;
import org.ctrl.comm.ComException;
import org.ctrl.comm.IComHandler;
import org.ctrl.utils.OmronUtils;
import org.ctrl.vend.omron.toolbus.ToolbusWordBit;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;

/**
 * Write a single bit to WR and read it back from RR.
 * Address format example: "10.00" (word.bit, bit 0-15).
 */
public class AreaReadWriteBit {

    private final OmronUtils utils = new OmronUtils();

    public int writeAndRead(IComHandler handler, IDevice plc, String addressBit, int bitValue)
            throws ComException {
        return writeAndRead(handler, plc, addressBit, bitValue, MemoryWrite.HEX);
    }

    public int writeAndRead(IComHandler handler, IDevice plc, String addressBit, int bitValue, int mode)
            throws ComException {
        ParsedAddress parsed = parseAddressBit(addressBit);
        int currentValue = readWordRR(handler, plc, parsed.address);
        int newWordValue = setBitValue(currentValue, parsed.bit, bitValue != 0);

        AreaWriteWR writeWR = new AreaWriteWR(plc, parsed.address, new int[] { newWordValue }, mode);
        handler.send(writeWR);

        return readBitRR(handler, plc, parsed.address, parsed.bit);
    }

    public int readBitRR(IComHandler handler, IDevice plc, String addressBit) throws ComException {
        ParsedAddress parsed = parseAddressBit(addressBit);
        return readBitRR(handler, plc, parsed.address, parsed.bit);
    }

    public int readBitRR(IComHandler handler, IDevice plc, int address, int bit) throws ComException {
        String reply = readWordRRHex(handler, plc, address);
        ToolbusWordBit wordBit = new ToolbusWordBit();
        wordBit.setWorldBits(address, reply);
        return wordBit.getWordBit(bit) ? 1 : 0;
    }

    private int readWordRR(IComHandler handler, IDevice plc, int address) throws ComException {
        String reply = readWordRRHex(handler, plc, address);
        return utils.convertToHexDec16(reply);
    }

    private String readWordRRHex(IComHandler handler, IDevice plc, int address) throws ComException {
        AreaReadRR readRR = new AreaReadRR(plc, address, 1);
        handler.send(readRR);
        String reply = readRR.getReply() == null ? null : readRR.getReply().toString();
        if (reply == null) {
            throw new IllegalStateException("RR read returned null reply for address " + address);
        }
        return reply;
    }

    private int setBitValue(int currentWord, int bit, boolean value) {
        String baseHex = utils.getFormateHexWrite(currentWord);
        ToolbusWordBit wordBit = new ToolbusWordBit();
        wordBit.setWorldBits(0, baseHex);
        wordBit.setBit(bit, value);
        return Integer.parseInt(wordBit.getBitToWorld(), 2);
    }

    private ParsedAddress parseAddressBit(String addressBit) {
        if (addressBit == null || addressBit.trim().isEmpty()) {
            throw new IllegalArgumentException("addressBit is required (ex: 10.00)");
        }
        String trimmed = addressBit.trim();
        int dot = trimmed.indexOf('.');
        if (dot <= 0 || dot == trimmed.length() - 1) {
            throw new IllegalArgumentException("addressBit must be in the format word.bit (ex: 10.00)");
        }
        String wordPart = trimmed.substring(0, dot);
        String bitPart = trimmed.substring(dot + 1);
        int address;
        int bit;
        try {
            address = Integer.parseInt(wordPart);
            bit = Integer.parseInt(bitPart);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("addressBit must be numeric (ex: 10.00)", ex);
        }
        if (bit < 0 || bit > 15) {
            throw new IllegalArgumentException("bit must be between 0 and 15");
        }
        return new ParsedAddress(address, bit);
    }

    private static final class ParsedAddress {
        private final int address;
        private final int bit;

        private ParsedAddress(int address, int bit) {
            this.address = address;
            this.bit = bit;
        }
    }
}
