-- Script para inserção das tags de qualidade para a máquina Escolha_41 (ID 13)
-- Device ID: 3
-- Endereço inicial: D4000

DO $$ 
DECLARE 
    v_device_id INT := 3;
    v_machine_id BIGINT := 13;
    v_start_addr INT := 4000;
    v_mem_id INT;
BEGIN 
    -- 1. Qualidade_Gatilho (D4000)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4000', v_start_addr, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Gatilho', v_machine_id, v_mem_id, true);

    -- 2. Total_Defeitos1 (D4001)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4001', v_start_addr + 1, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Total_Defeitos1', v_machine_id, v_mem_id, true);

    -- 3. Total_Defeitos2 (D4002)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4002', v_start_addr + 2, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Total_Defeitos2', v_machine_id, v_mem_id, true);

    -- 4. Total_Defeitos3 (D4003)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4003', v_start_addr + 3, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Total_Defeitos3', v_machine_id, v_mem_id, true);

    -- 5. Codigo_Defeito1 (D4004)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4004', v_start_addr + 4, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Codigo_Defeito1', v_machine_id, v_mem_id, true);

    -- 6. Codigo_Defeito2 (D4005)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4005', v_start_addr + 5, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Codigo_Defeito2', v_machine_id, v_mem_id, true);

    -- 7. Codigo_Defeito3 (D4006)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4006', v_start_addr + 6, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Codigo_Defeito3', v_machine_id, v_mem_id, true);

    -- 8. Qtde_Amostragem (D4007)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4007', v_start_addr + 7, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qtde_Amostragem', v_machine_id, v_mem_id, true);

    -- 9. Qualidade_Ano (D4008)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4008', v_start_addr + 8, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Ano', v_machine_id, v_mem_id, true);

    -- 10. Qualidade_Mes (D4009)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4009', v_start_addr + 9, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Mes', v_machine_id, v_mem_id, true);

    -- 11. Qualidade_Dia (D4010)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4010', v_start_addr + 10, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Dia', v_machine_id, v_mem_id, true);

    -- 12. Qualidade_Hora (D4011)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4011', v_start_addr + 11, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Hora', v_machine_id, v_mem_id, true);

    -- 13. Qualidade_Min (D4012)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4012', v_start_addr + 12, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Min', v_machine_id, v_mem_id, true);

    -- 14. Qualidade_Seg (D4013)
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4013', v_start_addr + 13, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Seg', v_machine_id, v_mem_id, true);

    -- 15. Qualidade_Maquina_Current (D4014) - Sem persistência de histórico
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4014', v_start_addr + 14, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Maquina_Current', v_machine_id, v_mem_id, false);

    -- 16. Qualidade_Maquina_Persisted (D4015) - Com persistência de histórico
    INSERT INTO public.memory (device_id, name, address, bit) VALUES (v_device_id, 'DM_4015', v_start_addr + 15, -1) RETURNING id INTO v_mem_id;
    INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES ('Qualidade_Maquina_Persisted', v_machine_id, v_mem_id, true);

END $$;
