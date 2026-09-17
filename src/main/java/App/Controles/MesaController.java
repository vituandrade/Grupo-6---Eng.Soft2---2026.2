package App.Controles;

import App.Persistencia.InterfacePersistencia;
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
    private BorderPane painelConteudo;
    @FXML
    private Label labelUsuario;
    @FXML
    private Label labelTituloPagina;
    @FXML
    private TilePane painelMesas;
    @FXML
    private Button botaoProdutos;
    @FXML
    private Button botaoMesas;
    @FXML
    private Button botaoComandas;


    private Usuario usuarioLogado;
    private List<Mesa> listaDeMesas = new ArrayList<>();
    private List<Produto> listaDeProdutos;

    private Config config;
    private InterfacePersistencia persistenceService;

    public void setUsuarioLogado(Usuario usuario, List<Produto> produtos, Config config, InterfacePersistencia service) {
        this.usuarioLogado = usuario;
        this.listaDeProdutos = produtos;
        this.config = config;
        this.persistenceService = service;

        labelUsuario.setText(usuario.getNome());

        if (usuario instanceof Interno) {
            botaoProdutos.setVisible(true);
            botaoProdutos.setManaged(true);
        }

        this.painelConteudo = (BorderPane) painelRaiz.getCenter();
        this.centroOriginalMesas = painelConteudo.getCenter();
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
            controller.inicializar(mesaSelecionada, this.usuarioLogado, itensVendaveis);

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
    private void abrirDashboardMesas() {
        sincronizarMesasComConfig();
        painelConteudo.setCenter(this.centroOriginalMesas);
        selecionarMenu(botaoMesas, "Mesas");
    }

    @FXML
    private void abrirProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ProdutosView.fxml"));
            Node painelProdutos = loader.load();
            ProdutosController controller = loader.getController();
            this.listaDeProdutos = persistenceService.carregarProdutos();
            controller.inicializar(this.listaDeProdutos, this.persistenceService);
            painelConteudo.setCenter(painelProdutos);
            selecionarMenu(botaoProdutos, "Cardápio");
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar a tela de produtos.");
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

            painelConteudo.setCenter(painelComandas);
            selecionarMenu(botaoComandas, "Comandas");

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir a lista de comandas.");
        }
    }

    private void selecionarMenu(Button botaoSelecionado, String titulo) {
        labelTituloPagina.setText(titulo);
        for (Button botao : List.of(botaoMesas, botaoComandas, botaoProdutos)) {
            botao.getStyleClass().remove("menu-button-active");
        }
        botaoSelecionado.getStyleClass().add("menu-button-active");
    }

}
