package App.Validacao;

import Model.Atendimento.Comanda;
import Model.Atendimento.Pedido;
import Model.Produtos.ItemCardapio;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;
import Model.Pagamento.PagamentoDinheiro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoComandaTest {

    @Test
    void setQuantidadeDeveDefinirQuantidadeFinal() {
        ItemCardapio produto = new ItemCardapio(
                "Hambúrguer", "Lanche", "", 20.0, true
        );
        Usuario usuario = new Interno("teste", "123");
        Pedido pedido = new Pedido(produto, 2, "", usuario);

        pedido.setQuantidade(5);

        assertEquals(5, pedido.getQuantidade());
    }

    @Test
    void setQuantidadeNaoAceitaValorNaoPositivo() {
        ItemCardapio produto = new ItemCardapio(
                "Hambúrguer", "Lanche", "", 20.0, true
        );
        Pedido pedido = new Pedido(produto, 2, "", null);

        assertThrows(IllegalArgumentException.class, () -> pedido.setQuantidade(0));
        assertThrows(IllegalArgumentException.class, () -> pedido.setQuantidade(-1));
    }

    @Test
    void fechamentoRegistraFuncionarioEData() {
        Comanda comanda = new Comanda();
        ItemCardapio produto = new ItemCardapio(
                "Hambúrguer", "Lanche", "", 20.0, true
        );
        Usuario usuario = new Interno("teste", "123");
        comanda.adicionarPedido(new Pedido(produto, 1, "", usuario));

        PagamentoDinheiro pagamento = new PagamentoDinheiro(20.0, 20.0);
        assertTrue(pagamento.processar());

        comanda.registrarFechamento(pagamento, 0.0, usuario);

        assertTrue(comanda.isFechada());
        assertSame(usuario, comanda.getFuncionarioFechamento());
        assertNotNull(comanda.getDataAbertura());
        assertNotNull(comanda.getDataFechamento());
        assertFalse(comanda.getDataFechamento().before(comanda.getDataAbertura()));
    }

    @Test
    void naoPermiteFecharComandaSemItens() {
        Comanda comanda = new Comanda();
        PagamentoDinheiro pagamento = new PagamentoDinheiro(0.0, 0.0);
        assertTrue(pagamento.processar());

        assertThrows(
                IllegalStateException.class,
                () -> comanda.registrarFechamento(
                        pagamento,
                        0.0,
                        new Interno("teste", "123")
                )
        );
    }
}
