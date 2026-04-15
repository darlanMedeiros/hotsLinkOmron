-- Script para limpar duplicatas e aplicar restrição de unicidade na tabela defeito

-- 1. Remove números duplicados (define como NULL os que estiverem repetidos, mantendo apenas o primeiro registro)
UPDATE public.defeito 
SET number = NULL 
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.defeito
    WHERE number IS NOT NULL
    GROUP BY number
) AND number IS NOT NULL;

-- 2. Garante que a coluna 'number' existe
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='defeito' AND column_name='number') THEN
        ALTER TABLE defeito ADD COLUMN number INTEGER;
    END IF;
END $$;

-- 3. Remove a constraint anterior se existir e adiciona a nova restrição de UNIQUE
ALTER TABLE public.defeito DROP CONSTRAINT IF EXISTS uq_defeito_number;
ALTER TABLE public.defeito ADD CONSTRAINT uq_defeito_number UNIQUE (number);
