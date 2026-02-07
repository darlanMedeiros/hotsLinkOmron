/*
 * MemoryVariable.java
 *
 *
 *
 */

package org.ctrl.extras;

import java.util.ArrayList;

import org.ctrl.IData;
import org.ctrl.vend.omron.toolbus.memory.MemoryRead;

/**
 *
 * @author JanCarel
 */
public class MemoryVariable {

  private String name;
  private String area;
  private int address;
  private int[] value;
  private IData data;
  private int lenght;
  protected ArrayList<Integer> valueAux;

  /**
   * Creates a new instance of MemoryVariable.<br>
   * A memory variable has a String memory area e.g. DM HR AR etc and an address
   * in this memory area.
   */
  public MemoryVariable(String name, String memoryArea, int memoryAddress, int lenght) {
    this.lenght = lenght;
    this.address = memoryAddress;
    this.area = memoryArea;
    this.setName(name);
  }

  public int[] getValue() {

    if (!valueAux.isEmpty()) {

      this.value = new int[valueAux.size()];

      for (int i = 0; i < valueAux.size(); i++) {
        this.value[i] = valueAux.get(i);
      }
      return this.value;
    }

    return null;
  }

  public void setValue(int value, int start) {
    if (this.valueAux == null) {
      this.valueAux = new ArrayList<>();
      this.valueAux.add(start, value);

    } else {
      this.valueAux.add(start, value);

    }

  }

  public String getName() {
    return name;
  }

  private void setName(String name) {
    this.name = name;
  }

  /**
   * @return the area of this memory variable
   */
  public String getArea() {
    return area;
  }

  /**
   * @return the address of this memory variable in the relevant memory area.
   */
  public int getAddress() {
    return address;
  }

  public String toString() {
    return area + address;
  }

  public void setVariable(MemoryRead memoryRead) {
    memoryRead.setAddress(getAddress());
  }

  public void setData(IData reply) {
    this.data = reply;
  }
  public String getStringValue() {
    // this.value = data.getLength();
    return data.toString();
  }
  public int getIntValue(){
    return data.toInteger();
  }

  public int getLength() {
    return lenght;
  }

  public void setType(int lenght) {
    this.lenght = lenght;
  }

}
