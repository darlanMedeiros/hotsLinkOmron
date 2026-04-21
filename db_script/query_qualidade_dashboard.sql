-- Consulta para o Dashboard de Qualidade
-- Retorna os registros de qualidade enriquecidos com nomes de máquina, turno e detalhes dos defeitos

SELECT 
    q.id AS qualidade_id,
    q.hora AS data_hora,
    m.name AS maquina,
    t.name AS turno,
    q.value AS quantidade_amostragem_total,
    d.name AS defeito,
    qdv.amostragem AS quantidade_amostragem_defeito,
    qdv.value AS quantidade_defeito
FROM 
    public.qualidade q
JOIN 
    public.machine m ON q.machine_id = m.id
JOIN 
    public.turno t ON q.turno_id = t.id
LEFT JOIN 
    public.qualidade_defeito_valor qdv ON q.id = qdv.qualidade_id
LEFT JOIN 
    public.defeito d ON qdv.defeito_id = d.id
ORDER BY 
    q.hora DESC, q.id DESC;
