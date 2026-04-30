package org.ctrl.db.api.model;

public class QualityRoleOption {

    private final Integer id;
    private final String name;
    private final String description;
    private final Boolean active;

    public QualityRoleOption(Integer id, String name, String description, Boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return active;
    }
}

