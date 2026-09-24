package App.Persistencia;

import Model.Atendimento.Comanda;
import Model.Atendimento.EstadoMesa;
import Model.Atendimento.Mesa;
import Model.Atendimento.Pedido;
import Model.Atendimento.Venda;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Persistência SQLite usada pela aplicação.
 *
 * Na primeira execução, os dados dos arquivos JSON existentes são copiados
 * para o banco. Os JSONs continuam no projeto somente como carga inicial.
 */
public class DatabaseService implements InterfacePersistencia {

    private static final String CAMINHO_PADRAO = "Dados/restaurante.db";
    private static final DateTimeFormatter DATA_HORA_BANCO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

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
        String criarVendas = """
                CREATE TABLE IF NOT EXISTS vendas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    comanda_id INTEGER NOT NULL,
                    data_abertura TEXT NOT NULL,
                    data_hora TEXT NOT NULL,
                    cliente_nome TEXT NOT NULL,
                    atendimento TEXT NOT NULL,
                    forma_pagamento TEXT NOT NULL,
                    subtotal REAL NOT NULL CHECK (subtotal >= 0),
                    desconto REAL NOT NULL CHECK (desconto >= 0),
                    total REAL NOT NULL CHECK (total >= 0),
                    funcionario TEXT NOT NULL
                )
                """;
        String criarVendaItens = """
                CREATE TABLE IF NOT EXISTS venda_itens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venda_id INTEGER NOT NULL,
                    produto_nome TEXT NOT NULL,
                    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
                    preco_unitario REAL NOT NULL CHECK (preco_unitario >= 0),
                    subtotal REAL NOT NULL CHECK (subtotal >= 0),
                    FOREIGN KEY (venda_id) REFERENCES vendas(id) ON DELETE CASCADE
                )
                """;

        String criarComandasAbertas = """
                CREATE TABLE IF NOT EXISTS comandas_abertas (
                    id INTEGER PRIMARY KEY,
                    cliente_nome TEXT NOT NULL,
                    mesa_numero INTEGER,
                    data_abertura TEXT NOT NULL
                )
                """;
        String criarPedidosAbertos = """
                CREATE TABLE IF NOT EXISTS pedidos_abertos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    comanda_id INTEGER NOT NULL,
                    produto_nome TEXT NOT NULL,
                    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
                    observacao TEXT,
                    horario TEXT NOT NULL,
                    numero_lote INTEGER NOT NULL DEFAULT 0,
                    atendente_nome TEXT,
                    FOREIGN KEY (comanda_id) REFERENCES comandas_abertas(id) ON DELETE CASCADE
                )
                """;

        // UC03 – tabela de mesas cadastradas individualmente
        String criarMesas = """
                CREATE TABLE IF NOT EXISTS mesas (
                    numero INTEGER PRIMARY KEY CHECK (numero > 0),
                    estado TEXT NOT NULL DEFAULT 'LIVRE'
                )
                """;

        try (Connection conexao = conectar(); Statement statement = conexao.createStatement()) {
            statement.execute(criarConfig);
            statement.execute(criarUsuarios);
            statement.execute(criarProdutos);
            statement.execute(criarItensEstoque);
            statement.execute(criarMovimentacoes);
            statement.execute(criarVendas);
            statement.execute(criarVendaItens);
            statement.execute(criarComandasAbertas);
            statement.execute(criarPedidosAbertos);
            statement.execute(criarMesas);
            garantirColunaDisponibilidade(statement);
            garantirColunaEstadoMesa(statement);
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

    private void garantirColunaEstadoMesa(Statement statement) throws SQLException {
        try {
            statement.execute("ALTER TABLE mesas ADD COLUMN estado TEXT NOT NULL DEFAULT 'LIVRE'");
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
                SELECT p.nome, p.descricao, p.preco,
                       CASE
                           WHEN p.tipo_classe = 'servico' THEN p.estoque
                           WHEN i.unidade_medida IS NOT NULL
                                AND LOWER(REPLACE(i.unidade_medida, '.', '')) = 'un'
                                AND ABS(i.quantidade - ROUND(i.quantidade)) < 0.000001
                           THEN CAST(i.quantidade AS INTEGER)
                           ELSE p.estoque
                       END AS estoque,
                       p.categoria_nome, p.tipo_classe, p.disponivel
                FROM produtos p
                LEFT JOIN itens_estoque i
                    ON LOWER(i.nome) = LOWER(p.nome)
                ORDER BY p.categoria_nome, p.nome
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
                sincronizarEstoqueDosProdutos(conexao, produtos);
                conexao.commit();
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar os itens do cardápio.", e);
        }
    }

    private void sincronizarEstoqueDosProdutos(
            Connection conexao,
            List<Produto> produtos
    ) throws SQLException {
        String sql = """
                INSERT INTO itens_estoque (nome, unidade_medida, quantidade)
                VALUES (?, 'un.', ?)
                ON CONFLICT(nome) DO UPDATE SET
                    quantidade = excluded.quantidade
                WHERE LOWER(REPLACE(itens_estoque.unidade_medida, '.', '')) = 'un'
                """;

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            for (Produto produto : produtos) {
                if (produto instanceof Servico) {
                    continue;
                }
                statement.setString(1, produto.getNome());
                statement.setDouble(2, produto.getEstoque());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void sincronizarProdutoComEstoque(
            Connection conexao,
            ItemEstoque item
    ) throws SQLException {
        if (item == null
                || item.getNome() == null
                || !"un".equalsIgnoreCase(
                        item.getUnidadeMedida()
                                .replace(".", "")
                                .trim()
                )
                || item.getQuantidade() < 0
                || item.getQuantidade() > Integer.MAX_VALUE
                || Math.rint(item.getQuantidade()) != item.getQuantidade()) {
            return;
        }

        String sql = """
                UPDATE produtos
                SET estoque = ?
                WHERE LOWER(nome) = LOWER(?)
                  AND tipo_classe <> 'servico'
                """;

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, (int) item.getQuantidade());
            statement.setString(2, item.getNome());
            statement.executeUpdate();
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
            produto.setEstoque(estoque);
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
                sincronizarProdutoComEstoque(conexao, atualizado);
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

    @Override
    public void salvarComandaAberta(
            Comanda comanda,
            Mesa mesa,
            Usuario funcionario
    ) {
        if (comanda == null) {
            throw new IllegalArgumentException("Comanda não pode ser nula.");
        }
        if (comanda.isFechada()) {
            throw new IllegalStateException("A comanda já está fechada.");
        }

        String inserirComanda = """
                INSERT INTO comandas_abertas (id, cliente_nome, mesa_numero, data_abertura)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    cliente_nome = excluded.cliente_nome,
                    mesa_numero = excluded.mesa_numero,
                    data_abertura = excluded.data_abertura
                """;
        String apagarPedidos = "DELETE FROM pedidos_abertos WHERE comanda_id = ?";
        String inserirPedido = """
                INSERT INTO pedidos_abertos (
                    comanda_id, produto_nome, quantidade, observacao, horario, numero_lote, atendente_nome
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String cliente = comanda.getClienteNome() == null || comanda.getClienteNome().isBlank()
                ? "Cliente " + comanda.getId()
                : comanda.getClienteNome().trim();
        String dataAbertura = DATA_HORA_BANCO.format(
                LocalDateTime.ofInstant(
                        comanda.getDataAbertura().toInstant(),
                        ZoneId.systemDefault()
                )
        );
        Integer numeroMesa = mesa == null ? null : mesa.getNumMesa();

        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement statement = conexao.prepareStatement(inserirComanda)) {
                    statement.setInt(1, comanda.getId());
                    statement.setString(2, cliente);
                    if (numeroMesa == null) {
                        statement.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(3, numeroMesa);
                    }
                    statement.setString(4, dataAbertura);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = conexao.prepareStatement(apagarPedidos)) {
                    statement.setInt(1, comanda.getId());
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = conexao.prepareStatement(inserirPedido)) {
                    for (Pedido pedido : comanda.getPedidos()) {
                        statement.setInt(1, comanda.getId());
                        statement.setString(2, pedido.getItem().getNome());
                        statement.setInt(3, pedido.getQuantidade());
                        statement.setString(4, pedido.getObservacao());
                        statement.setString(5, formatarData(pedido.getHorario()));
                        statement.setInt(6, pedido.getNumeroLote());
                        Usuario atendente = pedido.getAtendente() == null ? funcionario : pedido.getAtendente();
                        if (atendente == null || atendente.getNome() == null) {
                            statement.setNull(7, java.sql.Types.VARCHAR);
                        } else {
                            statement.setString(7, atendente.getNome());
                        }
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                conexao.commit();
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                if (erro instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new PersistenciaException("Não foi possível salvar a comanda aberta.", erro);
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar a comanda aberta.", e);
        }
    }

    @Override
    public void carregarComandasAbertas(
            List<Mesa> mesas,
            List<Comanda> comandasSemMesa,
            List<Produto> produtos,
            List<Usuario> usuarios
    ) {
        if (mesas == null || comandasSemMesa == null) {
            throw new IllegalArgumentException("As coleções de mesas e comandas são obrigatórias.");
        }

        String sqlComandas = """
                SELECT id, cliente_nome, mesa_numero, data_abertura
                FROM comandas_abertas
                ORDER BY id
                """;
        String sqlPedidos = """
                SELECT produto_nome, quantidade, observacao, horario, numero_lote, atendente_nome
                FROM pedidos_abertos
                WHERE comanda_id = ?
                ORDER BY id
                """;

        try (Connection conexao = conectar();
             PreparedStatement comandasStatement = conexao.prepareStatement(sqlComandas);
             ResultSet comandasResult = comandasStatement.executeQuery()) {

            ajustarProximoIdDasComandas(conexao);

            while (comandasResult.next()) {
                int id = comandasResult.getInt("id");
                String cliente = comandasResult.getString("cliente_nome");
                String dataTexto = comandasResult.getString("data_abertura");
                Date data = converterData(dataTexto);
                Comanda comanda = new Comanda(id, cliente, data);

                try (PreparedStatement pedidosStatement = conexao.prepareStatement(sqlPedidos)) {
                    pedidosStatement.setInt(1, id);
                    try (ResultSet pedidosResult = pedidosStatement.executeQuery()) {
                        while (pedidosResult.next()) {
                            Produto produto = localizarProduto(produtos, pedidosResult.getString("produto_nome"));
                            if (produto == null) {
                                throw new PersistenciaException(
                                        "O produto do pedido da comanda " + id + " não foi encontrado no cardápio.",
                                        null
                                );
                            }

                            Usuario atendente = localizarUsuario(
                                    usuarios,
                                    pedidosResult.getString("atendente_nome")
                            );
                            if (atendente == null && usuarios != null && !usuarios.isEmpty()) {
                                atendente = usuarios.get(0);
                            }

                            Date horario = converterData(pedidosResult.getString("horario"));
                            Pedido pedido = new Pedido(
                                    produto,
                                    pedidosResult.getInt("quantidade"),
                                    pedidosResult.getString("observacao"),
                                    atendente,
                                    horario,
                                    pedidosResult.getInt("numero_lote")
                            );
                            comanda.adicionarPedido(pedido);
                        }
                    }
                }

                comanda.recalcularProximoLote();

                int numeroMesa = comandasResult.getInt("mesa_numero");
                boolean temMesa = !comandasResult.wasNull();
                if (temMesa) {
                    Mesa mesa = localizarMesa(mesas, numeroMesa);
                    if (mesa != null && !mesa.temComandaAberta()) {
                        mesa.adicionarComanda(comanda);
                    }
                } else {
                    comandasSemMesa.add(comanda);
                }
            }
        } catch (SQLException | RuntimeException e) {
            if (e instanceof PersistenciaException persistenciaException) {
                throw persistenciaException;
            }
            throw new PersistenciaException("Não foi possível carregar as comandas abertas.", e);
        }
    }

    private void ajustarProximoIdDasComandas(Connection conexao) throws SQLException {
        String sql = """
                SELECT MAX(valor) AS maior_id FROM (
                    SELECT COALESCE(MAX(id), 0) AS valor FROM comandas_abertas
                    UNION ALL
                    SELECT COALESCE(MAX(comanda_id), 0) AS valor FROM vendas
                )
                """;
        try (Statement statement = conexao.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            if (resultado.next()) {
                Comanda.ajustarProximoIdPersistido(resultado.getInt("maior_id"));
            }
        }
    }

    private Mesa localizarMesa(List<Mesa> mesas, int numero) {
        for (Mesa mesa : mesas) {
            if (mesa.getNumMesa() == numero) {
                return mesa;
            }
        }
        return null;
    }

    private Produto localizarProduto(List<Produto> produtos, String nome) {
        if (produtos == null || nome == null) {
            return null;
        }
        for (Produto produto : produtos) {
            if (produto.getNome().equalsIgnoreCase(nome)) {
                return produto;
            }
        }
        return null;
    }

    private Usuario localizarUsuario(List<Usuario> usuarios, String nome) {
        if (usuarios == null || nome == null || nome.isBlank()) {
            return null;
        }
        for (Usuario usuario : usuarios) {
            if (usuario.getNome() != null && usuario.getNome().equalsIgnoreCase(nome)) {
                return usuario;
            }
        }
        return null;
    }

    private String formatarData(java.util.Date data) {
        if (data == null) {
            return DATA_HORA_BANCO.format(LocalDateTime.now());
        }
        return DATA_HORA_BANCO.format(
                LocalDateTime.ofInstant(
                        data.toInstant(),
                        ZoneId.systemDefault()
                )
        );
    }

    private java.util.Date converterData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return java.util.Date.from(
                    LocalDateTime.parse(texto, DATA_HORA_BANCO)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
            );
        } catch (RuntimeException e) {
            try {
                return java.util.Date.from(
                        LocalDateTime.parse(texto)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                );
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    @Override
    public Venda registrarVenda(Comanda comanda, Mesa mesa, Usuario funcionario, Model.Pagamento.Pagamento pagamento, double desconto) {
        if (comanda == null) {
            throw new IllegalArgumentException("Comanda não pode ser nula.");
        }
        if (comanda.isFechada()) {
            throw new IllegalStateException("A comanda já está fechada.");
        }
        if (pagamento == null || !pagamento.getStatusConfirmado()) {
            throw new IllegalStateException("O pagamento precisa estar confirmado.");
        }
        if (comanda.getPedidos().isEmpty()) {
            throw new IllegalStateException("A comanda precisa ter ao menos um item para ser fechada.");
        }
        double subtotalValidacao = comanda.calcularSubtotal();
        if (desconto < 0 || desconto > subtotalValidacao) {
            throw new IllegalArgumentException("Desconto inválido para a comanda.");
        }

        String sqlVenda = """
                INSERT INTO vendas (
                    comanda_id, data_abertura, data_hora, cliente_nome, atendimento,
                    forma_pagamento, subtotal, desconto, total, funcionario
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String sqlItem = """
                INSERT INTO venda_itens (
                    venda_id, produto_nome, quantidade, preco_unitario, subtotal
                ) VALUES (?, ?, ?, ?, ?)
                """;

        String atendimento = mesa == null
                ? "Cliente sem mesa"
                : String.format("Mesa %02d", mesa.getNumMesa());
        String cliente = comanda.getClienteNome() == null || comanda.getClienteNome().isBlank()
                ? "Cliente sem identificação"
                : comanda.getClienteNome().trim();
        String nomeFuncionario = funcionario == null || funcionario.getNome() == null || funcionario.getNome().isBlank()
                ? "Não informado"
                : funcionario.getNome();

        double subtotal = comanda.calcularSubtotal();
        double total = subtotal - desconto;
        LocalDateTime dataHora = LocalDateTime.now();
        LocalDateTime dataAbertura = LocalDateTime.ofInstant(
                comanda.getDataAbertura().toInstant(),
                ZoneId.systemDefault()
        );

        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);
            try {
                int vendaId;
                try (PreparedStatement statement = conexao.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, comanda.getId());
                    statement.setString(2, DATA_HORA_BANCO.format(dataAbertura));
                    statement.setString(3, DATA_HORA_BANCO.format(dataHora));
                    statement.setString(4, cliente);
                    statement.setString(5, atendimento);
                    statement.setString(6, pagamento.getTipo());
                    statement.setDouble(7, subtotal);
                    statement.setDouble(8, desconto);
                    statement.setDouble(9, total);
                    statement.setString(10, nomeFuncionario);
                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("O banco não retornou o identificador da venda.");
                        }
                        vendaId = keys.getInt(1);
                    }
                }

                try (PreparedStatement statement = conexao.prepareStatement(sqlItem)) {
                    for (Pedido pedido : comanda.getPedidos()) {
                        statement.setInt(1, vendaId);
                        statement.setString(2, pedido.getItem().getNome());
                        statement.setInt(3, pedido.getQuantidade());
                        statement.setDouble(4, pedido.getItem().getPreco());
                        statement.setDouble(5, pedido.getSubtotal());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                try (PreparedStatement statement = conexao.prepareStatement(
                        "DELETE FROM comandas_abertas WHERE id = ?")) {
                    statement.setInt(1, comanda.getId());
                    statement.executeUpdate();
                }

                conexao.commit();
                return new Venda(
                        vendaId,
                        comanda.getId(),
                        dataHora,
                        cliente,
                        atendimento,
                        pagamento.getTipo(),
                        subtotal,
                        desconto,
                        total,
                        nomeFuncionario
                );
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                if (erro instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new PersistenciaException("Não foi possível registrar a venda.", erro);
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível registrar a venda.", e);
        }
    }

    @Override
    public List<Venda> carregarHistoricoVendas(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String formaPagamento
    ) {
        List<Venda> vendas = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, comanda_id, data_hora, cliente_nome, atendimento,
                       forma_pagamento, subtotal, desconto, total, funcionario
                FROM vendas
                WHERE 1 = 1
                """);
        List<String> parametros = new ArrayList<>();

        if (dataInicial != null) {
            sql.append(" AND data_hora >= ?");
            parametros.add(DATA_HORA_BANCO.format(dataInicial.atStartOfDay()));
        }
        if (dataFinal != null) {
            sql.append(" AND data_hora < ?");
            parametros.add(DATA_HORA_BANCO.format(dataFinal.plusDays(1).atStartOfDay()));
        }
        if (formaPagamento != null
                && !formaPagamento.isBlank()
                && !"Todas".equalsIgnoreCase(formaPagamento)) {
            sql.append(" AND forma_pagamento = ?");
            parametros.add(formaPagamento);
        }
        sql.append(" ORDER BY data_hora DESC, id DESC");

        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                statement.setString(i + 1, parametros.get(i));
            }

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    vendas.add(new Venda(
                            resultado.getInt("id"),
                            resultado.getInt("comanda_id"),
                            LocalDateTime.parse(resultado.getString("data_hora"), DATA_HORA_BANCO),
                            resultado.getString("cliente_nome"),
                            resultado.getString("atendimento"),
                            resultado.getString("forma_pagamento"),
                            resultado.getDouble("subtotal"),
                            resultado.getDouble("desconto"),
                            resultado.getDouble("total"),
                            resultado.getString("funcionario")
                    ));
                }
            }
            return vendas;
        } catch (SQLException | RuntimeException e) {
            throw new PersistenciaException("Não foi possível carregar o histórico de vendas.", e);
        }
    }

    // ── UC03 – Mesas ─────────────────────────────────────────────────────────

    /** Retorna as mesas e os estados persistidos em ordem numérica. */
    @Override
    public List<Mesa> carregarMesas() {
        List<Mesa> mesas = new ArrayList<>();
        String sql = "SELECT numero, estado FROM mesas ORDER BY numero";
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                EstadoMesa estado;
                try {
                    estado = EstadoMesa.valueOf(resultado.getString("estado"));
                } catch (IllegalArgumentException | NullPointerException e) {
                    estado = EstadoMesa.LIVRE;
                }
                mesas.add(new Mesa(resultado.getInt("numero"), estado));
            }
            return mesas;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível carregar as mesas.", e);
        }
    }

    /** Cria ou atualiza uma mesa e seu estado. */
    @Override
    public void salvarMesa(Mesa mesa) {
        if (mesa == null) {
            throw new IllegalArgumentException("Mesa não pode ser nula.");
        }
        String sql = """
                INSERT INTO mesas (numero, estado) VALUES (?, ?)
                ON CONFLICT(numero) DO UPDATE SET estado = excluded.estado
                """;
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, mesa.getNumMesa());
            statement.setString(2, mesa.getEstado().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Não foi possível salvar a mesa " + mesa.getNumMesa() + ".",
                    e
            );
        }
    }

    /**
     * Remove a mesa com o número informado.
     */
    @Override
    public void removerMesa(int numero) {
        String sql = "DELETE FROM mesas WHERE numero = ?";
        try (Connection conexao = conectar();
             PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, numero);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível remover a mesa " + numero + ".", e);
        }
    }
}
