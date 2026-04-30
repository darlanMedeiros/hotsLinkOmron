-- Script completo de TAGs para PRENSA (inclui producao, status e qualidade completa)
-- Idempotente: pode rodar mais de uma vez.
-- Pre-requisito: device PRENSA e machines ja cadastradas.

WITH input_data(tag_name, memory_area, address, bit, machine_name, persist_history, quality_group, quality_role) AS (
    VALUES
        -- PRODUCAO / CONTADORES
        ('PECAPH29','DM',100,-1,'PRENSA 29',true,NULL,NULL),
        ('PECAPH30','DM',102,-1,'PRENSA 30',true,NULL,NULL),
        ('PECAPH31','DM',104,-1,'PRENSA 31',true,NULL,NULL),
        ('PECAROLLERCARGA41','DM',106,-1,'ROLLER CARGA 41',true,NULL,NULL),
        ('PECAROLLERDESC41','DM',108,-1,'ROLLER DESC 41',true,NULL,NULL),
        ('PECAROLLERCARGA42','DM',110,-1,'ROLLER CARGA 42',true,NULL,NULL),
        ('PECAROLLERDESC42','DM',112,-1,'ROLLER DESC 42',true,NULL,NULL),
        ('PECAROLLERCARGA43','DM',114,-1,'ROLLER CARGA 43',true,NULL,NULL),
        ('PECAROLLERDESC43','DM',116,-1,'ROLLER DESC 43',true,NULL,NULL),
        ('QUALIDADE41','DM',118,-1,'ESCOLHA 41',true,NULL,NULL),
        ('QUALIDADE42','DM',120,-1,'ESCOLHA 42',true,NULL,NULL),
        ('QUALIDADE43','DM',122,-1,'ESCOLHA 43',true,NULL,NULL),
        ('PRODUCAO_PH29','DM',308,-1,'PRENSA 29',true,NULL,NULL),
        ('PRODUCAO_PH30','DM',309,-1,'PRENSA 30',true,NULL,NULL),
        ('PRODUCAO_PH31','DM',310,-1,'PRENSA 31',true,NULL,NULL),
        ('PRODUCAO_SEC25','DM',311,-1,'SECADOR 25',true,NULL,NULL),
        ('PRODUCAO_SEC26','DM',312,-1,'SECADOR 26',true,NULL,NULL),
        ('PRODUCAO_SEC33','DM',313,-1,'SECADOR 33',true,NULL,NULL),

        -- STATUS PRENSA (RR)
        ('STATUS_PH29','RR',0,-1,'PRENSA 29',false,NULL,NULL),
        ('STATUS_PH30','RR',1,-1,'PRENSA 30',false,NULL,NULL),
        ('STATUS_PH31','RR',2,-1,'PRENSA 31',false,NULL,NULL),

        -- QUALIDADE COMPLETA - ESCOLHA 41
        ('Qualidade_Gatilho','DM',500,-1,'ESCOLHA 41',false,'ESCOLHA_41','TRIGGER'),
        ('Total_Defeitos1','DM',501,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_TOTAL_1'),
        ('Total_Defeitos2','DM',502,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_TOTAL_2'),
        ('Total_Defeitos3','DM',503,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_TOTAL_3'),
        ('Codigo_Defeito1','DM',504,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_CODE_1'),
        ('Codigo_Defeito2','DM',505,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_CODE_2'),
        ('Codigo_Defeito3','DM',506,-1,'ESCOLHA 41',true,'ESCOLHA_41','DEFECT_CODE_3'),
        ('Qtde_Amostragem','DM',507,-1,'ESCOLHA 41',true,'ESCOLHA_41','SAMPLING'),
        ('Qualidade_Ano','DM',508,-1,'ESCOLHA 41',true,'ESCOLHA_41','YEAR'),
        ('Qualidade_Mes','DM',509,-1,'ESCOLHA 41',true,'ESCOLHA_41','MONTH'),
        ('Qualidade_Dia','DM',510,-1,'ESCOLHA 41',true,'ESCOLHA_41','DAY'),
        ('Qualidade_Hora','DM',511,-1,'ESCOLHA 41',true,'ESCOLHA_41','HOUR'),
        ('Qualidade_Min','DM',512,-1,'ESCOLHA 41',true,'ESCOLHA_41','MINUTE'),
        ('Qualidade_Seg','DM',513,-1,'ESCOLHA 41',true,'ESCOLHA_41','SECOND'),
        ('Qualidade_Maquina_Current','DM',514,-1,'ESCOLHA 41',false,'ESCOLHA_41','MACHINE_QUALITY_CURRENT'),
        ('Qualidade_Maquina_Persisted','DM',515,-1,'ESCOLHA 41',true,'ESCOLHA_41','MACHINE_QUALITY_PERSISTED'),

        -- QUALIDADE COMPLETA - ESCOLHA 42
        ('Qualidade_Gatilho','DM',520,-1,'ESCOLHA 42',false,'ESCOLHA_42','TRIGGER'),
        ('Total_Defeitos1','DM',521,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_TOTAL_1'),
        ('Total_Defeitos2','DM',522,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_TOTAL_2'),
        ('Total_Defeitos3','DM',523,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_TOTAL_3'),
        ('Codigo_Defeito1','DM',524,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_CODE_1'),
        ('Codigo_Defeito2','DM',525,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_CODE_2'),
        ('Codigo_Defeito3','DM',526,-1,'ESCOLHA 42',true,'ESCOLHA_42','DEFECT_CODE_3'),
        ('Qtde_Amostragem','DM',527,-1,'ESCOLHA 42',true,'ESCOLHA_42','SAMPLING'),
        ('Qualidade_Ano','DM',528,-1,'ESCOLHA 42',true,'ESCOLHA_42','YEAR'),
        ('Qualidade_Mes','DM',529,-1,'ESCOLHA 42',true,'ESCOLHA_42','MONTH'),
        ('Qualidade_Dia','DM',530,-1,'ESCOLHA 42',true,'ESCOLHA_42','DAY'),
        ('Qualidade_Hora','DM',531,-1,'ESCOLHA 42',true,'ESCOLHA_42','HOUR'),
        ('Qualidade_Min','DM',532,-1,'ESCOLHA 42',true,'ESCOLHA_42','MINUTE'),
        ('Qualidade_Seg','DM',533,-1,'ESCOLHA 42',true,'ESCOLHA_42','SECOND'),
        ('Qualidade_Maquina_Current','DM',534,-1,'ESCOLHA 42',false,'ESCOLHA_42','MACHINE_QUALITY_CURRENT'),
        ('Qualidade_Maquina_Persisted','DM',535,-1,'ESCOLHA 42',true,'ESCOLHA_42','MACHINE_QUALITY_PERSISTED'),

        -- QUALIDADE COMPLETA - ESCOLHA 43
        ('Qualidade_Gatilho','DM',540,-1,'ESCOLHA 43',false,'ESCOLHA_43','TRIGGER'),
        ('Total_Defeitos1','DM',541,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_TOTAL_1'),
        ('Total_Defeitos2','DM',542,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_TOTAL_2'),
        ('Total_Defeitos3','DM',543,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_TOTAL_3'),
        ('Codigo_Defeito1','DM',544,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_CODE_1'),
        ('Codigo_Defeito2','DM',545,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_CODE_2'),
        ('Codigo_Defeito3','DM',546,-1,'ESCOLHA 43',true,'ESCOLHA_43','DEFECT_CODE_3'),
        ('Qtde_Amostragem','DM',547,-1,'ESCOLHA 43',true,'ESCOLHA_43','SAMPLING'),
        ('Qualidade_Ano','DM',548,-1,'ESCOLHA 43',true,'ESCOLHA_43','YEAR'),
        ('Qualidade_Mes','DM',549,-1,'ESCOLHA 43',true,'ESCOLHA_43','MONTH'),
        ('Qualidade_Dia','DM',550,-1,'ESCOLHA 43',true,'ESCOLHA_43','DAY'),
        ('Qualidade_Hora','DM',551,-1,'ESCOLHA 43',true,'ESCOLHA_43','HOUR'),
        ('Qualidade_Min','DM',552,-1,'ESCOLHA 43',true,'ESCOLHA_43','MINUTE'),
        ('Qualidade_Seg','DM',553,-1,'ESCOLHA 43',true,'ESCOLHA_43','SECOND'),
        ('Qualidade_Maquina_Current','DM',554,-1,'ESCOLHA 43',false,'ESCOLHA_43','MACHINE_QUALITY_CURRENT'),
        ('Qualidade_Maquina_Persisted','DM',555,-1,'ESCOLHA 43',true,'ESCOLHA_43','MACHINE_QUALITY_PERSISTED')
),
resolved AS (
    SELECT
        i.tag_name,
        i.memory_area,
        i.address,
        i.bit,
        i.persist_history,
        i.quality_group,
        i.quality_role,
        d.id AS device_id,
        m.id AS machine_id
    FROM input_data i
    JOIN public.device d
      ON d.mnemonic = 'PRENSA'
    JOIN public.machine m
      ON m.device_id = d.id
     AND m.name = i.machine_name
),
upsert_memory AS (
    INSERT INTO public.memory (device_id, name, address, bit)
    SELECT DISTINCT r.device_id, r.memory_area, r.address, r.bit
    FROM resolved r
    ON CONFLICT (device_id, name, address, bit)
    DO NOTHING
    RETURNING id
),
cleanup_same_machine_name AS (
    DELETE FROM public.tag t
    USING resolved r, public.memory mem
    WHERE t.machine_id = r.machine_id
      AND t.name = r.tag_name
      AND mem.id = t.memory_id
      AND mem.device_id = r.device_id
      AND (mem.name <> r.memory_area OR mem.address <> r.address OR mem.bit <> r.bit)
)
INSERT INTO public.tag (name, machine_id, memory_id, persist_history, quality_group, quality_role)
SELECT
    r.tag_name,
    r.machine_id,
    mem.id,
    r.persist_history,
    r.quality_group,
    r.quality_role
FROM resolved r
JOIN public.memory mem
  ON mem.device_id = r.device_id
 AND mem.name = r.memory_area
 AND mem.address = r.address
 AND mem.bit = r.bit
ON CONFLICT (memory_id)
DO UPDATE SET
    name = EXCLUDED.name,
    machine_id = EXCLUDED.machine_id,
    persist_history = EXCLUDED.persist_history,
    quality_group = EXCLUDED.quality_group,
    quality_role = EXCLUDED.quality_role;
