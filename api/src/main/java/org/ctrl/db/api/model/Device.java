package org.ctrl.db.api.model;

public class Device {

    private final Integer id;
    private final String mnemonic;
    private final String name;
    private final String description;

    public Device(Integer id, String mnemonic, String name, String description) {
        this.id = id;
        this.mnemonic = mnemonic;
        this.name = name;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
