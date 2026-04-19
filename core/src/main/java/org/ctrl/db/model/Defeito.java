package org.ctrl.db.model;

public class Defeito {

    private final Long id;
    private final String name;
    private final Integer number;

    public Defeito(Long id, String name, Integer number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getNumber() {
        return number;
    }
}
