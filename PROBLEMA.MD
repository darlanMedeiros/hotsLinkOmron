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



**Criar o Sql**: Criar o sql para salvar os defeitos na tabela defeitos com os seguintes dados:
number - name
32 - Arranhados
33 - Arranhado de Linha(caco)
34 - Bolha de Ar (caco)
36 - Contaminação
38 - Depressão
39 - Depressão de Borda
5 - Enfolhamentos
6 - Estampo sujo / Marcado
11 - Falha Decorado / Desenho
41 - Falha Rebarbador
12 - Facheado
13 - Fervido
14 - Furo de Esmalte
28 - Grumo
42 - Martelado
43 - Massinha
44 - Pingo d'água
45 - Pingo Verde
18 - Ponta quebrada
20 - Re-presagem
49 - Retração
50 - Sujeira / Resíduo forno
51 - Sujeira Amarela
52 - Sujeira no Rebarbador
53 - Sujeira Preta
54 - Trinca de Extração
55 - Trinca de Resfriamento
56 - Trinca de Secagem
57 - Trinca Mecânica
58 - Trinca Aresta
31 - Lascados
46 - Quebra na aresta
59 - Fora de Esquadro
60 - Luneta
61 - Tamanho GG
62 - Tamanho PP
63 - Torto



