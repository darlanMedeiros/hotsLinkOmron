# README-INTEGRATION (Template)

Use este template para registrar a integracao de uma tela/componente no modulo `web`.

## 1. Identificacao

- Feature/tela:
- Responsavel:
- Data:
- Branch:
- Link Figma (frame/version):

## 2. Escopo

- Objetivo da integracao:
- O que entra:
- O que fica fora:

## 3. Arquivos impactados

- Componentes UI:
- Composicao/pagina:
- Estilos/tokens:
- Servicos/API:
- Documentacao:

## 4. Design Tokens

- Cores novas/alteradas:
- Tipografia:
- Espacamento/radius/sombra:
- Onde foi aplicado (arquivo):

## 5. Contrato com Backend

- Base path:
- Endpoints usados:

1. `METHOD /api/...`
2. `METHOD /api/...`

- Exemplo de payload/resposta:

```json
{
  "example": true
}
```

## 6. Estados da UI

- [ ] default
- [ ] loading
- [ ] empty
- [ ] error
- [ ] sucesso

## 7. Responsividade e Acessibilidade

- Breakpoints validados:
- Navegacao por teclado:
- Contraste minimo:
- Labels/aria quando aplicavel:

## 8. Flags de Integracao

- [ ] Usa `fetch('/api/...')` com caminho relativo
- [ ] Compat�vel com proxy do Vite (`VITE_API_URL`)
- [ ] Sem mocks no fluxo principal
- [ ] Erros HTTP tratados

## 9. Testes e Validacao

Comandos executados:

```bash
cd web
npm run build
npm run dev
```

Backend local (quando aplicavel):

```bash
mvn -pl api spring-boot:run
```

Resultado:

- Build frontend:
- Subida local frontend:
- Subida local backend:
- Chamada real `/api/...`:

## 10. Checklist de PR

- [ ] Layout aderente ao Figma
- [ ] Sem regressao visual evidente
- [ ] Integracao com API validada
- [ ] Documentacao atualizada
- [ ] Sem arquivos indevidos (`node_modules`, `dist`, etc.)

## 11. Evidencias

- Prints (before/after):
- GIF/video curto:
- Logs/erros relevantes:

## 12. Pendencias

1.
2.
3.

## 13. Decisoes tecnicas

- Decisao:
- Motivo:
- Impacto:
