package App.Persistencia;

import App.Utils.RuntimeTypeAdapterFactory;
import Model.Atendimento.Mesa;
import Model.Produtos.*;
import Model.Produtos.Alimentos.Refeicao;
import Model.Produtos.Alimentos.TiraGosto;
import Model.Produtos.Bedidas.ComAlcool;
import Model.Produtos.Bedidas.SemAlcool;
import Model.Produtos.Outros.Descartaveis;
import Model.Produtos.Outros.Servico;
import Model.Reservas.Reserva;
import Model.Reservas.ReservaComum;
import Model.Reservas.ReservaEvento;
import Model.Sistema.Config;
import Model.Usuarios.Garcom;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de persistência usando SQLite via JDBC.
 * Na primeira execução, migra automaticamente os dados dos arquivos JSON para o banco.
 */
public class DatabaseService implements InterfacePersistencia {

    private static final String DB_PATH       = "Dados/restaurante.db";
    private static final String CONFIG_FILE   = "Dados/config.json";
    private static final String USUARIOS_FILE = "Dados/usuarios.json";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson;

    public DatabaseService() {
        RuntimeTypeAdapterFactory<Produto> produtoAdapter = RuntimeTypeAdapterFactory
                .of(Produto.class, "tipo_classe")
                .registerSubtype(ComAlcool.class,    "com_alcool")
                .registerSubtype(SemAlcool.class,    "sem_alcool")
                .registerSubtype(Refeicao.class,     "refeicao")
                .registerSubtype(TiraGosto.class,    "tira_gosto")
                .registerSubtype(Servico.class,      "servico")
                .registerSubtype(Descartaveis.class, "descartavel");

        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(produtoAdapter)
                .setPrettyPrinting()
                .create();

        garantirDiretorio(DB_PATH);
        inicializarBanco();
        migrarJsonsSeNecessario();
    }

    // ─── Conexão ─────────────────────────────────────────────

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    // ─── DDL ─────────────────────────────────────────────────

    private void inicializarBanco() {
        String[] sqls = {
            """
            CREATE TABLE IF NOT EXISTS config (
                id                INTEGER PRIMARY KEY DEFAULT 1,
                nome_restaurante  TEXT    NOT NULL DEFAULT 'Sistema Restaurante',
                info_restaurante  TEXT    NOT NULL DEFAULT 'CNPJ / Endereco / Telefone',
                numero_de_mesas   INTEGER NOT NULL DEFAULT 10,
                taxa_de_servico   REAL    NOT NULL DEFAULT 10.0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                nome          TEXT    UNIQUE NOT NULL,
                senha         TEXT    NOT NULL,
                acesso_config INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS produtos (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                nome           TEXT    NOT NULL,
                descricao      TEXT,
                preco          REAL    NOT NULL,
                estoque        INTEGER NOT NULL,
                categoria_nome TEXT    NOT NULL,
                tipo_classe    TEXT    NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reservas (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                num_mesa         INTEGER NOT NULL,
                nome_cliente     TEXT    NOT NULL,
                data_hora        TEXT    NOT NULL,
                tipo             TEXT    NOT NULL,
                descricao_evento TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS historico_pagamentos (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                num_mesa       INTEGER NOT NULL,
                cliente_nome   TEXT    NOT NULL,
                valor_total    REAL    NOT NULL,
                tipo_pagamento TEXT    NOT NULL,
                detalhes       TEXT,
                data_hora      TEXT    NOT NULL
            )
            """
        };

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) stmt.execute(sql);
            System.out.println("[DB] Banco inicializado em: " + DB_PATH);
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao criar tabelas: " + e.getMessage());
        }
    }

    // ─── Migração JSON → SQLite ───────────────────────────────

    private void migrarJsonsSeNecessario() {
        try (Connection conn = conectar()) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM config")) {
                if (rs.getInt(1) == 0) migrarConfig(conn);
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM usuarios")) {
                if (rs.getInt(1) == 0) migrarUsuarios(conn);
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM produtos")) {
                if (rs.getInt(1) == 0) migrarProdutos(conn);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro na migração: " + e.getMessage());
        }
    }

    private void migrarConfig(Connection conn) {
        Config c = carregarConfigDeJson();
        String sql = "INSERT OR IGNORE INTO config (id, nome_restaurante, info_restaurante, numero_de_mesas, taxa_de_servico) VALUES (1,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNomeRestaurante());
            ps.setString(2, c.getInfoRestaurante());
            ps.setInt(3, c.getNumeroDeMesas());
            ps.setDouble(4, c.getTaxaDeServico());
            ps.executeUpdate();
            System.out.println("[DB] Config migrada do JSON.");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao migrar config: " + e.getMessage());
        }
    }

    private void migrarUsuarios(Connection conn) {
        List<Usuario> usuarios = carregarUsuariosDeJson();
        String sql = "INSERT OR IGNORE INTO usuarios (nome, senha, acesso_config) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Usuario u : usuarios) {
                ps.setString(1, u.getNome());
                ps.setString(2, u.getSenha());
                ps.setInt(3, u.AcessoEstoque() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("[DB] " + usuarios.size() + " usuario(s) migrado(s) do JSON.");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao migrar usuarios: " + e.getMessage());
        }
    }

    private void migrarProdutos(Connection conn) {
        PersistenceService jsonService = new PersistenceService();
        List<Produto> produtos = jsonService.carregarProdutos();
        String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria_nome, tipo_classe) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Produto p : produtos) {
                ps.setString(1, p.getNome());
                ps.setString(2, p.getDescricao());
                ps.setDouble(3, p.getPreco());
                ps.setInt(4, p.getEstoque());
                ps.setString(5, p.getCategoriaNome());
                ps.setString(6, inferirTipoClasse(p));
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("[DB] " + produtos.size() + " produto(s) migrado(s) do JSON.");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao migrar produtos: " + e.getMessage());
        }
    }

    private String inferirTipoClasse(Produto p) {
        if (p instanceof ComAlcool)    return "com_alcool";
        if (p instanceof SemAlcool)    return "sem_alcool";
        if (p instanceof TiraGosto)    return "tira_gosto";
        if (p instanceof Refeicao)     return "refeicao";
        if (p instanceof Servico)      return "servico";
        if (p instanceof Descartaveis) return "descartavel";
        return "refeicao";
    }

    // ─── CONFIG ──────────────────────────────────────────────

    @Override
    public Config carregarConfig() {
        String sql = "SELECT nome_restaurante, info_restaurante, numero_de_mesas, taxa_de_servico FROM config WHERE id = 1";
        try (Connection conn = conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Config config = new Config();
                config.setNomeRestaurante(rs.getString("nome_restaurante"));
                config.setInfoRestaurante(rs.getString("info_restaurante"));
                config.setNumeroDeMesas(rs.getInt("numero_de_mesas"));
                config.setTaxaDeServico(rs.getDouble("taxa_de_servico"));
                return config;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao carregar config: " + e.getMessage());
        }
        return new Config();
    }

    @Override
    public void salvarConfig(Config config) {
        String sql = "INSERT INTO config (id, nome_restaurante, info_restaurante, numero_de_mesas, taxa_de_servico) VALUES (1,?,?,?,?) "
                   + "ON CONFLICT(id) DO UPDATE SET nome_restaurante=excluded.nome_restaurante, "
                   + "info_restaurante=excluded.info_restaurante, numero_de_mesas=excluded.numero_de_mesas, "
                   + "taxa_de_servico=excluded.taxa_de_servico";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getNomeRestaurante());
            ps.setString(2, config.getInfoRestaurante());
            ps.setInt(3, config.getNumeroDeMesas());
            ps.setDouble(4, config.getTaxaDeServico());
            ps.executeUpdate();
            System.out.println("[DB] Config salva.");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar config: " + e.getMessage());
        }
    }

    // ─── USUARIOS ────────────────────────────────────────────

    @Override
    public List<Usuario> carregarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT nome, senha, acesso_config FROM usuarios ORDER BY nome";
        try (Connection conn = conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nome   = rs.getString("nome");
                String senha  = rs.getString("senha");
                boolean admin = rs.getInt("acesso_config") == 1;
                lista.add(admin ? new Interno(nome, senha) : new Garcom(nome, senha));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao carregar usuarios: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public void salvarUsuarios(List<Usuario> usuarios) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                conn.createStatement().executeUpdate("DELETE FROM usuarios");
                String sql = "INSERT INTO usuarios (nome, senha, acesso_config) VALUES (?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Usuario u : usuarios) {
                        ps.setString(1, u.getNome());
                        ps.setString(2, u.getSenha());
                        ps.setInt(3, u.AcessoEstoque() ? 1 : 0);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                System.out.println("[DB] " + usuarios.size() + " usuario(s) salvo(s).");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar usuarios: " + e.getMessage());
        }
    }

    // ─── PRODUTOS ────────────────────────────────────────────

    @Override
    public List<Produto> carregarProdutos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT nome, descricao, preco, estoque, categoria_nome, tipo_classe FROM produtos ORDER BY categoria_nome, nome";
        try (Connection conn = conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Produto p = criarProduto(
                        rs.getString("tipo_classe"),
                        rs.getString("nome"),
                        rs.getString("descricao"),
                        rs.getDouble("preco"),
                        rs.getInt("estoque"),
                        rs.getString("categoria_nome")
                );
                if (p != null) lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao carregar produtos: " + e.getMessage());
        }
        return lista;
    }

    private Produto criarProduto(String tipo, String nome, String descricao,
                                  double preco, int estoque, String categoria) {
        return switch (tipo) {
            case "com_alcool"  -> new ComAlcool(nome, descricao, preco, estoque, categoria);
            case "sem_alcool"  -> new SemAlcool(nome, descricao, preco, estoque, categoria);
            case "tira_gosto"  -> new TiraGosto(nome, descricao, preco, estoque, categoria);
            case "refeicao"    -> new Refeicao(nome, descricao, preco, estoque, categoria);
            case "servico"     -> new Servico(nome, descricao, preco, estoque, categoria);
            case "descartavel" -> new Descartaveis(nome, descricao, preco, estoque, categoria);
            default            -> null;
        };
    }

    @Override
    public void salvarProdutos(List<Produto> produtos) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                conn.createStatement().executeUpdate("DELETE FROM produtos");
                String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria_nome, tipo_classe) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Produto p : produtos) {
                        ps.setString(1, p.getNome());
                        ps.setString(2, p.getDescricao());
                        ps.setDouble(3, p.getPreco());
                        ps.setInt(4, p.getEstoque());
                        ps.setString(5, p.getCategoriaNome());
                        ps.setString(6, inferirTipoClasse(p));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                System.out.println("[DB] " + produtos.size() + " produto(s) salvo(s).");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar produtos: " + e.getMessage());
        }
    }

    // ─── RESERVAS ────────────────────────────────────────────

    @Override
    public void salvarReserva(Reserva reserva) {
        String sql = "INSERT INTO reservas (num_mesa, nome_cliente, data_hora, tipo, descricao_evento) VALUES (?,?,?,?,?)";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reserva.getMesa().getNumMesa());
            ps.setString(2, reserva.getNomeCliente());
            ps.setString(3, reserva.getDataHoraInicio().format(DT_FMT));
            ps.setString(4, reserva instanceof ReservaEvento ? "evento" : "comum");
            ps.setString(5, null);
            ps.executeUpdate();
            System.out.println("[DB] Reserva salva: mesa " + reserva.getMesa().getNumMesa());
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao salvar reserva: " + e.getMessage());
        }
    }

    @Override
    public List<Reserva> carregarReservas(List<Mesa> mesas) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT id, num_mesa, nome_cliente, data_hora, tipo FROM reservas "
                   + "WHERE datetime(data_hora) >= datetime('now', '-2 hours', 'localtime') ORDER BY data_hora ASC";
        try (Connection conn = conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int numMesa        = rs.getInt("num_mesa");
                String nomeCliente = rs.getString("nome_cliente");
                String dataHoraStr = rs.getString("data_hora");
                String tipo        = rs.getString("tipo");

                Mesa mesa = mesas.stream()
                        .filter(m -> m.getNumMesa() == numMesa)
                        .findFirst().orElse(null);
                if (mesa == null) continue;

                LocalDateTime dataHora = LocalDateTime.parse(dataHoraStr, DT_FMT);
                Reserva reserva = "evento".equals(tipo)
                        ? new ReservaEvento(nomeCliente, mesa, dataHora)
                        : new ReservaComum(nomeCliente, mesa, dataHora);

                lista.add(reserva);
                mesa.adicionarReserva(reserva);
            }
            System.out.println("[DB] " + lista.size() + " reserva(s) carregada(s).");
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao carregar reservas: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public void removerReserva(int reservaId) {
        String sql = "DELETE FROM reservas WHERE id = ?";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao remover reserva: " + e.getMessage());
        }
    }

    // ─── HISTORICO PAGAMENTOS ────────────────────────────────

    @Override
    public void registrarPagamento(int numMesa, String clienteNome, double valor,
                                    String tipoPagamento, String detalhes) {
        String sql = "INSERT INTO historico_pagamentos (num_mesa, cliente_nome, valor_total, tipo_pagamento, detalhes, data_hora) VALUES (?,?,?,?,?,?)";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numMesa);
            ps.setString(2, clienteNome);
            ps.setDouble(3, valor);
            ps.setString(4, tipoPagamento);
            ps.setString(5, detalhes);
            ps.setString(6, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
            System.out.println("[DB] Pagamento registrado: mesa " + numMesa + " R$" + String.format("%.2f", valor));
        } catch (SQLException e) {
            System.err.println("[DB] Erro ao registrar pagamento: " + e.getMessage());
        }
    }

    // ─── Fallback JSON (apenas para migração) ───────────────

    private Config carregarConfigDeJson() {
        File f = new File(CONFIG_FILE);
        if (!f.exists()) return new Config();
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            Config c = gson.fromJson(r, Config.class);
            return c != null ? c : new Config();
        } catch (IOException e) {
            return new Config();
        }
    }

    private List<Usuario> carregarUsuariosDeJson() {
        File f = new File(USUARIOS_FILE);
        if (!f.exists()) {
            List<Usuario> padrao = new ArrayList<>();
            padrao.add(new Interno("admin", "admin"));
            return padrao;
        }
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            List<Usuario> lista = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                boolean isInterno = obj.get("acessoConfig").getAsBoolean();
                String nome  = obj.get("nome").getAsString();
                String senha = obj.get("senha").getAsString();
                lista.add(isInterno ? new Interno(nome, senha) : new Garcom(nome, senha));
            }
            return lista;
        } catch (IOException | IllegalStateException e) {
            List<Usuario> padrao = new ArrayList<>();
            padrao.add(new Interno("admin", "admin"));
            return padrao;
        }
    }

    // ─── Utilitário ──────────────────────────────────────────

    private void garantirDiretorio(String caminho) {
        File f = new File(caminho);
        File dir = f.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
    }
}
