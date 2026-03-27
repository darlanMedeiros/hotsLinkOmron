package org.ctrl.db.api.dto;

import java.util.List;

public class MiniFabricaRequest {

    private String name;
    private Long fabricaId;
    private List<Long> setorIds;

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

    public List<Long> getSetorIds() {
        return setorIds;
    }

    public void setSetorIds(List<Long> setorIds) {
        this.setorIds = setorIds;
    }
}
