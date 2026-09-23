package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Atendimento.Pedido;
import Model.Produtos.Produto;
import Model.Usuarios.Usuario;
import Model.Produtos.ItemVendavel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComandaController extends BaseController {

    @FXML private Label labelTituloComanda;
    @FXML private TextField campoPesquisa;
    @FXML private Label labelTotal;
    @FXML private TreeView<Object> arvorePedidos;
    @FXML private TabPane tabPaneCategorias;
    @FXML private Label labelItemSelecionado;
    @FXML private Spinner<Integer> spinnerQuantidade;
    @FXML private TextField campoObservacao;
    @FXML private ListView<Pedido> listaCarrinho;

    private ObservableList<Pedido> observableCarrinho;

    private Mesa mesa;
    private Comanda comanda;
    private Usuario atendente;
    private List<ItemVendavel> itensDisponiveis;
    private Produto produtoSelecionado;
    private MesaController navegador;

    public void setNavegador(MesaController navegador) {
        this.navegador = navegador;
    }

    public void carregarComanda(
            Mesa mesa,
            Comanda comanda,
            Usuario atendente,
            List<ItemVendavel> itens
    ) {
        this.mesa = mesa;
        this.comanda = comanda;
        this.atendente = atendente;
        this.itensDisponiveis = itens;
        this.produtoSelecionado = null;

        atualizarTituloComanda();

        campoPesquisa.setText("");

        campoPesquisa.textProperty().addListener(
                (obs, oldValue, newValue) ->
                        construirAbasDeProdutos(newValue)
        );

        this.observableCarrinho =
                FXCollections.observableArrayList();

        this.listaCarrinho.setItems(
                this.observableCarrinho
        );

        configurarArvorePedidos();
        configurarSpinner();
        construirAbasDeProdutos("");
        atualizarArvoreDePedidos();
        atualizarTotal();
    }

    private void atualizarTituloComanda() {
        String texto =
                comanda == null
                        ? "Comanda"
                        : comanda.toString();

        String numeroComanda =
                texto.replaceAll(
                        ".*?Comanda\\s+#?(\\d+).*",
                        "$1"
                );

        if (numeroComanda.equals(texto)) {
            numeroComanda = texto;
        }

        if (mesa != null) {
            labelTituloComanda.setText(
                    "Comanda #" +
                    numeroComanda +
                    " • Mesa " +
                    String.format(
                            "%02d",
                            mesa.getNumMesa()
                    )
            );
        } else {
            labelTituloComanda.setText(
                    "Comanda #" +
                    numeroComanda +
                    " • Cliente sem mesa"
            );
        }
    }

    private void configurarSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valorFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        999,
                        1
                );

        spinnerQuantidade.setValueFactory(
                valorFactory
        );
    }

    private void configurarArvorePedidos() {
        arvorePedidos.setCellFactory(
                tree -> new TreeCell<Object>() {

                    @Override
                    protected void updateItem(
                            Object item,
                            boolean empty
                    ) {
                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle(
                                    "-fx-background-color: white;"
                            );
                            return;
                        }

                        if (item instanceof Pedido) {
                            Pedido pedido =
                                    (Pedido) item;

                            HBox linha =
                                    new HBox(8);

                            linha.setAlignment(
                                    Pos.CENTER_LEFT
                            );

                            Label nome =
                                    new Label(
                                            pedido.getItem().getNome()
                                    );

                            nome.setStyle(
                                    "-fx-font-size: 11px;" +
                                    "-fx-text-fill: #34434A;"
                            );

                            Label quantidade =
                                    new Label(
                                            String.valueOf(
                                                    pedido.getQuantidade()
                                            )
                                    );

                            quantidade.setPrefWidth(55);

                            quantidade.setAlignment(
                                    Pos.CENTER
                            );

                            quantidade.setStyle(
                                    "-fx-font-size: 11px;" +
                                    "-fx-text-fill: #526069;"
                            );

                            Label unitario =
                                    new Label(
                                            String.format(
                                                    "R$ %.2f",
                                                    pedido.getItem().getPreco()
                                            )
                                    );

                            unitario.setPrefWidth(75);

                            unitario.setAlignment(
                                    Pos.CENTER_RIGHT
                            );

                            unitario.setStyle(
                                    "-fx-font-size: 11px;" +
                                    "-fx-text-fill: #526069;"
                            );

                            Label subtotal =
                                    new Label(
                                            String.format(
                                                    "R$ %.2f",
                                                    pedido.getSubtotal()
                                            )
                                    );

                            subtotal.setPrefWidth(75);

                            subtotal.setAlignment(
                                    Pos.CENTER_RIGHT
                            );

                            subtotal.setStyle(
                                    "-fx-font-size: 11px;" +
                                    "-fx-text-fill: #27343A;" +
                                    "-fx-font-weight: bold;"
                            );

                            Region espaco =
                                    new Region();

                            HBox.setHgrow(
                                    espaco,
                                    Priority.ALWAYS
                            );

                            linha.getChildren().addAll(
                                    nome,
                                    espaco,
                                    quantidade,
                                    unitario,
                                    subtotal
                            );

                            setText(null);
                            setGraphic(linha);

                            setStyle(
                                    "-fx-background-color: white;"
                            );

                        } else {
                            Label grupo =
                                    new Label(
                                            String.valueOf(item)
                                    );

                            grupo.setStyle(
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-text-fill: #27343A;"
                            );

                            setText(null);
                            setGraphic(grupo);

                            setStyle(
                                    "-fx-background-color: #F5F7F8;"
                            );
                        }
                    }
                }
        );
    }

    private void atualizarArvoreDePedidos() {
        TreeItem<Object> root =
                new TreeItem<>("Raiz");

        root.setExpanded(true);

        Map<Integer, TreeItem<Object>> gruposVisuais =
                new HashMap<>();

        for (Pedido p : this.comanda.getPedidos()) {

            int lote =
                    p.getNumeroLote();

            if (!gruposVisuais.containsKey(lote)) {

                String nomeGrupo =
                        lote == 0
                                ? "Pedidos registrados"
                                : "Pedido #" + lote;

                TreeItem<Object> grupoItem =
                        new TreeItem<>(
                                nomeGrupo
                        );

                grupoItem.setExpanded(true);

                gruposVisuais.put(
                        lote,
                        grupoItem
                );

                root.getChildren().add(
                        grupoItem
                );
            }

            TreeItem<Object> itemFolha =
                    new TreeItem<>(p);

            gruposVisuais
                    .get(lote)
                    .getChildren()
                    .add(itemFolha);
        }

        arvorePedidos.setRoot(root);
        arvorePedidos.setShowRoot(false);
    }

    @FXML
    private void confirmarPedidoCompleto() {

        if (observableCarrinho.isEmpty()) {
            mostrarAlerta(
                    "Vazio",
                    "Adicione itens à lista provisória antes de enviar."
            );
            return;
        }

        List<Pedido> pedidosConfirmados =
                new ArrayList<>();

        StringBuilder erros =
                new StringBuilder();

        int novoLote =
                this.comanda.gerarNovoNumeroLote();

        for (Pedido pedido :
                observableCarrinho) {

            ItemVendavel item =
                    pedido.getItem();

            if (item instanceof Produto) {

                Produto p =
                        (Produto) item;

                if (p.getEstoque() >=
                        pedido.getQuantidade()) {

                    p.setEstoque(
                            p.getEstoque()
                                    - pedido.getQuantidade()
                    );

                    pedido.setNumeroLote(
                            novoLote
                    );

                    pedidosConfirmados.add(
                            pedido
                    );

                } else {

                    erros.append(
                            "- "
                    ).append(
                            p.getNome()
                    ).append(
                            ": Estoque insuficiente.\n"
                    );
                }

            } else {

                pedido.setNumeroLote(
                        novoLote
                );

                pedidosConfirmados.add(
                        pedido
                );
            }
        }

        for (Pedido p :
                pedidosConfirmados) {

            this.comanda.adicionarPedido(
                    p
            );
        }

        this.observableCarrinho.removeAll(
                pedidosConfirmados
        );

        atualizarArvoreDePedidos();
        atualizarTotal();

        if (erros.length() > 0) {
            mostrarAlerta(
                    "Alguns itens não foram adicionados",
                    erros.toString()
            );
        }
    }

    @FXML
    private void removerPedido() {

        TreeItem<Object> itemSelecionado =
                arvorePedidos
                        .getSelectionModel()
                        .getSelectedItem();

        if (itemSelecionado == null) {
            mostrarAlerta(
                    "Erro",
                    "Selecione um item na lista para remover."
            );
            return;
        }

        Object valor =
                itemSelecionado.getValue();

        if (!(valor instanceof Pedido)) {
            mostrarAlerta(
                    "Erro",
                    "Selecione um item do pedido."
            );
            return;
        }

        Pedido pedido =
                (Pedido) valor;

        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/CancelarItemView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            CancelarItemController controller =
                    loader.getController();

            controller.inicializar(
                    pedido,
                    this.comanda
            );

            Stage stage =
                    new Stage();

            stage.setTitle(
                    "Cancelar item"
            );

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            stage.setResizable(false);

            stage.setScene(
                    new Scene(
                            root,
                            620,
                            320
                    )
            );

            stage.showAndWait();

            if (!controller.isConfirmado()) {
                return;
            }

            ItemVendavel itemCancelado =
                    pedido.getItem();

            if (itemCancelado instanceof Produto) {

                Produto produtoCancelado =
                        (Produto) itemCancelado;

                produtoCancelado.setEstoque(
                        produtoCancelado.getEstoque()
                                + pedido.getQuantidade()
                );
            }

            this.comanda.getPedidos().remove(
                    pedido
            );

            atualizarArvoreDePedidos();
            atualizarTotal();

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir a tela de cancelamento."
            );
        }
    }

    @FXML
    private void adicionarAoCarrinho() {

        if (this.produtoSelecionado == null) {
            mostrarAlerta(
                    "Erro",
                    "Nenhum produto selecionado."
            );
            return;
        }

        int qtdParaAdicionar =
                spinnerQuantidade.getValue();

        String obsAtual =
                campoObservacao.getText() == null
                        ? ""
                        : campoObservacao
                                .getText()
                                .trim();

        Pedido pedidoExistente =
                null;

        for (Pedido itemCarrinho :
                observableCarrinho) {

            boolean mesmoNome =
                    itemCarrinho
                            .getItem()
                            .getNome()
                            .equals(
                                    this.produtoSelecionado
                                            .getNome()
                            );

            String obsItem =
                    itemCarrinho.getObservacao() == null
                            ? ""
                            : itemCarrinho
                                    .getObservacao()
                                    .trim();

            boolean mesmaObs =
                    obsItem.equalsIgnoreCase(
                            obsAtual
                    );

            if (mesmoNome && mesmaObs) {
                pedidoExistente =
                        itemCarrinho;
                break;
            }
        }

        if (pedidoExistente != null) {

            int qtdTotal =
                    pedidoExistente.getQuantidade()
                            + qtdParaAdicionar;

            if (this.produtoSelecionado.getEstoque()
                    < qtdTotal) {

                mostrarAlerta(
                        "Estoque",
                        "Estoque insuficiente. Total no carrinho seria: "
                                + qtdTotal
                );
                return;
            }

            pedidoExistente.setQuantidade(
                    qtdTotal
            );

            listaCarrinho.refresh();

        } else {

            if (this.produtoSelecionado.getEstoque()
                    < qtdParaAdicionar) {

                mostrarAlerta(
                        "Estoque",
                        "Estoque insuficiente."
                );
                return;
            }

            Pedido novoPedido =
                    new Pedido(
                            this.produtoSelecionado,
                            qtdParaAdicionar,
                            obsAtual,
                            this.atendente
                    );

            observableCarrinho.add(
                    novoPedido
            );

            listaCarrinho.scrollTo(
                    novoPedido
            );
        }

        spinnerQuantidade
                .getValueFactory()
                .setValue(1);
    }

    @FXML
    private void removerDoCarrinho() {

        Pedido selecionado =
                listaCarrinho
                        .getSelectionModel()
                        .getSelectedItem();

        if (selecionado != null) {
            observableCarrinho.remove(
                    selecionado
            );
        }
    }

    private void atualizarTotal() {
        labelTotal.setText(
                String.format(
                        "Total: R$ %.2f",
                        comanda.calcularTotal()
                )
        );
    }

    private void construirAbasDeProdutos(
            String termoPesquisa
    ) {

        tabPaneCategorias
                .getTabs()
                .clear();

        if (!termoPesquisa.isEmpty()) {
            this.produtoSelecionado = null;

            labelItemSelecionado.setText(
                    "Selecione um item..."
            );
        }

        this.produtoSelecionado = null;

        Map<String, List<Produto>>
                produtosPorCategoria =
                new HashMap<>();

        String termo =
                termoPesquisa
                        .toLowerCase()
                        .trim();

        for (ItemVendavel item :
                this.itensDisponiveis) {

            if (item instanceof Produto) {

                Produto p =
                        (Produto) item;

                if (!p.isDisponivel()) {
                    continue;
                }

                if (!termo.isEmpty()
                        && !p.getNome()
                        .toLowerCase()
                        .startsWith(termo)) {
                    continue;
                }

                String nomeCat =
                        p.getCategoriaNome();

                if (nomeCat == null
                        || nomeCat.trim().isEmpty()) {
                    continue;
                }

                produtosPorCategoria
                        .computeIfAbsent(
                                nomeCat,
                                k -> new ArrayList<>()
                        )
                        .add(p);
            }
        }

        for (
                Map.Entry<
                        String,
                        List<Produto>
                        > entry :
                produtosPorCategoria
                        .entrySet()
        ) {

            String nomeCategoria =
                    entry.getKey();

            List<Produto>
                    produtosDaAba =
                    entry.getValue();

            Tab tab =
                    new Tab(
                            nomeCategoria
                    );

            tab.setClosable(false);

            TilePane grid =
                    new TilePane();

            grid.setPadding(
                    new Insets(8)
            );

            grid.setHgap(6);
            grid.setVgap(6);

            for (
                    Produto p :
                    produtosDaAba
            ) {

                Button btnProduto =
                        new Button(
                                p.getNome()
                        );

                btnProduto.setPrefSize(
                        88,
                        55
                );

                btnProduto.setWrapText(
                        true
                );

                btnProduto.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-border-color: #D5DEE2;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-size: 10px;" +
                        "-fx-text-fill: #34434A;"
                );

                btnProduto.setOnAction(
                        e -> {

                            if (
                                    this.produtoSelecionado != null
                                    && this.produtoSelecionado
                                    .getNome()
                                    .equals(
                                            p.getNome()
                                    )
                            ) {

                                spinnerQuantidade
                                        .increment(1);

                            } else {

                                this.produtoSelecionado =
                                        p;

                                spinnerQuantidade
                                        .getValueFactory()
                                        .setValue(1);

                                campoObservacao.clear();
                            }

                            labelItemSelecionado
                                    .setText(
                                            p.getNome()
                                    );
                        }
                );

                grid.getChildren().add(
                        btnProduto
                );
            }

            tab.setContent(
                    grid
            );

            tabPaneCategorias
                    .getTabs()
                    .add(
                            tab
                    );
        }
    }

    @FXML
    private void voltar() {
        if (navegador != null) {
            navegador.abrirListaComandas();
        }
    }
}