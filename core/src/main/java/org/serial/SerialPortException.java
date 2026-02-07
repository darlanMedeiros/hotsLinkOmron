package org.serial;






public class SerialPortException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public SerialPortException() {
		
	}
	

	public SerialPortException(Exception cause)  {
        super(cause);
    }

    public SerialPortException(String msg) throws SerialPortException {
        super(msg);
    }
    
    public SerialPortException (String msg, Exception causa) throws SerialPortException{
        super(msg, causa);
    }
}
