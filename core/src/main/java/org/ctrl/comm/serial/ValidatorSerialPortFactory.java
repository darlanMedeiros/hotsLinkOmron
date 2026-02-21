package org.ctrl.comm.serial;

import java.util.Objects;

public class ValidatorSerialPortFactory implements Comparable<ValidatorSerialPortFactory> {
    final private String factoryClassname;
    final private String connectorClassname;

    public ValidatorSerialPortFactory(String connectorClassname, String factoryClassname) {
        this.factoryClassname = factoryClassname;
        this.connectorClassname = connectorClassname;
    }

    public String getFactoryClassname() {
        return factoryClassname;
    }

    public String getConnectorClassname() {
        return connectorClassname;
    }

    public boolean validate() {
        try {
            Class.forName(getConnectorClassname());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public SerialPortAbstractFactory getFactory() throws ClassNotFoundException {
        if (validate()) {
            try {
                Class<?> factoryClass = Class.forName(getFactoryClassname());
                Object o = factoryClass.getConstructors()[0].newInstance();
                if (o instanceof SerialPortAbstractFactory) {
                    return (SerialPortAbstractFactory) o;
                }
            } catch (Exception e) {
               
            }
        }
        throw new ClassNotFoundException();
    }

    @Override
    public int compareTo(ValidatorSerialPortFactory o) {
        if (o == null) {
            return 1;
        }
        int byConnector = this.connectorClassname.compareTo(o.connectorClassname);
        if (byConnector != 0) {
            return byConnector;
        }
        return this.factoryClassname.compareTo(o.factoryClassname);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValidatorSerialPortFactory)) {
            return false;
        }
        ValidatorSerialPortFactory other = (ValidatorSerialPortFactory) obj;
        return Objects.equals(this.factoryClassname, other.factoryClassname)
                && Objects.equals(this.connectorClassname, other.connectorClassname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factoryClassname, connectorClassname);
    }
}
