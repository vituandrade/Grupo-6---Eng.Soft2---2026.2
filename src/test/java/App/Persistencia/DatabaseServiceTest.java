package App.Persistencia;

import Model.Produtos.ItemCardapio;
import Model.Produtos.Produto;
import Model.Sistema.Config;
import Model.Usuarios.Garcom;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
        assertTrue(banco.carregarConfig().getNumeroDeMesas() > 0);
    }

    private DatabaseService criarBanco() {
        String caminho = pastaTemporaria.resolve("teste.db").toString();
        return new DatabaseService(caminho, false);
    }
}
