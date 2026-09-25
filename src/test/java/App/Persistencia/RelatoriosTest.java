package App.Persistencia;

import org.junit.jupiter.api.Test;
import Model.Estoque.TipoMovimentacao;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatoriosTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    void geraResumoDeVendasNoPeriodo() throws Exception {
        String caminho = pastaTemporaria.resolve("relatorios-vendas.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);
        inserirVenda(caminho, 1, "2026-09-10 12:00:00.000", "Mesa 01", "Dinheiro", 100.0, 90.0);
        inserirVenda(caminho, 2, "2026-09-11 13:00:00.000", "Mesa 02", "Pix presencial", 50.0, 50.0);

        InterfacePersistencia.ResumoVendas resumo =
                banco.gerarResumoVendas(
                        java.time.LocalDate.of(2026, 9, 10),
                        java.time.LocalDate.of(2026, 9, 10)
                );

        assertEquals(90.0, resumo.totalVendido());
        assertEquals(1, resumo.comandasFechadas());
        assertEquals(90.0, resumo.ticketMedio());
    }

    @Test
    void geraItensMaisVendidosOrdenadosPorQuantidade() throws Exception {
        String caminho = pastaTemporaria.resolve("relatorios-itens.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);
        inserirVendaComItens(caminho, 1, "2026-09-10 12:00:00.000", List.of(
                new ItemBanco("Pizza", 3, 30.0),
                new ItemBanco("Refrigerante", 1, 8.0)
        ));
        inserirVendaComItens(caminho, 2, "2026-09-11 13:00:00.000", List.of(
                new ItemBanco("Pizza", 2, 20.0)
        ));

        List<InterfacePersistencia.ItemRelatorio> itens = banco.carregarItensMaisVendidos();

        assertEquals(2, itens.size());
        assertEquals("Pizza", itens.get(0).produto());
        assertEquals(5.0, itens.get(0).quantidade());
        assertEquals(50.0, itens.get(0).valorTotal());
    }

    @Test
    void geraConsumoPorPeriodoSemIncluirVendasForaDoPeriodo() throws Exception {
        String caminho = pastaTemporaria.resolve("relatorios-consumo.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);
        inserirVendaComItens(caminho, 1, "2026-09-10 12:00:00.000", List.of(
                new ItemBanco("Hambúrguer", 2, 40.0)
        ));
        inserirVendaComItens(caminho, 2, "2026-09-12 12:00:00.000", List.of(
                new ItemBanco("Hambúrguer", 7, 140.0)
        ));

        List<InterfacePersistencia.ItemRelatorio> itens =
                banco.carregarConsumoPorPeriodo(
                        java.time.LocalDate.of(2026, 9, 10),
                        java.time.LocalDate.of(2026, 9, 10)
                );

        assertEquals(1, itens.size());
        assertEquals(2.0, itens.get(0).quantidade());
        assertEquals(40.0, itens.get(0).valorTotal());
    }

    @Test
    void geraRelatorioDeEstoqueComSituacao() {
        String caminho = pastaTemporaria.resolve("relatorios-estoque.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);
        banco.registrarMovimentacaoEstoque(null, "Arroz", "kg", TipoMovimentacao.ENTRADA, 10);
        banco.registrarMovimentacaoEstoque(null, "Molho", "un.", TipoMovimentacao.ENTRADA, 3);
        banco.registrarMovimentacaoEstoque(null, "Sal", "kg", TipoMovimentacao.ENTRADA, 0.5);

        List<InterfacePersistencia.EstoqueRelatorio> itens = banco.carregarRelatorioEstoque();

        assertEquals(3, itens.size());

        InterfacePersistencia.EstoqueRelatorio sal = itens.stream()
                .filter(item -> "Sal".equals(item.produto()))
                .findFirst()
                .orElseThrow();

        assertEquals(0.5, sal.quantidade());
        assertEquals("Estoque baixo", sal.situacao());
    }

    private void inserirVenda(
            String caminho,
            int comandaId,
            String dataHora,
            String atendimento,
            String pagamento,
            double subtotal,
            double total
    ) throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho);
             PreparedStatement statement = conexao.prepareStatement("""
                     INSERT INTO vendas (
                         comanda_id, data_abertura, data_hora, cliente_nome, atendimento,
                         forma_pagamento, subtotal, desconto, total, funcionario
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setInt(1, comandaId);
            statement.setString(2, dataHora);
            statement.setString(3, dataHora);
            statement.setString(4, "Cliente");
            statement.setString(5, atendimento);
            statement.setString(6, pagamento);
            statement.setDouble(7, subtotal);
            statement.setDouble(8, subtotal - total);
            statement.setDouble(9, total);
            statement.setString(10, "admin");
            statement.executeUpdate();
        }
    }

    private void inserirVendaComItens(
            String caminho,
            int comandaId,
            String dataHora,
            List<ItemBanco> itens
    ) throws Exception {
        int vendaId;

        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho);
             PreparedStatement venda = conexao.prepareStatement("""
                     INSERT INTO vendas (
                         comanda_id, data_abertura, data_hora, cliente_nome, atendimento,
                         forma_pagamento, subtotal, desconto, total, funcionario
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            venda.setInt(1, comandaId);
            venda.setString(2, dataHora);
            venda.setString(3, dataHora);
            venda.setString(4, "Cliente");
            venda.setString(5, "Mesa 01");
            venda.setString(6, "Dinheiro");
            double total = itens.stream().mapToDouble(ItemBanco::subtotal).sum();
            venda.setDouble(7, total);
            venda.setDouble(8, 0.0);
            venda.setDouble(9, total);
            venda.setString(10, "admin");
            venda.executeUpdate();
            try (var keys = venda.getGeneratedKeys()) {
                keys.next();
                vendaId = keys.getInt(1);
            }

            try (PreparedStatement item = conexao.prepareStatement("""
                    INSERT INTO venda_itens (
                        venda_id, produto_nome, quantidade, preco_unitario, subtotal
                    ) VALUES (?, ?, ?, ?, ?)
                    """)) {
                for (ItemBanco registro : itens) {
                    item.setInt(1, vendaId);
                    item.setString(2, registro.produto());
                    item.setInt(3, registro.quantidade());
                    item.setDouble(4, registro.precoUnitario());
                    item.setDouble(5, registro.subtotal());
                    item.executeUpdate();
                }
            }
        }
    }

    private record ItemBanco(String produto, int quantidade, double subtotal) {

        double precoUnitario() {
            return subtotal / quantidade;
        }
    }
}
