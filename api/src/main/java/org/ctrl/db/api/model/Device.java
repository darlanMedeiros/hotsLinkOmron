package org.ctrl.db.api.model;

public class Device {

    private final Integer id;
    private final String mnemonic;
    private final String name;
    private final String description;
    private final Integer nodeId;

    public Device(Integer id, String mnemonic, String name, String description, Integer nodeId) {
        this.id = id;
        this.mnemonic = mnemonic;
        this.name = name;
        this.description = description;
        this.nodeId = nodeId;
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

    public Integer getNodeId() {
        return nodeId;
    }
}
