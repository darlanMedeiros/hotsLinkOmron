package org.ctrl.db.api.model;

public class Setor {

    private final Long id;
    private final String name;

    public Setor(Long id, String name) {
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
