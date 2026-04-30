package org.ctrl.db.api.dto;

public class TagCrudRequest {

    private String name;
    private Long machineId;
    private Integer memoryId;
    private Boolean persistHistory;
    private String qualityGroup;
    private String qualityRole;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Integer memoryId) {
        this.memoryId = memoryId;
    }

    public Boolean getPersistHistory() {
        return persistHistory;
    }

    public void setPersistHistory(Boolean persistHistory) {
        this.persistHistory = persistHistory;
    }

    public String getQualityGroup() {
        return qualityGroup;
    }

    public void setQualityGroup(String qualityGroup) {
        this.qualityGroup = qualityGroup;
    }

    public String getQualityRole() {
        return qualityRole;
    }

    public void setQualityRole(String qualityRole) {
        this.qualityRole = qualityRole;
    }
}
