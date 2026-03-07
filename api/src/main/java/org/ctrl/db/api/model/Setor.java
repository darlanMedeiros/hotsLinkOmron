package org.ctrl.db.api.model;

public class Setor {

    private final Long id;
    private final String name;
    private final Long miniFabricaId;

    public Setor(Long id, String name, Long miniFabricaId) {
        this.id = id;
        this.name = name;
        this.miniFabricaId = miniFabricaId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMiniFabricaId() {
        return miniFabricaId;
    }
}
