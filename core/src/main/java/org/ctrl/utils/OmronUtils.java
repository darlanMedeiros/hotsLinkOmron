/*
 *
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

package org.ctrl.utils;

/**
 *
 * @author JanCarel
 */
public class OmronUtils {

	/** Creates a new instance of OmronUtils */
	public OmronUtils() {
	}

	/**
	 * @param value the value for which a 4-long formatted HEX string is desired
	 */
	public String getFormatedHex(long value) {

		String result = Long.toHexString(value).toUpperCase();

		switch (result.length()) {
		case 1:
			return "000" + result;
		case 2:
			return "00" + result;
		case 3:
			return "0" + result;
		default:
			return result;
		}

	}

	public String getFormateHexWrite(int value) {
		String result = Integer.toHexString(value).toUpperCase();
		return result;
	}

	public String getFormatedDec(int value) {

		String result = Integer.toString(value).toUpperCase();

		switch (result.length()) {
		case 1:
			return "000" + result;
		case 2:
			return "00" + result;
		case 3:
			return "0" + result;
		default:
			return result;
		}

	}

	public String getFormatedBinary(int value) {

		String sb = Integer.toBinaryString(value);
		int len = 0;
		len = sb.length();
		while (len < 16) {
			sb = "0" + sb;
			len++;
		}
		return sb.toString();
	}

	public String getFormatedBinary(String value) {

		int valor = Integer.parseInt(value, 16);

		return getFormatedBinary(valor);

	}

	public boolean[] getBolleanBits(String value) {

		String aux = this.getFormatedBinary(value);

		char[] c = aux.toCharArray();
		boolean[] bol = new boolean[16];

		for (int i = 0; i < c.length; i++) {

			if (c[i] == '1') {
				bol[i] = true;
			} else {
				bol[i] = false;
			}
		}

		return bol;

	}

	public int convertToHexDec16(String value) {
		return Integer.parseInt(value, 16);
	}

	public Long convertStringHexDoubleToDec(String value){

		return Long.parseLong(value);
		
	}

	public String convertArrayBooleanToString(boolean[] bits) {
		String s = new String();

		for (int i = 0; i < bits.length; i++) {

			if (bits[i] == true) {
				s += '1';
			} else {
				s += '0';
			}

		}

		return s;

	}

}
