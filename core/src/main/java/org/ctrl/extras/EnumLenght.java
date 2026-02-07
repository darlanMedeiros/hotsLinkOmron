package org.ctrl.extras;

public enum EnumLenght {

    WORD(1), DWORD(2), QWORD(4);

    EnumLenght(int value) {
        setLength(value);
        
    }

    private int lenght; 
    
    
   
    public int getLenght() {
        return lenght;
    }

    private void setLength(int value){
        this.lenght = value;
    }
  
  
    

}
