-- Queries de apoio para PLCs, TAGs e valores atuais/historico
-- Banco: PostgreSQL
-- Schema esperado: public

-- 1) PLCs cadastrados
SELECT
  d.id,
  d.mnemonic,
  d.name,
  d.description
FROM public.device d
ORDER BY d.id;

-- 2) TAGs salvas por PLC (mapeamento tag -> memoria)
SELECT
  d.id       AS device_id,
  d.mnemonic AS plc_mnemonic,
  d.name     AS plc_name,
  t.id       AS tag_id,
  t.name     AS tag_name,
  m.id       AS memory_id,
  m.name     AS memory_name
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
ORDER BY d.id, t.name;

-- 3) TAGs com valor atual (memory_value_current)
SELECT
  d.id           AS device_id,
  d.mnemonic     AS plc_mnemonic,
  t.name         AS tag_name,
  m.name         AS memory_name,
  mvc.value      AS current_value,
  mvc.status     AS current_status,
  mvc.updated_at AS current_updated_at
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
ORDER BY d.id, t.name;

-- 4) Buscar uma TAG por nome em todos os PLCs
-- Exemplo: PECAROLLERCARGA41
SELECT
  t.name         AS tag_name,
  d.mnemonic     AS plc_mnemonic,
  m.name         AS memory_name,
  mvc.value      AS current_value,
  mvc.updated_at AS current_updated_at
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
WHERE t.name = 'PECAROLLERCARGA41'
ORDER BY d.mnemonic;

-- 5) Buscar uma TAG por nome em um PLC especifico
-- Exemplo: TAG PECAPH29 no PLC1CARGA
SELECT
  t.name         AS tag_name,
  d.mnemonic     AS plc_mnemonic,
  m.name         AS memory_name,
  mvc.value      AS current_value,
  mvc.updated_at AS current_updated_at
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
WHERE t.name = 'PECAPH29'
  AND d.mnemonic = 'PLC1CARGA'
ORDER BY d.mnemonic;

-- 6) Historico da TAG por PLC (memory_value)
-- Exemplo: ultimos 200 registros
SELECT
  d.mnemonic     AS plc_mnemonic,
  t.name         AS tag_name,
  m.name         AS memory_name,
  mv.value       AS history_value,
  mv.status      AS history_status,
  mv.updated_at  AS history_updated_at
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
JOIN public.memory_value mv ON mv.memory_id = m.id
WHERE t.name = 'PECAPH29'
  AND d.mnemonic = 'PLC1CARGA'
ORDER BY mv.updated_at DESC
LIMIT 200;

-- 7) Ultima atualizacao por PLC
SELECT
  d.mnemonic,
  MAX(mvc.updated_at) AS last_update_at
FROM public.device d
JOIN public.memory m ON m.device_id = d.id
JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
GROUP BY d.mnemonic
ORDER BY d.mnemonic;

-- 8) Tags sem valor atual (cadastro existe, mas ainda sem leitura)
SELECT
  d.mnemonic AS plc_mnemonic,
  t.name     AS tag_name,
  m.name     AS memory_name
FROM public.tag t
JOIN public.device d ON d.id = t.device_id
JOIN public.memory m ON m.id = t.memory_id
LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id
WHERE mvc.memory_id IS NULL
ORDER BY d.mnemonic, t.name;

