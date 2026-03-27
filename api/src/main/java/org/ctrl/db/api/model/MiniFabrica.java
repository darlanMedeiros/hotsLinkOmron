package org.ctrl.db.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MiniFabrica {

    private final Long id;
    private final String name;
    private final Long fabricaId;
    private final List<Long> setorIds;

    public MiniFabrica(Long id, String name, Long fabricaId, List<Long> setorIds) {
        this.id = id;
        this.name = name;
        this.fabricaId = fabricaId;
        this.setorIds = setorIds == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(setorIds));
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

    public List<Long> getSetorIds() {
        return setorIds;
    }
}
