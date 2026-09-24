package App.Persistencia;

import Model.Atendimento.EstadoMesa;
import Model.Atendimento.Mesa;
import Model.Estoque.ItemEstoque;
import Model.Estoque.TipoMovimentacao;
import Model.Produtos.ItemCardapio;
import Model.Produtos.Alimentos.Refeicao;
import Model.Produtos.Produto;
import Model.Sistema.Config;
import Model.Usuarios.Garcom;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseServiceTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    void persisteConfiguracaoEPerfisDeUsuario() {
        DatabaseService banco = criarBanco();
        Config config = new Config();
        config.setNomeRestaurante("Mesa Certa");
        config.setNumeroDeMesas(12);

        banco.salvarConfig(config);
        banco.salvarUsuarios(List.of(
                new Interno("admin", "admin"),
                new Garcom("garcom", "123")
        ));

        assertEquals("Mesa Certa", banco.carregarConfig().getNomeRestaurante());
        assertEquals(12, banco.carregarConfig().getNumeroDeMesas());

        List<Usuario> usuarios = banco.carregarUsuarios();
        assertEquals(2, usuarios.size());
        assertInstanceOf(Interno.class, usuarios.get(0));
        assertInstanceOf(Garcom.class, usuarios.get(1));
    }

    @Test
    void persisteDisponibilidadeDoItemDoCardapio() {
        DatabaseService banco = criarBanco();
        ItemCardapio disponivel = new ItemCardapio(
                "X-Burger", "Lanche", "Hambúrguer", 24.90, true
        );
        ItemCardapio indisponivel = new ItemCardapio(
                "Pudim", "Sobremesa", "", 9.90, false
        );

        banco.salvarProdutos(List.of(disponivel, indisponivel));
        List<Produto> produtos = banco.carregarProdutos();

        assertEquals(2, produtos.size());
        assertTrue(produtos.stream().anyMatch(p -> p.getNome().equals("X-Burger") && p.isDisponivel()));
        assertTrue(produtos.stream().anyMatch(p -> p.getNome().equals("Pudim") && !p.isDisponivel()));
        assertFalse(produtos.isEmpty());
    }

    @Test
    void migraCargaInicialDosArquivosJson() {
        String caminho = pastaTemporaria.resolve("migracao.db").toString();
        DatabaseService banco = new DatabaseService(caminho, true);

        assertFalse(banco.carregarUsuarios().isEmpty());
        assertFalse(banco.carregarProdutos().isEmpty());
        assertFalse(banco.carregarItensEstoque().isEmpty());
        assertTrue(banco.carregarConfig().getNumeroDeMesas() > 0);
    }

    @Test
    void persisteItemESeuSaldoAposReabrirBanco() {
        String caminho = pastaTemporaria.resolve("estoque-persistente.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);

        banco.registrarMovimentacaoEstoque(null, "Farinha", "kg", TipoMovimentacao.ENTRADA, 12.5);

        DatabaseService bancoReaberto = new DatabaseService(caminho, false);
        ItemEstoque item = bancoReaberto.carregarItensEstoque().get(0);
        assertEquals("Farinha", item.getNome());
        assertEquals("kg", item.getUnidadeMedida());
        assertEquals(12.5, item.getQuantidade());
    }

    @Test
    void itemDeCardapioRecuperaSaldoSincronizado() {
        DatabaseService banco = criarBanco();
        ItemCardapio produto = new ItemCardapio(
                "Coca-Cola", "Bebida", "", 8.0, true
        );
        produto.setEstoque(6);

        banco.salvarProdutos(List.of(produto));

        assertEquals(6, banco.carregarProdutos().get(0).getEstoque());
        assertEquals(6, banco.carregarItensEstoque().get(0).getQuantidade());
    }

    @Test
    void estoqueDoProdutoEEstoqueDoModuloPermanecemSincronizados() {
        DatabaseService banco = criarBanco();
        Refeicao produto = new Refeicao(
                "Picanha", "", 80.0, 10, "Prato"
        );

        banco.salvarProdutos(List.of(produto));
        assertEquals(10, banco.carregarItensEstoque().get(0).getQuantidade());
        assertEquals(10, banco.carregarProdutos().get(0).getEstoque());

        banco.registrarMovimentacaoEstoque(
                banco.carregarItensEstoque().get(0).getId(),
                "Picanha",
                "un.",
                TipoMovimentacao.SAIDA,
                3
        );

        assertEquals(7, banco.carregarItensEstoque().get(0).getQuantidade());
        assertEquals(7, banco.carregarProdutos().get(0).getEstoque());
    }

    @Test
    void entradaSomaESaidaSubtraiSaldo() {
        DatabaseService banco = criarBanco();
        ItemEstoque criado = banco.registrarMovimentacaoEstoque(
                null, "Refrigerante", "un.", TipoMovimentacao.ENTRADA, 10
        );

        banco.registrarMovimentacaoEstoque(
                criado.getId(), criado.getNome(), criado.getUnidadeMedida(), TipoMovimentacao.ENTRADA, 4
        );
        banco.registrarMovimentacaoEstoque(
                criado.getId(), criado.getNome(), criado.getUnidadeMedida(), TipoMovimentacao.SAIDA, 3
        );

        assertEquals(11, banco.carregarItensEstoque().get(0).getQuantidade());
    }

    @Test
    void saidaMaiorQueSaldoNaoAlteraSaldoNemRegistraMovimento() throws Exception {
        String caminho = pastaTemporaria.resolve("saldo-insuficiente.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);
        ItemEstoque criado = banco.registrarMovimentacaoEstoque(
                null, "Carne", "kg", TipoMovimentacao.ENTRADA, 5
        );

        SaldoInsuficienteException erro = assertThrows(
                SaldoInsuficienteException.class,
                () -> banco.registrarMovimentacaoEstoque(
                criado.getId(), criado.getNome(), criado.getUnidadeMedida(), TipoMovimentacao.SAIDA, 6
                )
        );

        assertEquals("Saldo insuficiente.", erro.getMessage());
        assertEquals(5, banco.carregarItensEstoque().get(0).getQuantidade());
        assertEquals(1, contarMovimentacoes(caminho));
    }

    private long contarMovimentacoes(String caminho) throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho);
             Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery("SELECT COUNT(*) FROM movimentacoes_estoque")) {
            return resultado.getLong(1);
        }
    }

    private DatabaseService criarBanco() {
        String caminho = pastaTemporaria.resolve("teste.db").toString();
        return new DatabaseService(caminho, false);
    }

    // ── UC03 – Mesas ─────────────────────────────────────────────────────────

    @Test
    void persisteMesaERecuperaAposReiniciar() {
        String caminho = pastaTemporaria.resolve("mesas-persistentes.db").toString();
        DatabaseService banco = new DatabaseService(caminho, false);

        // Cria três mesas com números não sequenciais
        banco.salvarMesa(new Mesa(3));
        banco.salvarMesa(new Mesa(7, EstadoMesa.OCUPADA));
        banco.salvarMesa(new Mesa(15, EstadoMesa.AGUARDANDO_FECHAMENTO));

        // Reabre o banco (simula reinicialização da aplicação)
        DatabaseService bancoReaberto = new DatabaseService(caminho, false);
        List<Mesa> mesas = bancoReaberto.carregarMesas();

        assertEquals(3, mesas.size(), "Devem existir 3 mesas após reiniciar");
        assertEquals(List.of(3, 7, 15),
                mesas.stream().map(Mesa::getNumMesa).toList(),
                "Números devem ser os mesmos e em ordem crescente");
        assertEquals(EstadoMesa.OCUPADA, mesas.get(1).getEstado());
        assertEquals(EstadoMesa.AGUARDANDO_FECHAMENTO, mesas.get(2).getEstado());
    }

    @Test
    void adicionaEstadoAoBancoCriadoPelaVersaoAnterior() throws Exception {
        String caminho = pastaTemporaria.resolve("mesas-legadas.db").toString();
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho);
             Statement statement = conexao.createStatement()) {
            statement.execute("CREATE TABLE mesas (numero INTEGER PRIMARY KEY CHECK (numero > 0))");
            statement.execute("INSERT INTO mesas (numero) VALUES (4)");
        }

        DatabaseService bancoAtualizado = new DatabaseService(caminho, false);
        Mesa mesaMigrada = bancoAtualizado.carregarMesas().get(0);

        assertEquals(4, mesaMigrada.getNumMesa());
        assertEquals(EstadoMesa.LIVRE, mesaMigrada.getEstado());
    }

    @Test
    void salvarMesaDuplicadaNaoLancaExcecao() {
        DatabaseService banco = criarBanco();
        banco.salvarMesa(new Mesa(5));

        // Salvar novamente atualiza o estado sem duplicar a mesa.
        assertDoesNotThrow(() -> banco.salvarMesa(new Mesa(5, EstadoMesa.OCUPADA)));
        assertEquals(1, banco.carregarMesas().size(), "Não deve criar duplicata");
        assertEquals(EstadoMesa.OCUPADA, banco.carregarMesas().get(0).getEstado());
    }

    @Test
    void removerMesaExclui() {
        DatabaseService banco = criarBanco();
        banco.salvarMesa(new Mesa(1));
        banco.salvarMesa(new Mesa(2));

        banco.removerMesa(1);

        List<Mesa> mesas = banco.carregarMesas();
        assertEquals(1, mesas.size());
        assertEquals(2, mesas.get(0).getNumMesa());
    }
}
