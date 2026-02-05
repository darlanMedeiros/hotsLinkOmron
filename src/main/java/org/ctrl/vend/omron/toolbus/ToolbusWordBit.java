package org.ctrl.vend.omron.toolbus;

import java.util.HashMap;
import java.util.Map;

import org.ctrl.utils.OmronUtils;

public class ToolbusWordBit {

	protected int address;
	protected boolean value = false;
	private OmronUtils utils = new OmronUtils();

	Map<Integer,ToolbusWordBit> worldBits = new HashMap<Integer,  ToolbusWordBit>();

	public ToolbusWordBit(int address) {

		this.address = address;

	}

	public ToolbusWordBit(int address, boolean value) {
		
		this.address = address;
		this.value = value;
		
	}

	public ToolbusWordBit() {
		
	}

	public void setWorldBits(int address, String value) {
		
	

		boolean[] bit = utils.getBolleanBits(value);
		
		int j = 0;

		for (int i = bit.length ; i > 0; i--) {
			
			//System.out.println(address + "."+ (j) + ": "+bit[i-1]);
			worldBits.put(j, this.getToolbusWordBit(address, bit[i-1]));
			j++;
		}

	}
	
	protected ToolbusWordBit getToolbusWordBit(int address, boolean value) {
		return new ToolbusWordBit(address, value );
	}

	@Override
	public String toString() {
		return "ToolbusWordBit [worldBits=" + worldBits.toString() + "]";
	}
	
	
	public boolean getWordBit(int bit) {
		
		return worldBits.get(bit).value;
		
	}
	
	public String getBitToWorld() {
		boolean[] bits = new boolean[16];
		
		int j = 15;
		
		for (int i = 0; i < 16; i++) {
			 bits[i] = worldBits.get(j).value;
			 j--;
			
		}
		
		return utils.convertArrayBooleanToString(bits);		
		
	}

	public void setBit(int bit, boolean value) {
		
		ToolbusWordBit worldBit = worldBits.get(bit);
		
		worldBit.setValue(value);
		
		worldBits.replace(bit, worldBit);
		
	}

	private void setValue(boolean value) {
		this.value = value;
		
	}
	
	

}
