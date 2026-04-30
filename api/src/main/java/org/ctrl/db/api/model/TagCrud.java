package org.ctrl.db.api.model;

public class TagCrud {

    private final Integer id;
    private final String name;
    private final Long machineId;
    private final Integer memoryId;
    private final Boolean persistHistory;
    private final String qualityGroup;
    private final String qualityRole;

    public TagCrud(Integer id, String name, Long machineId, Integer memoryId, Boolean persistHistory,
            String qualityGroup, String qualityRole) {
        this.id = id;
        this.name = name;
        this.machineId = machineId;
        this.memoryId = memoryId;
        this.persistHistory = persistHistory;
        this.qualityGroup = qualityGroup;
        this.qualityRole = qualityRole;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMachineId() {
        return machineId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public Boolean getPersistHistory() {
        return persistHistory;
    }

    public String getQualityGroup() {
        return qualityGroup;
    }

    public String getQualityRole() {
        return qualityRole;
    }
}
