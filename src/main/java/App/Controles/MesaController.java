package App.Controles;

import App.Persistencia.InterfacePersistencia;
import App.Persistencia.PersistenceService;
import Model.Atendimento.Mesa;
import Model.Produtos.ItemVendavel;
import Model.Produtos.Produto;
import Model.Sistema.Config;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MesaController extends BaseController{

    @FXML
    private BorderPane painelRaiz;
    private Node centroOriginalMesas;
    @FXML
    private Label labelUsuario;
    @FXML
    private Button botaoConfig;
    @FXML
    private TilePane painelMesas;
    @FXML
    private Button botaoProdutos;
    @FXML
    private Button botaoUsuarios;
    @FXML
    private Button botaoReserva;


    private Usuario usuarioLogado;
    private List<Mesa> listaDeMesas = new ArrayList<>();
    private List<Produto> listaDeProdutos;
    private List<Usuario> listaDeUsuarios;

    private Config config;
    private InterfacePersistencia persistenceService;

    public void setUsuarioLogado(Usuario usuario, List<Usuario> usuarios, List<Produto> produtos, Config config, InterfacePersistencia service) {
        this.usuarioLogado = usuario;
        this.listaDeUsuarios = usuarios;
        this.listaDeProdutos = produtos;
        this.config = config;
        this.persistenceService = service;

        labelUsuario.setText("Usuário: " + usuario.getNome() +
                " (" + usuario.getClass().getSimpleName() + ")");

        if (usuario instanceof Interno) {
            botaoConfig.setVisible(true);
            botaoProdutos.setVisible(true);
            botaoUsuarios.setVisible(true);
            botaoReserva.setVisible(true);
        }

        this.centroOriginalMesas = painelRaiz.getCenter();
        carregarMesas();
    }

    private void carregarMesas() {
        painelMesas.getChildren().clear();
        this.listaDeMesas.clear();

        int numeroTotalDeMesas = this.config.getNumeroDeMesas();
        for (int i = 1; i <= numeroTotalDeMesas; i++) {
            Mesa novaMesa = new Mesa(i);
            this.listaDeMesas.add(novaMesa);
            VBox mesaBox = criarMesaVisual(novaMesa);
            painelMesas.getChildren().add(mesaBox);
        }

        // Carrega reservas persistidas no banco e vincula às mesas
        this.persistenceService.carregarReservas(this.listaDeMesas);
        atualizarVisualDasMesas();
    }

    private void atualizarVisualDasMesas() {
        painelMesas.getChildren().clear();
        for (Mesa mesa : this.listaDeMesas) {
            VBox mesaBox = criarMesaVisual(mesa);
            painelMesas.getChildren().add(mesaBox);
        }
    }

    private VBox criarMesaVisual(Mesa mesa) {
        VBox box = new VBox(10);
        String estiloFundo;
        String statusTexto;
        Button botaoAcao = new Button();

        if (mesa.isAguardandoPagamento()) {
            estiloFundo = "-fx-background-color: #fff3cd;";
            statusTexto = "Aguardando Pagamento";
            botaoAcao.setText("Gerenciar");
            botaoAcao.setOnAction(e -> abrirMesaEspecifica(mesa.getNumMesa()));

        } else if (mesa.isOcupada()) {
            estiloFundo = "-fx-background-color: #f8d7da;";
            statusTexto = "Ocupada (" + mesa.getComandas().size() + ")";
            botaoAcao.setText("Gerenciar");
            botaoAcao.setOnAction(e -> abrirMesaEspecifica(mesa.getNumMesa()));

        } else if (mesa.estaReservadaAgora()) {
            estiloFundo = "-fx-background-color: #cce5ff;"; // Azul claro
            statusTexto = "Reservada - " + mesa.getNomeClienteReserva();

            botaoAcao.setText("Ocupar");
            botaoAcao.setOnAction(e -> abrirMesaEspecifica(mesa.getNumMesa()));

        } else {
            estiloFundo = "-fx-background-color: #d4edda;";
            statusTexto = "Livre";
            botaoAcao.setText("Abrir Mesa");
            botaoAcao.setOnAction(e -> abrirMesaEspecifica(mesa.getNumMesa()));
        }

        box.setStyle("-fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 10; " + estiloFundo);
        box.setPrefSize(120, 100);

        Label label = new Label("Mesa " + mesa.getNumMesa());
        label.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label(statusTexto);

        box.getChildren().addAll(label, statusLabel, botaoAcao);
        return box;
    }

    private void abrirMesaEspecifica(int numeroMesa) {
        Mesa mesaSelecionada = this.listaDeMesas.get(numeroMesa - 1);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/GerenciarMesaView.fxml"));
            Parent root = loader.load();
            GerenciarMesaController controller = loader.getController();

            List<ItemVendavel> itensVendaveis = new ArrayList<>(this.persistenceService.carregarProdutos());
            controller.inicializar(mesaSelecionada, this.usuarioLogado, itensVendaveis,
                    this.persistenceService);

            Stage gerenciarStage = new Stage();
            gerenciarStage.initModality(Modality.APPLICATION_MODAL);
            gerenciarStage.setTitle("Gerenciando Mesa " + numeroMesa);
            gerenciarStage.setScene(new Scene(root));

            gerenciarStage.setResizable(false);
            gerenciarStage.showAndWait();

            atualizarVisualDasMesas();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir o gerenciador da mesa.");
        }
    }

    @FXML
    private void abrirConfiguracoes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ConfigView.fxml"));
            Node painelConfig = loader.load();
            ConfigController controller = loader.getController();
            controller.inicializar(this.config, this.persistenceService);
            painelRaiz.setCenter(painelConfig);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar a tela de configurações.");
        }
    }

    @FXML
    private void abrirDashboardMesas() {
        sincronizarMesasComConfig();
        painelRaiz.setCenter(this.centroOriginalMesas);
    }

    @FXML
    private void abrirProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ProdutosView.fxml"));
            Node painelProdutos = loader.load();
            ProdutosController controller = loader.getController();
            this.listaDeProdutos = persistenceService.carregarProdutos();
            controller.inicializar(this.listaDeProdutos, this.persistenceService);
            painelRaiz.setCenter(painelProdutos);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar a tela de produtos.");
        }
    }

    @FXML
    private void abrirUsuarios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/UsuariosView.fxml"));
            Node painelUsuarios = loader.load();
            UsuariosController controller = loader.getController();

            controller.inicializar(this.listaDeUsuarios, (PersistenceService) this.persistenceService);

            painelRaiz.setCenter(painelUsuarios);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar a tela de usuários.");
        }
    }
    private void sincronizarMesasComConfig() {
        int numeroAtualNaLista = this.listaDeMesas.size();
        int numeroDesejadoDoConfig = this.config.getNumeroDeMesas();

        if (numeroAtualNaLista == numeroDesejadoDoConfig) {
            atualizarVisualDasMesas();
            return;
        }

        if (numeroDesejadoDoConfig > numeroAtualNaLista) {
            for (int i = numeroAtualNaLista + 1; i <= numeroDesejadoDoConfig; i++) {
                Mesa novaMesa = new Mesa(i);
                this.listaDeMesas.add(novaMesa);
            }
        } else {
            this.listaDeMesas.removeIf(mesa -> mesa.getNumMesa() > numeroDesejadoDoConfig);
        }
        atualizarVisualDasMesas();
    }
    @FXML
    private void abrirNovaReserva() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ReservaView.fxml"));
            Parent root = loader.load();

            ReservaController controller = loader.getController();
            controller.inicializar(this.listaDeMesas, this::atualizarVisualDasMesas,
                    this.persistenceService);

            Stage stage = new Stage();
            stage.setTitle("Nova Reserva");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirListaComandas() {
        try {
            sincronizarMesasComConfig();
            if (this.listaDeProdutos == null) {
                this.listaDeProdutos = persistenceService.carregarProdutos();
            }
            List<ItemVendavel> itensVendaveis = new ArrayList<>(this.listaDeProdutos);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ListaComandas.fxml"));
            Node painelComandas = loader.load();

            ListaComandasController controller = loader.getController();

            controller.inicializar(this.listaDeMesas, this.usuarioLogado, itensVendaveis);

            painelRaiz.setCenter(painelComandas);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir a lista de comandas.");
        }
    }

    @FXML
    private void abrirRelatorioPedidos() {
        try {
            sincronizarMesasComConfig();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/RelatorioPedidos.fxml"));
            Node painelRelatorio = loader.load();
            RelatorioPedidosController controller = loader.getController();
            controller.inicializar(this.listaDeMesas);

            painelRaiz.setCenter(painelRelatorio);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir o relatório de pedidos.");
        }
    }

}