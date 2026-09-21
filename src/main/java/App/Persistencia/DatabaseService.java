package App.Persistencia;

import Model.Estoque.ItemEstoque;
import Model.Estoque.TipoMovimentacao;
import Model.Produtos.ItemCardapio;
import Model.Produtos.Produto;
import Model.Produtos.Alimentos.Refeicao;
import Model.Produtos.Alimentos.TiraGosto;
import Model.Produtos.Bedidas.ComAlcool;
import Model.Produtos.Bedidas.SemAlcool;
import Model.Produtos.Outros.Descartaveis;
import Model.Produtos.Outros.Servico;
import Model.Sistema.Config;
import Model.Usuarios.Garcom;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistência SQLite usada pela aplicação.
 *
 * Na primeira execução, os dados dos arquivos JSON existentes são copiados
 * para o banco. Os JSONs continuam no projeto somente como carga inicial.
 */
public class DatabaseService implements InterfacePersistencia {

    private static final String CAMINHO_PADRAO = "Dados/restaurante.db";

    private final String caminhoBanco;

    public DatabaseService() {
        this(CAMINHO_PADRAO, true);
    }

    public DatabaseService(String caminhoBanco, boolean migrarJson) {
        this.caminhoBanco = caminhoBanco;
        garantirDiretorio();
        inicializarBanco();
        if (migrarJson) {
            migrarJsonSeNecessario();
        }
    }

    private Connection conectar() throws SQLException {
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + caminhoBanco);
        try (Statement statement = conexao.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return conexao;
    }

    private void garantirDiretorio() {
        File pasta = new File(caminhoBanco).getParentFile();
        if (pasta != null && !pasta.exists() && !pasta.mkdirs()) {
            throw new PersistenciaException("Não foi possível criar a pasta do banco de dados.", null);
        }
    }

    private void inicializarBanco() {
        String criarConfig = """
                CREATE TABLE IF NOT EXISTS config (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    nome_restaurante TEXT NOT NULL,
                    info_restaurante TEXT NOT NULL,
                    numero_de_mesas INTEGER NOT NULL,
                    taxa_de_servico REAL NOT NULL
                )
                """;
        String criarUsuarios = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT UNIQUE NOT NULL,
                    senha TEXT NOT NULL,
                    acesso_config INTEGER NOT NULL DEFAULT 0
                )
                """;
        String criarProdutos = """
                CREATE TABLE IF NOT EXISTS produtos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    descricao TEXT,
                    preco REAL NOT NULL CHECK (preco > 0),
                    estoque INTEGER NOT NULL DEFAULT 0,
                    categoria_nome TEXT NOT NULL,
                    tipo_classe TEXT NOT NULL,
                    disponivel INTEGER NOT NULL DEFAULT 1
                )
                """;
        String criarItensEstoque = """
                CREATE TABLE IF NOT EXISTS itens_estoque (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT UNIQUE COLLATE NOCASE NOT NULL,
                    unidade_medida TEXT NOT NULL,
                    quantidade REAL NOT NULL DEFAULT 0 CHECK (quantidade >= 0)
                )
                """;
        String criarMovimentacoes = """
                CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id INTEGER NOT NULL,
                    tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
                    quantidade REAL NOT NULL CHECK (quantidade > 0),
                    data_hora TEXT NOT NULL,
                    FOREIGN KEY (item_id) REFERENCES itens_estoque(id)
                )
                """;

        try (Connection conexao = conectar(); Statement statement = conexao.createStatement()) {
            statement.execute(criarConfig);
            statement.execute(criarUsuarios);
            statement.execute(criarProdutos);
            statement.execute(criarItensEstoque);
            statement.execute(criarMovimentacoes);
            garantirColunaDisponibilidade(statement);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível inicializar o banco de dados.", e);
        }
    }

    private void garantirColunaDisponibilidade(Statement statement) throws SQLException {
        try {
            statement.execute("ALTER TABLE produtos ADD COLUMN disponivel INTEGER NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                throw e;
            }
        }
    }

    private void migrarJsonSeNecessario() {
        PersistenceService json = new PersistenceService();
        try (Connection conexao = conectar()) {
            if (tabelaVazia(conexao, "config")) {
                salvarConfig(json.carregarConfig());
            }
            if (tabelaVazia(conexao, "usuarios")) {
                salvarUsuarios(json.carregarUsuarios());
            }
            if (tabelaVazia(conexao, "produtos")) {
                salvarProdutos(json.carregarProdutos());
            }
            migrarEstoqueDosProdutosSeNecessario(conexao);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível migrar os dados antigos.", e);
        }
    }

    private void migrarEstoqueDosProdutosSeNecessario(Connection conexao) throws SQLException {
        if (!tabelaVazia(conexao, "itens_estoque")) {
            return;
        }

        String migrarItens = """
                INSERT OR IGNORE INTO itens_estoque (nome, unidade_medida, quantidade)
                SELECT nome, 'un.', estoque
                FROM produtos
                WHERE tipo_classe <> 'servico'
                """;
        String registrarSaldosIniciais = """
                INSERT INTO movimentacoes_estoque (item_id, tipo, quantidade, data_hora)
                SELECT id, 'ENTRADA', quantidade, ?
                FROM itens_estoque
                WHERE quantidade > 0
                """;

        try (Statement statement = conexao.createStatement();
             PreparedStatement movimentacoes = conexao.prepareStatement(registrarSaldosIniciais)) {
            statement.executeUpdate(migrarItens);
            movimentacoes.setString(1, LocalDateTime.now().toString());
            movimentacoes.executeUpdate();
        }
    }

    private boolean tabelaVazia(Connection conexao, String tabela) throws SQLException {
        try (Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return resultado.next() && resultado.getInt(1) == 0;
        }
    }

    @Override
    public Config carregarConfig() {
        String sql = """
                SELECT nome_restaurante, info_restaurante, numero_de_mesas, taxa_de_servico
                FROM config WHERE id = 1
                """;
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            if (resultado.next()) {
                Config config = new Config();
                config.setNomeRestaurante(resultado.getString("nome_restaurante"));
                config.setInfoRestaurante(resultado.getString("info_restaurante"));
                config.setNumeroDeMesas(resultado.getInt("numero_de_mesas"));
                config.setTaxaDeServico(resultado.getDouble("taxa_de_servico"));
                return config;
            }
            return new Config();
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível carregar as configurações.", e);
        }
    }

    @Override
    public void salvarConfig(Config config) {
        String sql = """
                INSERT INTO config
                    (id, nome_restaurante, info_restaurante, numero_de_mesas, taxa_de_servico)
                VALUES (1, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    nome_restaurante = excluded.nome_restaurante,
                    info_restaurante = excluded.info_restaurante,
                    numero_de_mesas = excluded.numero_de_mesas,
                    taxa_de_servico = excluded.taxa_de_servico
                """;
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, config.getNomeRestaurante());
            statement.setString(2, config.getInfoRestaurante());
            statement.setInt(3, config.getNumeroDeMesas());
            statement.setDouble(4, config.getTaxaDeServico());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar as configurações.", e);
        }
    }

    @Override
    public List<Usuario> carregarUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT nome, senha, acesso_config FROM usuarios ORDER BY nome";
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                String nome = resultado.getString("nome");
                String senha = resultado.getString("senha");
                boolean autorizado = resultado.getInt("acesso_config") == 1;
                usuarios.add(autorizado
                        ? new Interno(nome, senha)
                        : new Garcom(nome, senha));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível carregar os usuários.", e);
        }
    }

    @Override
    public void salvarUsuarios(List<Usuario> usuarios) {
        String sql = "INSERT INTO usuarios (nome, senha, acesso_config) VALUES (?, ?, ?)";
        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);
            try (Statement limpar = conexao.createStatement();
                 PreparedStatement inserir = conexao.prepareStatement(sql)) {
                limpar.executeUpdate("DELETE FROM usuarios");
                for (Usuario usuario : usuarios) {
                    inserir.setString(1, usuario.getNome());
                    inserir.setString(2, usuario.getSenha());
                    inserir.setInt(3, usuario.AcessoEstoque() ? 1 : 0);
                    inserir.addBatch();
                }
                inserir.executeBatch();
                conexao.commit();
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar os usuários.", e);
        }
    }

    @Override
    public List<Produto> carregarProdutos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = """
                SELECT nome, descricao, preco, estoque, categoria_nome, tipo_classe, disponivel
                FROM produtos ORDER BY categoria_nome, nome
                """;
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                Produto produto = criarProduto(
                        resultado.getString("tipo_classe"),
                        resultado.getString("nome"),
                        resultado.getString("descricao"),
                        resultado.getDouble("preco"),
                        resultado.getInt("estoque"),
                        resultado.getString("categoria_nome"),
                        resultado.getInt("disponivel") == 1
                );
                if (produto != null) {
                    produtos.add(produto);
                }
            }
            return produtos;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível carregar os itens do cardápio.", e);
        }
    }

    @Override
    public void salvarProdutos(List<Produto> produtos) {
        String sql = """
                INSERT INTO produtos
                    (nome, descricao, preco, estoque, categoria_nome, tipo_classe, disponivel)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);
            try (Statement limpar = conexao.createStatement();
                 PreparedStatement inserir = conexao.prepareStatement(sql)) {
                limpar.executeUpdate("DELETE FROM produtos");
                for (Produto produto : produtos) {
                    inserir.setString(1, produto.getNome());
                    inserir.setString(2, produto.getDescricao());
                    inserir.setDouble(3, produto.getPreco());
                    inserir.setInt(4, produto.getEstoque());
                    inserir.setString(5, produto.getCategoriaNome());
                    inserir.setString(6, identificarTipo(produto));
                    inserir.setInt(7, produto.isDisponivel() ? 1 : 0);
                    inserir.addBatch();
                }
                inserir.executeBatch();
                conexao.commit();
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar os itens do cardápio.", e);
        }
    }

    private Produto criarProduto(String tipo, String nome, String descricao, double preco,
                                  int estoque, String categoria, boolean disponivel) {
        Produto produto = switch (tipo) {
            case "item_cardapio" -> new ItemCardapio(nome, categoria, descricao, preco, disponivel);
            case "com_alcool" -> new ComAlcool(nome, descricao, preco, estoque, categoria);
            case "sem_alcool" -> new SemAlcool(nome, descricao, preco, estoque, categoria);
            case "tira_gosto" -> new TiraGosto(nome, descricao, preco, estoque, categoria);
            case "refeicao" -> new Refeicao(nome, descricao, preco, estoque, categoria);
            case "servico" -> new Servico(nome, descricao, preco, estoque, categoria);
            case "descartavel" -> new Descartaveis(nome, descricao, preco, estoque, categoria);
            default -> null;
        };
        if (produto != null) {
            produto.setDisponivel(disponivel);
        }
        return produto;
    }

    private String identificarTipo(Produto produto) {
        if (produto instanceof ItemCardapio) return "item_cardapio";
        if (produto instanceof ComAlcool) return "com_alcool";
        if (produto instanceof SemAlcool) return "sem_alcool";
        if (produto instanceof TiraGosto) return "tira_gosto";
        if (produto instanceof Refeicao) return "refeicao";
        if (produto instanceof Servico) return "servico";
        if (produto instanceof Descartaveis) return "descartavel";
        throw new PersistenciaException("Tipo de produto não suportado.", null);
    }

    @Override
    public List<ItemEstoque> carregarItensEstoque() {
        List<ItemEstoque> itens = new ArrayList<>();
        String sql = """
                SELECT id, nome, unidade_medida, quantidade
                FROM itens_estoque
                ORDER BY nome
                """;
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                itens.add(new ItemEstoque(
                        resultado.getInt("id"),
                        resultado.getString("nome"),
                        resultado.getString("unidade_medida"),
                        resultado.getDouble("quantidade")
                ));
            }
            return itens;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível carregar o estoque.", e);
        }
    }

    @Override
    public ItemEstoque registrarMovimentacaoEstoque(
            Integer itemId,
            String nomeNovoItem,
            String unidadeMedida,
            TipoMovimentacao tipo,
            double quantidade
    ) {
        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);
            try {
                ItemEstoque atualizado = itemId == null
                        ? criarItemEstoque(conexao, nomeNovoItem, unidadeMedida, tipo, quantidade)
                        : movimentarItemExistente(conexao, itemId, tipo, quantidade);

                registrarMovimentacao(conexao, atualizado.getId(), tipo, quantidade);
                conexao.commit();
                return atualizado;
            } catch (SaldoInsuficienteException e) {
                conexao.rollback();
                throw e;
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SaldoInsuficienteException e) {
            throw e;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível registrar a movimentação.", e);
        }
    }

    private ItemEstoque criarItemEstoque(
            Connection conexao,
            String nome,
            String unidade,
            TipoMovimentacao tipo,
            double quantidade
    ) throws SQLException {
        if (tipo != TipoMovimentacao.ENTRADA) {
            throw new SaldoInsuficienteException();
        }

        String sql = """
                INSERT INTO itens_estoque (nome, unidade_medida, quantidade)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, nome);
            statement.setString(2, unidade);
            statement.setDouble(3, quantidade);
            statement.executeUpdate();

            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O banco não retornou o item criado.");
                }
                return new ItemEstoque(chaves.getInt(1), nome, unidade, quantidade);
            }
        }
    }

    private ItemEstoque movimentarItemExistente(
            Connection conexao,
            int itemId,
            TipoMovimentacao tipo,
            double quantidade
    ) throws SQLException {
        String consulta = """
                SELECT nome, unidade_medida, quantidade
                FROM itens_estoque
                WHERE id = ?
                """;
        try (PreparedStatement statement = conexao.prepareStatement(consulta)) {
            statement.setInt(1, itemId);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("Item de estoque não encontrado.");
                }

                String nome = resultado.getString("nome");
                String unidade = resultado.getString("unidade_medida");
                double saldoAtual = resultado.getDouble("quantidade");
                double novoSaldo = tipo == TipoMovimentacao.ENTRADA
                        ? saldoAtual + quantidade
                        : saldoAtual - quantidade;

                if (novoSaldo < 0) {
                    throw new SaldoInsuficienteException();
                }

                try (PreparedStatement atualizar = conexao.prepareStatement(
                        "UPDATE itens_estoque SET quantidade = ? WHERE id = ?")) {
                    atualizar.setDouble(1, novoSaldo);
                    atualizar.setInt(2, itemId);
                    atualizar.executeUpdate();
                }
                return new ItemEstoque(itemId, nome, unidade, novoSaldo);
            }
        }
    }

    private void registrarMovimentacao(
            Connection conexao,
            int itemId,
            TipoMovimentacao tipo,
            double quantidade
    ) throws SQLException {
        String sql = """
                INSERT INTO movimentacoes_estoque (item_id, tipo, quantidade, data_hora)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            statement.setString(2, tipo.name());
            statement.setDouble(3, quantidade);
            statement.setString(4, LocalDateTime.now().toString());
            statement.executeUpdate();
        }
    }
}
