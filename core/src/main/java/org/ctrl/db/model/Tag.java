package org.ctrl.db.model;

public class Tag {

    private final Integer id;
    private final String name;
    private final long machineId;
    private final int memoryId;

    public Tag(Integer id, String name, long machineId, int memoryId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        this.id = id;
        this.name = name;
        this.machineId = machineId;
        this.memoryId = memoryId;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getMachineId() {
        return machineId;
    }

    public int getMemoryId() {
        return memoryId;
    }
}
