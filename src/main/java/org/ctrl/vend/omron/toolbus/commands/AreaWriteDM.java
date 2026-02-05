/*
 * AreaWriteDM.java
 *
 * Created on 9 januari 2006, 10:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.ctrl.vend.omron.toolbus.commands;

import org.ctrl.DataImp;
import org.ctrl.IDevice;
import org.ctrl.extras.MemoryVariable;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;

/**
 *
 * @author JanCarel
 */
public class AreaWriteDM extends MemoryWrite {
  
  public static final String NAME = "WD";
  public static final String DESCRIPTION = "DM Area Write";
  
  protected int address;
  protected int value;
  protected DataImp data=new DataImp();
  
  /** Creates a new instance of AreaWriteDM */
  public AreaWriteDM() {
    setData(data);
  }
  
  public AreaWriteDM(IDevice device) {
    setData(data);
    setTarget(device);
  }
  
  /**
   * @param address
   * @param value
   */
  public AreaWriteDM(IDevice device, int address, int[] value, int mode) {
    this();
    setMode(mode);
    setTarget(device);
    this.address = address;
    setValue(address, value,this.getMode());
  }
  
  /**
   * @param address
   * @param value
   */
  public AreaWriteDM(IDevice device, int address, int[] value) {
    this(device, address, value, MemoryWrite.HEX);
  }
  
      

  public AreaWriteDM(IDevice device, MemoryVariable variable, int[]value, int mode) {

    this(device, variable.getAddress(), value, mode);

}

/* (non-Javadoc)
       * @see org.pereni.ctrl.Command#getCommandId()
       */
  public int getCommandId() {
    return NAME.hashCode();
  }
  
    /* (non-Javadoc)
     * @see org.pereni.ctrl.Command#getCommandName()
     */
  public String getCommandName() {
    return NAME;
  }



}
