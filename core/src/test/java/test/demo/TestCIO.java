package test.demo;

import org.ctrl.utils.OmronUtils;

public class TestCIO {

    private static OmronUtils utils = new OmronUtils();

    public static void main(String args[]) {

        int value = 2147483647;
        int value2 = -2147483648;

        System.out.println(utils.getFormateHexWrite(value));
        System.out.println(utils.getFormateHexWrite(value2));

        System.out.println(value2 - value);

        


        
    }

    
}
