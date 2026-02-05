/*
 * $Id: FinsCommand.java,v 1.5 2005/10/24 13:55:07 remus Exp $
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
package org.ctrl.vend.omron.fins.command;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.ctrl.DataImp;
import org.ctrl.comm.AbstractMessage;
import org.ctrl.comm.IStatusCode;
import org.ctrl.vend.omron.fins.FinsStatusRegister;


/**
 *
 * @author <a href="http://remus.pereni.org">Remus Pereni</a>
 * @version $Revision: 1.5 $ $Date: 2005/10/24 13:55:07 $
 */
public abstract class FinsCommand extends AbstractMessage {

    protected int icf = 128;
    protected int rsv = 0;
    protected int gct = 2;
    protected int dna = 0;
    protected int da1 = 0;
    protected int da2 = 0;
    protected int sna = 0;
    protected int sa1 = 0;
    protected int sa2 = 252;
    protected int sid = 0;
    
    public static final DecimalFormat FORMAT = new DecimalFormat("0000");
    
    
    public void clear() {
        setIcf(128);
        setRsv(0);
        setGct(2);
        setDna(0);
        setDa1(0);
        setDa2(0);
        setSna(0);
        setSa1(0);
        setSa2(252);
        setSid(0);        
    }
   

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#parseData()
     */
    public void parseData() {
        int[] messageBody = getReply().toHexArray();  
        
        /* DBG: 
        System.err.println("Meswsage body length is " + messageBody.length + " / " + Arrays.toString(messageBody));
        for(int idx = 0 ; idx < messageBody.length ; idx++) {
            System.err.print((char) messageBody[idx]);
        }
        */
        
        if( messageBody.length > 28 ) {
            // ICF 
            setIcf(Integer.decode("0x" + (char)messageBody[0] + (char)messageBody[1]));
            
            // RSV
            setRsv(Integer.decode("0x" + (char)messageBody[2] + (char)messageBody[3]));
            
            // GCT
            setGct(Integer.decode("0x" + (char)messageBody[4] + (char)messageBody[5]));
            
            // DNA
            setDna(Integer.decode("0x" + (char)messageBody[6] + (char)messageBody[7]));

            // DA1
            setDa1(Integer.decode("0x" + (char)messageBody[8] + (char)messageBody[9]));
            
            // DA2
            setDa2(Integer.decode("0x" + (char)messageBody[10] + (char)messageBody[11]));
            
            // SNA
            setSna(Integer.decode("0x" + (char)messageBody[12] + (char)messageBody[13]));

            // SA1
            setSa1(Integer.decode("0x" + (char)messageBody[14] + (char)messageBody[15]));

            // SA2
            setSa2(Integer.decode("0x" + (char)messageBody[16] + (char)messageBody[17]));
            
            // SID
            setSid(Integer.decode("0x" + (char)messageBody[18] + (char)messageBody[19]));

                   
            IStatusCode  status = FinsStatusRegister.get( new String("" + (char)messageBody[24] + (char)messageBody[25]+ (char)messageBody[26]+ (char)messageBody[27]) );
            setResponseStatusCode(status); 
                      
            int[] realReply = new int[messageBody.length - 28];
            System.arraycopy(messageBody, 28, realReply, 0, realReply.length);
            
            /* DBG:
            System.err.println("\nReald data" );
            for(int idx = 0 ; idx < realReply.length ; idx++) {
                System.err.print((char) realReply[idx]);
            }
            System.err.println("");
            */
            
            if( getReply() instanceof DataImp ) { 
                ((DataImp)getReply()).set(realReply);
            } else {
                setReply(new DataImp(realReply));    
            }
        } else {
            // ICF 
            setIcf(Integer.decode("0x" + (char)messageBody[0] + (char)messageBody[1]));

            // DA2
            setDa2(Integer.decode("0x" + (char)messageBody[2] + (char)messageBody[3]));
            
            // SA2
            setSa2(Integer.decode("0x" + (char)messageBody[4] + (char)messageBody[5]));
            
            // SID
            setSid(Integer.decode("0x" + (char)messageBody[6] + (char)messageBody[7]));

                   
            IStatusCode  status = FinsStatusRegister.get( new String("" + (char)messageBody[12] + (char)messageBody[13]+ (char)messageBody[14]+ (char)messageBody[15]) );
            setResponseStatusCode(status); 
                      
            int[] realReply = new int[messageBody.length - 16];
            System.arraycopy(messageBody, 16, realReply, 0, realReply.length);
            
            if( getReply() instanceof DataImp ) { 
                ((DataImp)getReply()).set(realReply);
            } else {
                setReply(new DataImp(realReply));    
            }            
        }
        
        
    }

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#formatData()
     */
    public void formatData() {
        // TODO Auto-generated method stub
    }
    

    /* (non-Javadoc)
     * @see org.pereni.ctrl.comm.Message#getFormatedData()
     */
    public int[] getFormatedData() {
        String header =     getFormattedAs2Hex(getIcf())
                        +   getFormattedAs2Hex(getRsv())
                        +   getFormattedAs2Hex(getGct())
                        +   getFormattedAs2Hex(getDna())
                        +   getFormattedAs2Hex(getDa1())
                        +   getFormattedAs2Hex(getDa2())
                        +   getFormattedAs2Hex(getSna())
                        +   getFormattedAs2Hex(getSa1())
                        +   getFormattedAs2Hex(getSa2())
                        +   getFormattedAs2Hex(getSid())
                        +   getCommandName();
        
        
        int[] result = new int[header.length() +  getData().getLength()];
        
        int idx;
        for(idx = 0 ; idx < header.length(); idx++) {
            result[idx] = header.charAt(idx);
        }
        
        if( getData().getLength() > 0 ) {
            System.arraycopy(getData().toHexArray(), 0, result, idx, getData().getLength());            
        }
        
        return result;        
    }

 

    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#isCommandConfirmed()
     */
    public boolean isCommandConfirmed() {
        return true;
    }

    /**
     * @return Returns the da1.
     */
    public int getDa1() {
        return da1;
    }

    /**
     * @param da1 The da1 to set.
     */
    public void setDa1(int da1) {
        this.da1 = da1;
    }

    /**
     * @return Returns the da2.
     */
    public int getDa2() {
        return da2;
    }

    /**
     * @param da2 The da2 to set.
     */
    public void setDa2(int da2) {
        this.da2 = da2;
    }

    /**
     * @return Returns the dna.
     */
    public int getDna() {
        return dna;
    }

    /**
     * @param dna The dna to set.
     */
    public void setDna(int dna) {
        this.dna = dna;
    }

    /**
     * @return Returns the gct.
     */
    public int getGct() {
        return gct;
    }

    /**
     * @param gct The gct to set.
     */
    public void setGct(int gct) {
        this.gct = gct;
    }

    /**
     * @return Returns the icf.
     */
    public int getIcf() {
        return icf;
    }

    /**
     * @param icf The icf to set.
     */
    public void setIcf(int icf) {
        this.icf = icf;
    }

    /**
     * @return Returns the rsv.
     */
    public int getRsv() {
        return rsv;
    }

    /**
     * @param rsv The rsv to set.
     */
    public void setRsv(int rsv) {
        this.rsv = rsv;
    }

    /**
     * @return Returns the sa1.
     */
    public int getSa1() {
        return sa1;
    }

    /**
     * @param sa1 The sa1 to set.
     */
    public void setSa1(int sa1) {
        this.sa1 = sa1;
    }

    /**
     * @return Returns the sa2.
     */
    public int getSa2() {
        return sa2;
    }

    /**
     * @param sa2 The sa2 to set.
     */
    public void setSa2(int sa2) {
        this.sa2 = sa2;
    }

    /**
     * @return Returns the sid.
     */
    public int getSid() {
        return sid;
    }

    /**
     * @param sid The sid to set.
     */
    public void setSid(int sid) {
        this.sid = sid;
    }

    /**
     * @return Returns the sna.
     */
    public int getSna() {
        return sna;
    }

    /**
     * @param sna The sna to set.
     */
    public void setSna(int sna) {
        this.sna = sna;
    }

    
    protected String getFormattedAs2Hex(int val) {
        if ( val < 16 ) {
            return "0" + Integer.toHexString(val);
        }
        
        return Integer.toHexString(val);
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String result = " dev:" + getTarget() + " cmd: " + getCommandName() + " val: "  + Arrays.toString(getData().toHexArray()) + " / " + getData().toString() + "\n"
                + " icf: " + getIcf()
                + " rsv: " + getRsv()
                + " gct: " + getGct()
                + " dna: " + getDna()
                + " da1: " + getDa1()
                + " da2: " + getDa2()
                + " sna: " + getSna()
                + " sa1: " + getSa1()
                + " sa2: " + getSa2()
                + " sid: " + getSid()
                + " value: ";
        int[] data = getReply().toHexArray();
        for(int idx=0; idx < data.length; idx++ ) {
            result += (char) data[idx];
        }
        
        return result;
         
    }


    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#expectsReply()
     */
    public boolean expectsReply() {
        return true;
    }
    
    
}
