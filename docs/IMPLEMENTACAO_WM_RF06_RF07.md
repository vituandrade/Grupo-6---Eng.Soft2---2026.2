# Implementação WM — RF06 e RF07

## RF06 — Fechamento de conta

- A tela de fechamento mantém o resumo da comanda, subtotal, desconto e total.
- O desconto continua permitido somente para o perfil já tratado pela aplicação (`Interno`).
- As formas presenciais previstas no requisito continuam sendo: Dinheiro, Cartão de Débito, Cartão de Crédito e Pix presencial.
- Antes de concluir o fechamento, a venda é persistida no SQLite.
- Depois da persistência, a comanda é marcada como fechada, com data/hora, funcionário, desconto e pagamento.
- Quando existe mesa vinculada, ela volta ao estado livre.

## RF07 — Histórico de vendas

- Foi criada a entidade `Model.Atendimento.Venda` para representar o registro histórico.
- O SQLite agora possui `vendas` e `venda_itens`.
- O histórico preserva data/hora, comanda, atendimento, forma de pagamento, subtotal, desconto, total e funcionário.
- A tela 08 permite consulta sem filtros e com filtros opcionais por data inicial, data final e forma de pagamento.
- Período inválido é bloqueado quando a data inicial é posterior à data final.
- Quando nenhuma venda atende aos filtros, a tela informa `Nenhuma venda encontrada.`
- Os itens da comanda fechada também são gravados em `venda_itens` para manter um retrato da venda e permitir reutilização posterior por relatórios.

## Testes e validação

- Foi adicionado teste de persistência do histórico, filtro por forma de pagamento e consulta por período.
- O núcleo Java das alterações foi compilado diretamente.
- A suíte Maven completa não foi executada neste ambiente porque as dependências Maven não estão disponíveis localmente e o ambiente não possui acesso ao repositório Maven.

## Correções posteriores

- O filtro de período do histórico usa o mesmo formato de data/hora empregado pelo SQLite.
- O saldo dos produtos vendáveis e o estoque por unidade são sincronizados pelo `DatabaseService`.
- A Tela 05 permite alterar a quantidade de um pedido já registrado, com validação de estoque e persistência.
