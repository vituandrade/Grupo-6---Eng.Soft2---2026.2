# Revisão e correções — Fase 1

## Escopo

Esta fase corrige problemas encontrados na revisão do esqueleto atual antes da implementação dos casos de uso ainda ausentes.

## Correções aplicadas

- `Pedido.setQuantidade(int)` passou a representar a quantidade final, em vez de somar novamente o valor recebido.
- A quantidade continua protegida contra valores menores ou iguais a zero.
- `Pedido` agora preserva o funcionário que registrou o item, preparando o domínio para histórico de vendas.
- `Comanda` agora registra data de abertura, data de fechamento e funcionário responsável pelo fechamento.
- O fechamento do domínio não permite uma comanda sem itens.
- O envio do carrinho valida o estoque agregado por produto antes de alterar qualquer saldo.
- O envio do carrinho persiste a alteração do estoque e desfaz a alteração em memória quando a persistência falha.
- O cancelamento de item também persiste a devolução do estoque e desfaz a alteração em memória se a persistência falhar.
- O fluxo de comanda passou a reutilizar a lista central de produtos da sessão, reduzindo divergência entre objetos carregados em pontos diferentes da aplicação.
- Foram adicionados testes de regressão para quantidade, metadados do fechamento e bloqueio de fechamento vazio.
- O `mvnw` foi normalizado para LF no ambiente de desenvolvimento Linux, pois o arquivo original estava com CRLF.

## Validação executada

A compilação direta dos modelos Java e um teste de fumaça do domínio passaram. A suíte Maven/JUnit não foi executada neste ambiente porque as dependências Maven não estão disponíveis localmente.

## Próxima etapa

Depois desta base, a implementação pode avançar para persistência de mesas/comandas/pedidos/pagamentos/vendas e, em seguida, para Histórico de vendas e Relatórios, mantendo as telas da referência visual.
