package org.ctrl.db.api.dto;

public class SetorRequest {

    private String name;
    private Long miniFabricaId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMiniFabricaId() {
        return miniFabricaId;
    }

    public void setMiniFabricaId(Long miniFabricaId) {
        this.miniFabricaId = miniFabricaId;
    }
}
