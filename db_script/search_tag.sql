-- Query to search for tags in the database
-- This query retrieves all tags with related machine and memory information

SELECT
    t.id,
    t.name AS tag_name,
    t.persist_history,
    m.name AS machine_name,
    mf.name AS mini_fabrica_name,
    s.name AS setor_name,
    d.mnemonic AS device_mnemonic,
    d.name AS device_name,
    mem.name AS memory_name,
    mem.address AS memory_address
FROM tag t
JOIN machine m ON t.machine_id = m.id
JOIN mini_fabrica mf ON m.mini_fabrica_id = mf.id
JOIN setor s ON m.setor_id = s.id
JOIN device d ON m.device_id = d.id
JOIN memory mem ON t.memory_id = mem.id AND mem.device_id = m.device_id
ORDER BY t.name;

-- Query to search for a specific tag by name (case-insensitive)
-- Replace 'TAG_NAME_HERE' with the actual tag name you want to search

SELECT
    t.id,
    t.name AS tag_name,
    t.persist_history,
    m.name AS machine_name,
    mf.name AS mini_fabrica_name,
    s.name AS setor_name,
    d.mnemonic AS device_mnemonic,
    d.name AS device_name,
    mem.name AS memory_name,
    mem.address AS memory_address
FROM tag t
JOIN machine m ON t.machine_id = m.id
JOIN mini_fabrica mf ON m.mini_fabrica_id = mf.id
JOIN setor s ON m.setor_id = s.id
JOIN device d ON m.device_id = d.id
JOIN memory mem ON t.memory_id = mem.id
WHERE LOWER(t.name) = LOWER('TAG_NAME_HERE')
ORDER BY t.name;

 SELECT t.name AS tag_name, t.persist_history AS persist_history, 
                        m.address AS memory_address, m.name AS memory_name 
                        FROM public.device d
                        JOIN public.tag t ON t.device_id = d.id
                        JOIN public.memory m ON m.id = t.memory_id
                        WHERE d.mnemonic = ?
                        ORDER BY t.id,