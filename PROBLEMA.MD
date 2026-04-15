<!-- spellcheck: off -->

collector priorizar a leitura de RR.


tag qualidade quando lida vai estar associado a tres tags de defeitos, uma qualidade vai possour 3 defeitos.

## Plano de Implementação: Coleta de Qualidade (Futuro)

### Convenção de Tags Obrigatória
Para que a coleta de qualidade funcione automaticamente, as tags no banco de dados devem seguir estes nomes por máquina:
- **Gatilho**: `Qualidade_Gatilho` (Monitorado para qualquer mudança de valor numérico).
- **Defeitos (IDs)**: `Qualidade_D1_Num`, `Qualidade_D2_Num`, `Qualidade_D3_Num`.
- **Defeitos (Valores/Qtd)**: `Qualidade_D1_Val`, `Qualidade_D2_Val`, `Qualidade_D3_Val`.
- **Data/Hora do CLP**: `Qualidade_Ano`, `Qualidade_Mes`, `Qualidade_Dia`, `Qualidade_Hora`, `Qualidade_Min`, `Qualidade_Seg`.

### Lógica de Funcionamento
1. **Detecção**: O Coletor monitora a tag `Qualidade_Gatilho`. 
2. **Leitura em Bloco**: Ao detectar qualquer mudança no valor do gatilho, o sistema realiza a leitura imediata das outras 12 tags associadas no mesmo Device.
3. **Turno**: O sistema utiliza os valores de Ano/Mês/Dia/Hora/Min/Seg lidos do CLP para determinar em qual Turno (tabela `turno`) o registro se encaixa.
4. **Persistência**: Os dados são salvos nas tabelas `qualidade` e `qualidade_defeito_valor`.



