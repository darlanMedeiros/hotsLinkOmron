package org.ctrl.db.api.dto;

public class MiniFabricaRequest {

    private String name;
    private Long fabricaId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFabricaId() {
        return fabricaId;
    }

    public void setFabricaId(Long fabricaId) {
        this.fabricaId = fabricaId;
    }
}
