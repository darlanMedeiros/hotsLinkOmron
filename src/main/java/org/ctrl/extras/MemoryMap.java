/*
 * MemoryMap.java
 *
 * Created on 9 januari 2006, 16:19
 *
 */

package org.ctrl.extras;

import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ctrl.IData;
import org.ctrl.vend.omron.toolbus.memory.MemoryRead;

/**
 *
 * @author JanCarel
 */
public class MemoryMap {

  private Log log;
  private HashMap<String, MemoryVariable> memory;
  public static final int HEX = 1;
  public static final int BCD = 2;

  /**
   * Creates a new instance of MemoryMap This is a hashmap with all the memory
   * variables that have been read. Convenience method to be able to look up the
   * variables quickly.
   */
  protected MemoryMap() {

    memory = new HashMap<String, MemoryVariable>();
    log = LogFactory.getLog(getClass().getName());
  }

  /**
   * handle to the instance of this memory map
   */
  private static MemoryMap _instance;

  /**
   * @return the singleton MemoryMap
   */
  public static MemoryMap getInstance() {
    if (_instance == null) {
      _instance = new MemoryMap();
    }
    return _instance;
  }

  /**
   * add a value to the memory map
   * 
   * @param m   the memory variable to add
   * @param val the value of this memory variable
   */
  public void addValue(MemoryVariable m, int val, int start) {
    if (memory.get(m.getName()) != null) {
      m.setValue(val, start);
    } else {
      m.setValue(val, start);
      memory.put(m.getName(), m);
    }

  }

  /**
   * get a value for a memoryvariable can also be io area
   * 
   * @param m the memory variable to get the value of
   */
  public int[] getValue(MemoryVariable m) {
    if (memory.get(m.getName()) != null) {
      return memory.get(m.getName()).getValue();
    }
    return new int[1];
  }

  /**
   * pop data from a reply into the memory map
   */
  public void process(String name, MemoryRead mr, int mode) {
    IData m = mr.getReply();
    String whichMemory = "";
    if (mr.getCommandName().equalsIgnoreCase("RD")) {
      whichMemory = "DM";
    } else if (mr.getCommandName().equalsIgnoreCase("RH")) {
      whichMemory = "HR";
    } else if (mr.getCommandName().equalsIgnoreCase("RR")) {
      whichMemory = "IO";
    } else {
      log.error("could not place memory area for command name " + mr.getCommandName());
      return;
    }
    int[] dataBuff = m.toHexArray();
    int start = mr.getAddress();
    int length = mr.getLength();
    for (int i = 0; i < length; i++) {
      String val = "";
      for (int j = 0; j < 4; j++) {
        val = val + (char) dataBuff[i * 4 + j];
      }
      int h = 0;
      if (mode == BCD) {
        try {
          h = Integer.parseInt(val.trim());
        } catch (NumberFormatException ex) {
          log.debug("NumberFormatException in parsing BCD values");
        }
      } else {
        try {
          /* convert a hex String to int -- Note, there is no lead 0x, case insensitive */
          h = Integer.parseInt(val.trim(), 16 /* radix */ );
        } catch (NumberFormatException ex) {
          log.debug("NumberFormatException in parsing HEX values");
        }
      }
      // addValue(new MemoryVariable(name, whichMemory, start + i,start+i), h);

    }
  }

  public void addVariable(String name, String memoryArea, int[] value, int mode, int address) {

    addValue(new MemoryVariable(name, memoryArea, address, 1), value, mode);
  }

  public void addVariable(MemoryVariable variable, int[] value, int mode) {

    String val = "";
    int valueAux = 0;
    if (value.length == 4) {
      for (int i = 0; i < value.length; i++) {

        val = val + (char) value[i];

      }
      if (mode == BCD) {
        try {
          valueAux = Integer.parseInt(val.trim());
        } catch (NumberFormatException ex) {
          log.debug("NumberFormatException in parsing BCD values");
        }
      } else {
        try {
          /* convert a hex String to int -- Note, there is no lead 0x, case insensitive */
          valueAux = Integer.parseInt(val.trim(), 16 /* radix */ );
        } catch (NumberFormatException ex) {
          log.debug("NumberFormatException in parsing HEX values");
        }
      }

      addValue(variable, valueAux, 0);

    } else {

     
      int length = value.length/4;
      for (int i = 0; i < length; i++) {
        val = "";
        for (int j = 0; j < 4; j++) {
          val = val + (char) value[i * 4 + j];
        }
        valueAux = 0;
        if (mode == BCD) {
          try {
            valueAux = Integer.parseInt(val.trim());
          } catch (NumberFormatException ex) {
            log.debug("NumberFormatException in parsing BCD values");
          }
        } else {
          try {
            /* convert a hex String to int -- Note, there is no lead 0x, case insensitive */
            valueAux = Integer.parseInt(val.trim(), 16 /* radix */ );
          } catch (NumberFormatException ex) {
            log.debug("NumberFormatException in parsing HEX values");
          }

        }
        addValue(variable, valueAux, i);
      }

    }

  }

  public void addVariable(MemoryVariable variable) {
    memory.put(variable.getName(), variable);
  }

  public int[] getValue(String name) {

    if (memory.get(name) != null) {
      return memory.get(name).getValue();
    }
    return new int[1];
  }

  /**
   * pop data from a reply into the memory map
   */

  public void addValue(MemoryVariable variable, int[] value, int mode) {
    addVariable(variable, value, mode);
  }

}
