package org.ctrl.db.model;

public class DeviceInfo {

    private final String mnemonic;
    private final String name;
    private final String description;

    public DeviceInfo(String mnemonic, String name, String description) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("Device mnemonic is required");
        }
        this.mnemonic = mnemonic.trim();
        this.name = name == null ? "" : name.trim();
        this.description = description == null ? "" : description.trim();
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
