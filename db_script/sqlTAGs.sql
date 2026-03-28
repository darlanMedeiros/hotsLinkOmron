-- Script final para PRENSA
-- Idempotente: pode rodar mais de uma vez.
-- Requer que as machines ja existam para o device PRENSA.

WITH input_data(tag_name, memory_name, address, machine_name) AS (
    VALUES
        ('PECAPH29','DM_0100',100,'PRENSA 29'),
        ('PECAPH30','DM_0102',102,'PRENSA 30'),
        ('PECAPH31','DM_0104',104,'PRENSA 31'),
        ('PECAROLLERCARGA41','DM_0106',106,'ROLLER CARGA 41'),
        ('PECAROLLERDESC41','DM_0108',108,'ROLLER DESC 41'),
        ('PECAROLLERCARGA42','DM_0110',110,'ROLLER CARGA 42'),
        ('PECAROLLERDESC42','DM_0112',112,'ROLLER DESC 42'),
        ('PECAROLLERCARGA43','DM_0114',114,'ROLLER CARGA 43'),
        ('PECAROLLERDESC43','DM_0116',116,'ROLLER DESC 43'),
        ('QUALIDADE41','DM_0118',118,'ESCOLHA 41'),
        ('QUALIDADE42','DM_0120',120,'ESCOLHA 42'),
        ('QUALIDADE43','DM_0122',122,'ESCOLHA 43'),
        ('PRODUCAO_PH29','DM_0308',308,'PRENSA 29'),
        ('PRODUCAO_PH30','DM_0309',309,'PRENSA 30'),
        ('PRODUCAO_PH31','DM_0310',310,'PRENSA 31'),
        ('PRODUCAO_SEC25','DM_0311',311,'SECADOR 25'),
        ('PRODUCAO_SEC26','DM_0312',312,'SECADOR 26'),
        ('PRODUCAO_SEC33','DM_0313',313,'SECADOR 33')
),
resolved AS (
    SELECT
        i.tag_name,
        i.memory_name,
        i.address,
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
    INSERT INTO public.memory (device_id, name, address)
    SELECT DISTINCT r.device_id, r.memory_name, r.address
    FROM resolved r
    ON CONFLICT (device_id, name)
    DO UPDATE SET address = EXCLUDED.address
    RETURNING id, device_id, name
),
cleanup_same_machine_name AS (
    DELETE FROM public.tag t
    USING resolved r, public.memory mem
    WHERE t.machine_id = r.machine_id
      AND t.name = r.tag_name
      AND mem.id = t.memory_id
      AND mem.device_id = r.device_id
      AND mem.name <> r.memory_name
)
INSERT INTO public.tag (name, machine_id, memory_id, persist_history)
SELECT
    r.tag_name,
    r.machine_id,
    mem.id,
    true
FROM resolved r
JOIN public.memory mem
  ON mem.device_id = r.device_id
 AND mem.name = r.memory_name
ON CONFLICT (memory_id)
DO UPDATE SET
    name = EXCLUDED.name,
    machine_id = EXCLUDED.machine_id,
    persist_history = EXCLUDED.persist_history;

-- Validacao (esperado: 12)
-- SELECT COUNT(*) AS total_ok
-- FROM public.tag t
-- JOIN public.machine mc ON mc.id = t.machine_id
-- JOIN public.device d ON d.id = mc.device_id
-- WHERE d.mnemonic = 'PRENSA'
--   AND t.name IN (
--     'PECAPH29','PECAPH30','PECAPH31',
--     'PECAROLLERCARGA41','PECAROLLERCARGA42','PECAROLLERCARGA43',
--     'PECAROLLERDESC41','PECAROLLERDESC42','PECAROLLERDESC43',
--     'QUALIDADE41','QUALIDADE42','QUALIDADE43'
--   );
