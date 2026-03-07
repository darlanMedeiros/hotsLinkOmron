package org.ctrl.db.api.model;

public class MiniFabrica {

    private final Long id;
    private final String name;
    private final Long fabricaId;

    public MiniFabrica(Long id, String name, Long fabricaId) {
        this.id = id;
        this.name = name;
        this.fabricaId = fabricaId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getFabricaId() {
        return fabricaId;
    }
}
