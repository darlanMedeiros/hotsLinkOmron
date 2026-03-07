package org.ctrl.db.api.model;

public class Fabrica {

    private final Long id;
    private final String name;

    public Fabrica(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
