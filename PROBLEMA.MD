<!-- spellcheck: off -->

collector priorizar a leitura de RR.


tag qualidade quando lida vai estar associado a tres tags de defeitos, uma qualidade vai possour 3 defeitos.

## Plano de Implementação: Coleta de Qualidade (Futuro)

### Convenção de Tags Obrigatória
Para que a coleta de qualidade funcione automaticamente, as tags no banco de dados devem seguir estes nomes por máquina:
- **Gatilho**: `Qualidade_Gatilho` (Monitorado para qualquer mudança de valor numérico).
- **Defeitos (IDs)**: `Total_Defeitos1`, `Total_Defeitos2`, `Total_Defeitos3`.
- **Defeitos (Valores/Qtd)**: `Codigo_Defeito1`, `Codigo_Defeito2`, `Codigo_Defeito3`.
- **Amostragem dos Defeitos**: `Qtde_Amostragem`.
- **Data/Hora do CLP**: `Qualidade_Ano`, `Qualidade_Mes`, `Qualidade_Dia`, `Qualidade_Hora`, `Qualidade_Min`, `Qualidade_Seg`.

- **Qualadede**: `Qualidade_Maquina_Current`, `Qualidade_Maquina_Persisted`
1. **Criar SQL**: Criar um SQL para criar as tags de qualidade no banco de dados. Para criar os sql vai usar os seguintes dados:
`device_id`: 3
`memory_address`: a partir de D4000
`machine`: Escolha_41 id 13
`tipo_qualidade_id`: 10


### Lógica de Funcionamento
1. **Detecção**: O Coletor monitora a tag `Qualidade_Gatilho`. 
2. **Leitura em Bloco**: Ao detectar qualquer mudança no valor do gatilho, o sistema realiza a leitura imediata das outras 13 tags (Total_Defeitos1, Total_Defeitos2, Total_Defeitos3, Codigo_Defeito1, Codigo_Defeito2, Codigo_Defeito3, Qtde_Amostragem, Qualidade_Ano, Qualidade_Mes, Qualidade_Dia, Qualidade_Hora, Qualidade_Min, Qualidade_Seg) associadas no mesmo Device.
3. **Qualidade_Maquina**: A qualidade da máquina é atualizada a cada 5 minutos. Vai salvar apenas a qualidade da máquina, não vai salvar os defeitos. Vai ter duas tags, uma current que não persites somente para mostrar supervisório e outra persistente, salva no banco para histórico.  
4. **Turno**: O sistema utiliza os valores de Ano/Mês/Dia/Hora/Min/Seg lidos do CLP para determinar em qual Turno (tabela `turno`) o registro se encaixa.
5. **Persistência**: Os dados são salvos nas tabelas `qualidade` e `qualidade_defeito_valor`.

**Implementar**: Implementar essa lógica de coleta de qualidade no coletor.






