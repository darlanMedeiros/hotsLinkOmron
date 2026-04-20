package org.ctrl.db.model;

public class QualidadeDefeitoValor {

    private final Long id;
    private final Long qualidadeId;
    private final Long defeitoId;
    private final String defeitoName;
    private final Integer value;

    public QualidadeDefeitoValor(Long id, Long qualidadeId, Long defeitoId, String defeitoName, Integer value) {
        this.id = id;
        this.qualidadeId = qualidadeId;
        this.defeitoId = defeitoId;
        this.defeitoName = defeitoName;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Long getQualidadeId() {
        return qualidadeId;
    }

    public Long getDefeitoId() {
        return defeitoId;
    }

    public String getDefeitoName() {
        return defeitoName;
    }

    public Integer getValue() {
        return value;
    }
}
