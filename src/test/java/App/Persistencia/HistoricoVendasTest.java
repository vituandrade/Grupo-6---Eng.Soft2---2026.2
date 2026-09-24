package App.Persistencia;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Atendimento.Pedido;
import Model.Atendimento.Venda;
import Model.Pagamento.PagamentoDinheiro;
import Model.Produtos.ItemCardapio;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoVendasTest {

    @TempDir
    Path diretorio;

    @Test
    void registrarVendaPersisteCabecalhoEItens() {
        DatabaseService db = new DatabaseService(
                diretorio.resolve("teste.db").toString(),
                false
        );
        Usuario funcionario = new Interno("wendell", "123");
        Comanda comanda = new Comanda();
        ItemCardapio produto = new ItemCardapio("Hambúrguer", "Lanche", "", 25.0, true);
        comanda.adicionarPedido(new Pedido(produto, 2, "", funcionario));
        PagamentoDinheiro pagamento = new PagamentoDinheiro(50.0, 50.0);
        assertTrue(pagamento.processar());

        Venda venda = db.registrarVenda(comanda, new Mesa(7), funcionario, pagamento, 5.0);
        assertEquals(50.0, venda.getSubtotal());
        assertEquals(5.0, venda.getDesconto());
        assertEquals(45.0, venda.getTotal());
        assertEquals("Mesa 07", venda.getAtendimento());

        List<Venda> historico = db.carregarHistoricoVendas(null, null, "Todas");
        assertEquals(1, historico.size());
        assertEquals(venda.getId(), historico.get(0).getId());
    }

    @Test
    void filtraPorFormaDePagamento() {
        DatabaseService db = new DatabaseService(
                diretorio.resolve("teste.db").toString(),
                false
        );
        Usuario funcionario = new Interno("wendell", "123");
        ItemCardapio produto = new ItemCardapio("Prato", "Prato", "", 30.0, true);

        Comanda dinheiro = new Comanda();
        dinheiro.adicionarPedido(new Pedido(produto, 1, "", funcionario));
        PagamentoDinheiro pgDinheiro = new PagamentoDinheiro(30.0, 30.0);
        assertTrue(pgDinheiro.processar());
        db.registrarVenda(dinheiro, null, funcionario, pgDinheiro, 0.0);

        Comanda cartao = new Comanda();
        cartao.adicionarPedido(new Pedido(produto, 1, "", funcionario));
        Model.Pagamento.PagamentoCartaoDebito pgCartao =
                new Model.Pagamento.PagamentoCartaoDebito(30.0, "Não informado");
        assertTrue(pgCartao.processar());
        db.registrarVenda(cartao, null, funcionario, pgCartao, 0.0);

        assertEquals(2, db.carregarHistoricoVendas(null, null, "Todas").size());
        assertEquals(1, db.carregarHistoricoVendas(null, null, "Dinheiro").size());
        assertEquals(1, db.carregarHistoricoVendas(null, null, "Cartão de Débito").size());
    }

    @Test
    void filtroPorDataIncluiVendasDoDiaInicialEFinal() {
        DatabaseService db = new DatabaseService(
                diretorio.resolve("teste.db").toString(),
                false
        );
        Usuario funcionario = new Interno("wendell", "123");
        ItemCardapio produto = new ItemCardapio("Prato", "Prato", "", 30.0, true);
        Comanda comanda = new Comanda();
        comanda.adicionarPedido(new Pedido(produto, 1, "", funcionario));
        PagamentoDinheiro pagamento = new PagamentoDinheiro(30.0, 30.0);
        assertTrue(pagamento.processar());
        Venda venda = db.registrarVenda(comanda, null, funcionario, pagamento, 0.0);

        LocalDate hoje = venda.getDataHora().toLocalDate();
        List<Venda> historico = db.carregarHistoricoVendas(
                hoje,
                hoje,
                "Todas"
        );

        assertEquals(1, historico.size());
        assertEquals(venda.getId(), historico.get(0).getId());
    }
}
