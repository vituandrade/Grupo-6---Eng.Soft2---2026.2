package App.Controles;

import App.Persistencia.InterfacePersistencia;

import Model.Atendimento.EstadoMesa;
import Model.Atendimento.Mesa;
import Model.Atendimento.Comanda;
import Model.Atendimento.Venda;
import Model.Produtos.ItemVendavel;
import Model.Produtos.Produto;
import Model.Sistema.Config;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MesaController extends BaseController {

    @FXML
    private BorderPane painelRaiz;

    private Node centroOriginalMesas;
    private Node barraSuperiorOriginal;
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
    private Button botaoPainelInicial;

    @FXML
    private Button botaoMesas;

    @FXML
    private Button botaoComandas;

    @FXML
    private Button botaoEstoque;

    @FXML
    private Button botaoHistorico;

    @FXML
    private Button botaoNovaComanda;

    // UC03 – botão cadastrar nova mesa (visível só para Interno)
    @FXML
    private Button botaoNovaMesa;

    private Usuario usuarioLogado;
    private List<Mesa> listaDeMesas = new ArrayList<>();
    private List<Produto> listaDeProdutos;
    private final List<Comanda> comandasSemMesa = new ArrayList<>();
    private Config config;
    private InterfacePersistencia persistenceService;

    public void setUsuarioLogado(
            Usuario usuario,
            List<Produto> produtos,
            Config config,
            InterfacePersistencia service
    ) {
        this.usuarioLogado = usuario;
        this.listaDeProdutos = produtos;
        this.config = config;
        this.persistenceService = service;

        labelUsuario.setText(usuario.getNome());

        if (usuario instanceof Interno) {
            botaoProdutos.setVisible(true);
            botaoProdutos.setManaged(true);
            botaoEstoque.setVisible(true);
            botaoEstoque.setManaged(true);
            // UC03 – somente Interno pode cadastrar novas mesas
            botaoNovaMesa.setVisible(true);
            botaoNovaMesa.setManaged(true);
        }

        this.painelConteudo =
                (BorderPane) painelRaiz.getCenter();

        this.centroOriginalMesas =
                painelConteudo.getCenter();

        this.barraSuperiorOriginal =
                painelConteudo.getTop();

        restaurarBarraSuperior();
        carregarMesas();
        carregarComandasPersistidas();
        atualizarVisualDasMesas();
        abrirPainelInicial();
    }

    private void carregarComandasPersistidas() {
        if (this.persistenceService == null) {
            return;
        }
        if (this.listaDeProdutos == null) {
            this.listaDeProdutos = this.persistenceService.carregarProdutos();
        }
        List<Usuario> usuarios = this.persistenceService.carregarUsuarios();
        this.comandasSemMesa.clear();
        this.persistenceService.carregarComandasAbertas(
                this.listaDeMesas,
                this.comandasSemMesa,
                this.listaDeProdutos,
                usuarios
        );
        // Uma comanda restaurada pode transformar uma mesa livre em ocupada.
        for (Mesa mesa : listaDeMesas) {
            persistenceService.salvarMesa(mesa);
        }
    }

    // ── Carregamento ────────────────────────────────────────────────────────

    /**
     * Carrega as mesas a partir do banco de dados (UC03).
     * Se o banco ainda não possui mesas cadastradas, usa config.getNumeroDeMesas()
     * como carga inicial (compatibilidade com instalações existentes).
     */
    private void carregarMesas() {
        painelMesas.getChildren().clear();
        this.listaDeMesas.clear();

        List<Mesa> mesasPersistidas = new ArrayList<>(persistenceService.carregarMesas());

        if (mesasPersistidas.isEmpty()) {
            // Carga inicial: semeia a partir da configuração global
            int totalConfig = this.config.getNumeroDeMesas();
            for (int i = 1; i <= totalConfig; i++) {
                Mesa mesa = new Mesa(i);
                persistenceService.salvarMesa(mesa);
                mesasPersistidas.add(mesa);
            }
        }

        this.listaDeMesas.addAll(mesasPersistidas);
    }

    private void atualizarVisualDasMesas() {
        painelMesas.getChildren().clear();

        for (Mesa mesa : this.listaDeMesas) {
            VBox mesaBox =
                    criarMesaVisual(mesa);

            painelMesas.getChildren().add(mesaBox);
        }
    }

    private VBox criarMesaVisual(Mesa mesa) {
        VBox box = new VBox(12);
        box.getStyleClass().add("table-card");
        box.setPrefSize(185, 125);

        Label titulo = new Label(String.format("Mesa %02d", mesa.getNumMesa()));
        titulo.getStyleClass().add("table-title");

        Label status = new Label(mesa.getEstado().getDescricao());
        status.setMaxWidth(Double.MAX_VALUE);
        status.getStyleClass().addAll("status-pill", classeCssDoEstado(mesa.getEstado()));

        String detalhe = mesa.getComandas().stream()
                .filter(comanda -> !comanda.isFechada())
                .findFirst()
                .map(comanda -> "Comanda #" + comanda.getId())
                .orElse("Sem comanda");
        Label detalheLabel = new Label(detalhe);
        detalheLabel.getStyleClass().add("table-detail");

        if (usuarioLogado instanceof Interno) {
            status.setTooltip(new Tooltip("Clique para alterar o estado"));
            status.setOnMouseClicked(evento -> {
                evento.consume();
                abrirAlteracaoEstado(mesa);
            });
        }

        box.setOnMouseClicked(evento -> abrirMesaEspecifica(mesa.getNumMesa()));
        box.getChildren().addAll(titulo, status, detalheLabel);

        return box;
    }

    private String classeCssDoEstado(EstadoMesa estado) {
        return switch (estado) {
            case LIVRE -> "status-livre";
            case OCUPADA -> "status-ocupada";
            case AGUARDANDO_FECHAMENTO -> "status-aguardando";
        };
    }

    private void abrirMesaEspecifica(
            int numeroMesa
    ) {
        Mesa mesaSelecionada = this.listaDeMesas.stream()
                .filter(mesa -> mesa.getNumMesa() == numeroMesa)
                .findFirst()
                .orElse(null);

        if (mesaSelecionada == null) {
            mostrarAlerta("Mesa", "A mesa selecionada não foi encontrada.");
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/GerenciarMesaView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            GerenciarMesaController controller =
                    loader.getController();

            if (this.listaDeProdutos == null) {
                this.listaDeProdutos = this.persistenceService.carregarProdutos();
            }

            List<ItemVendavel> itensVendaveis =
                    new ArrayList<>(this.listaDeProdutos);

            controller.inicializar(
                    mesaSelecionada,
                    this.usuarioLogado,
                    this.comandasSemMesa,
                    itensVendaveis,
                    this
            );

            mostrarTelaCompleta(root);

            selecionarMenu(
                    botaoMesas,
                    "Mesas"
            );

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir o gerenciador da mesa."
            );
        }
    }

    /**
     * UC03 – Fluxo principal: cadastrar nova mesa.
     * Exibe diálogo para informar o número; valida (FA01, FA02) e persiste.
     */
    @FXML
    private void adicionarNovaMesa() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/CadastrarMesa.fxml"));
            Node formulario = loader.load();
            CadastrarMesaController controller = loader.getController();

            ButtonType salvar = new ButtonType("SALVAR", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
            Dialog<ButtonType> dialogo = criarDialogo("Cadastrar nova mesa", formulario);
            dialogo.getDialogPane().getButtonTypes().addAll(cancelar, salvar);

            Button botaoSalvar = (Button) dialogo.getDialogPane().lookupButton(salvar);
            estilizarBotoesDialogo(dialogo, salvar, cancelar);
            botaoSalvar.addEventFilter(ActionEvent.ACTION, evento -> {
                if (!controller.validar(listaDeMesas).valido()) {
                    evento.consume();
                    return;
                }

                Mesa novaMesa = new Mesa(controller.getNumero());
                try {
                    persistenceService.salvarMesa(novaMesa);
                    listaDeMesas.add(novaMesa);
                    listaDeMesas.sort((a, b) -> Integer.compare(a.getNumMesa(), b.getNumMesa()));
                    atualizarVisualDasMesas();
                } catch (RuntimeException erro) {
                    evento.consume();
                    controller.exibirFalhaDePersistencia();
                }
            });
            dialogo.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("Erro", "Não foi possível abrir o cadastro de mesa.");
        }
    }

    private void abrirAlteracaoEstado(Mesa mesa) {
        Label estadoAtual = new Label("Estado atual: " + mesa.getEstado());
        estadoAtual.getStyleClass().add("dialog-subtitle");

        ComboBox<EstadoMesa> seletor = new ComboBox<>();
        seletor.getItems().setAll(EstadoMesa.values());
        seletor.setValue(mesa.getEstado());
        seletor.setMaxWidth(Double.MAX_VALUE);
        seletor.setPrefHeight(42);
        seletor.getStyleClass().add("estado-mesa-select");

        Label erro = new Label();
        erro.setWrapText(true);
        erro.setMinHeight(34);
        erro.getStyleClass().add("form-error");

        VBox conteudo = new VBox(10, estadoAtual, seletor, erro);
        conteudo.setPadding(new Insets(4));
        conteudo.setPrefWidth(430);

        ButtonType salvar = new ButtonType("SALVAR", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialogo = criarDialogo(
                String.format("Alterar estado da Mesa %02d", mesa.getNumMesa()),
                conteudo
        );
        dialogo.getDialogPane().getButtonTypes().addAll(cancelar, salvar);
        estilizarBotoesDialogo(dialogo, salvar, cancelar);

        Button botaoSalvar = (Button) dialogo.getDialogPane().lookupButton(salvar);
        botaoSalvar.addEventFilter(ActionEvent.ACTION, evento -> {
            EstadoMesa novoEstado = seletor.getValue();
            try {
                mesa.validarAlteracao(novoEstado);

                // Persiste primeiro: se o banco falhar, o cartão mantém o estado anterior.
                persistenceService.salvarMesa(new Mesa(mesa.getNumMesa(), novoEstado));
                mesa.alterarEstado(novoEstado);
                atualizarVisualDasMesas();
            } catch (IllegalArgumentException | IllegalStateException e) {
                evento.consume();
                erro.setText(e.getMessage());
            } catch (RuntimeException e) {
                evento.consume();
                erro.setText("Não foi possível gravar o novo estado. Tente novamente.");
            }
        });
        dialogo.showAndWait();
    }

    private Dialog<ButtonType> criarDialogo(String titulo, Node conteudo) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.initStyle(StageStyle.TRANSPARENT);
        dialogo.initOwner(painelRaiz.getScene().getWindow());
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.getDialogPane().getStylesheets().add(
                getClass().getResource("/App/Mesa.css").toExternalForm()
        );
        return dialogo;
    }

    private void estilizarBotoesDialogo(
            Dialog<ButtonType> dialogo,
            ButtonType salvar,
            ButtonType cancelar
    ) {
        dialogo.getDialogPane().lookupButton(salvar).getStyleClass().add("save-button");
        dialogo.getDialogPane().lookupButton(cancelar).getStyleClass().add("cancel-button");
    }

    @FXML
    public void abrirNovaComanda() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/AbrirComandaView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            AbrirComandaController dialogController =
                    loader.getController();

            dialogController.inicializar(
                    this.listaDeMesas
            );

            Stage stage =
                    new Stage();

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            stage.setTitle(
                    "Abrir comanda"
            );

            stage.setScene(
                    new Scene(
                            root,
                            800,
                            700
                    )
            );

            stage.setResizable(false);
            stage.showAndWait();

            if (dialogController.isConfirmada()
                    && dialogController.getComandaCriada() != null) {

                Comanda comandaCriada =
                        dialogController.getComandaCriada();

                Mesa mesaDaComanda =
                        dialogController.getMesaSelecionada();

                if (mesaDaComanda == null) {
                    comandasSemMesa.add(
                            comandaCriada
                    );
                }

                abrirTelaDaComanda(
                        mesaDaComanda,
                        comandaCriada
                );

                atualizarVisualDasMesas();
            }

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir o diálogo de nova comanda."
            );
        }
    }

    public ComandaController abrirTelaDaComanda(
            Mesa mesa,
            Comanda comanda
    ) throws IOException {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/App/ComandaView.fxml"
                        )
                );

        Parent root =
                loader.load();

        ComandaController controller =
                loader.getController();

        controller.setNavegador(this);

        persistirComandaAberta(comanda, mesa);

        this.listaDeProdutos = this.persistenceService.carregarProdutos();

        List<ItemVendavel> itensVendaveis =
                new ArrayList<>(this.listaDeProdutos);

        controller.carregarComanda(
                mesa,
                comanda,
                this.usuarioLogado,
                itensVendaveis
        );

        mostrarTelaCompleta(root);

        selecionarMenu(
                botaoComandas,
                "Comandas"
        );

        return controller;
    }

    public void abrirFechamento(
            Comanda comanda
    ) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/FechamentoContaView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            FechamentoContaController controller =
                    loader.getController();

            Mesa mesaDaComanda = encontrarMesaDaComanda(comanda);
            if (mesaDaComanda != null) {
                mesaDaComanda.setAguardandoPagamento(true);
                persistirMesa(mesaDaComanda);
            }

            controller.inicializar(
                    comanda,
                    this.usuarioLogado,
                    this,
                    mesaDaComanda
            );

            mostrarTelaCompleta(root);

            selecionarMenu(
                    botaoComandas,
                    "Comandas"
            );

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir a tela de fechamento da conta."
            );
        }
    }

    /**
     * Persiste a lista de produtos mantida pela sessão.
     * O fluxo de comandas usa os mesmos objetos, evitando divergência entre
     * o estoque consultado no pedido e o estoque salvo no banco.
     */
    public void persistirProdutosAtualizados() {
        if (this.listaDeProdutos == null) {
            throw new IllegalStateException("Lista de produtos não carregada.");
        }
        this.persistenceService.salvarProdutos(this.listaDeProdutos);
    }

    /**
     * Registra a venda decorrente do fechamento da comanda no histórico local.
     * A persistência ocorre antes da alteração final do estado da comanda.
     */
    public Venda registrarVendaNoHistorico(
            Comanda comanda,
            Mesa mesa,
            Model.Pagamento.Pagamento pagamento,
            double desconto
    ) {
        return this.persistenceService.registrarVenda(
                comanda,
                mesa,
                this.usuarioLogado,
                pagamento,
                desconto
        );
    }

    public void persistirComandaAberta(Comanda comanda, Mesa mesa) {
        if (this.persistenceService == null) {
            throw new IllegalStateException("Serviço de persistência não configurado.");
        }
        this.persistenceService.salvarComandaAberta(
                comanda,
                mesa,
                this.usuarioLogado
        );
        if (mesa != null) {
            this.persistenceService.salvarMesa(mesa);
        }
    }

    public void persistirMesa(Mesa mesa) {
        if (mesa == null || this.persistenceService == null) {
            return;
        }
        this.persistenceService.salvarMesa(mesa);
    }

    private Mesa encontrarMesaDaComanda(
            Comanda comanda
    ) {
        if (comanda == null) {
            return null;
        }

        for (Mesa mesa : listaDeMesas) {
            if (mesa.getComandas() != null
                    && mesa.getComandas().contains(comanda)) {
                return mesa;
            }
        }

        return null;
    }

    @FXML
    private void abrirDashboardMesas() {
        restaurarBarraSuperior();
        atualizarVisualDasMesas();

        painelConteudo.setCenter(
                this.centroOriginalMesas
        );

        selecionarMenu(
                botaoMesas,
                "Mesas"
        );
    }

    @FXML
    public void abrirPainelInicial() {
        try {
            restaurarBarraSuperior();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/App/PainelInicialView.fxml")
            );
            Node painelInicial = loader.load();

            PainelInicialController controller = loader.getController();
            controller.inicializar(
                    this.listaDeMesas,
                    this.comandasSemMesa,
                    this.persistenceService,
                    this.usuarioLogado,
                    this
            );

            painelConteudo.setCenter(painelInicial);
            selecionarMenu(botaoPainelInicial, "Painel inicial");
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar o painel inicial.");
        }
    }

    @FXML
    private void abrirProdutos() {
        try {
            restaurarBarraSuperior();

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/ProdutosView.fxml"
                            )
                    );

            Node painelProdutos =
                    loader.load();

            ProdutosController controller =
                    loader.getController();

            this.listaDeProdutos =
                    persistenceService.carregarProdutos();

            controller.inicializar(
                    this.listaDeProdutos,
                    this.persistenceService
            );

            painelConteudo.setCenter(
                    painelProdutos
            );

            selecionarMenu(
                    botaoProdutos,
                    "Cardápio"
            );

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível carregar a tela de produtos."
            );
        }
    }

    @FXML
    private void abrirHistoricoVendas() {
        try {
            restaurarBarraSuperior();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/App/HistoricoVendasView.fxml")
            );
            Node painelHistorico = loader.load();

            HistoricoVendasController controller = loader.getController();
            controller.inicializar(this.persistenceService);

            painelConteudo.setCenter(painelHistorico);
            selecionarMenu(botaoHistorico, "Histórico de vendas");
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível carregar o histórico de vendas.");
        }
    }

    @FXML
    public void abrirEstoque() {
        if (usuarioLogado == null || !usuarioLogado.AcessoEstoque()) {
            mostrarAlerta("Acesso restrito", "Seu usuário não possui permissão para movimentar o estoque.");
            return;
        }

        try {
            restaurarBarraSuperior();

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/EstoqueView.fxml"
                            )
                    );

            Node painelEstoque =
                    loader.load();

            EstoqueController controller =
                    loader.getController();

            controller.inicializar(
                    this.persistenceService
            );

            painelConteudo.setCenter(
                    painelEstoque
            );

            selecionarMenu(
                    botaoEstoque,
                    "Estoque"
            );

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível carregar a tela de estoque."
            );
        }
    }

    @FXML
    public void abrirListaComandas() {
        try {
            restaurarBarraSuperior();
            exibirAcaoNovaComanda(true);

            if (this.listaDeProdutos == null) {
                this.listaDeProdutos =
                        persistenceService.carregarProdutos();
            }

            List<ItemVendavel> itensVendaveis =
                    new ArrayList<>(
                            this.listaDeProdutos
                    );

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/ListaComandas.fxml"
                            )
                    );

            Node painelComandas =
                    loader.load();

            ListaComandasController controller =
                    loader.getController();

            controller.inicializar(
                    this.listaDeMesas,
                    this.usuarioLogado,
                    this.comandasSemMesa,
                    itensVendaveis,
                    this
            );

            painelConteudo.setCenter(
                    painelComandas
            );

            selecionarMenu(
                    botaoComandas,
                    "Comandas"
            );

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir a lista de comandas."
            );
        }
    }

    private void mostrarTelaCompleta(
            Node tela
    ) {
        painelConteudo.setTop(null);

        painelConteudo.setCenter(
                tela
        );
    }

    private void restaurarBarraSuperior() {
        painelConteudo.setTop(
                barraSuperiorOriginal
        );

        exibirAcaoNovaComanda(false);
    }

    private void exibirAcaoNovaComanda(boolean visivel) {
        botaoNovaComanda.setVisible(
                visivel
        );

        botaoNovaComanda.setManaged(
                visivel
        );
    }

    private void selecionarMenu(
            Button botaoSelecionado,
            String titulo
    ) {
        labelTituloPagina.setText(
                titulo
        );

        for (
                Button botao :
                List.of(
                        botaoPainelInicial,
                        botaoMesas,
                        botaoComandas,
                        botaoProdutos,
                        botaoEstoque,
                        botaoHistorico
                )
        ) {
            botao.getStyleClass().remove(
                    "menu-button-active"
            );
        }

        botaoSelecionado
                .getStyleClass()
                .add(
                        "menu-button-active"
                );
    }
}
