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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

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

    public void carregarComanda(Mesa mesa, Comanda comanda, Usuario atendente, List<ItemVendavel> itens) {
        this.mesa = mesa;
        this.comanda = comanda;
        this.atendente = atendente;
        this.itensDisponiveis = itens;
        this.produtoSelecionado = null;

        labelTituloComanda.setText(comanda.toString());
        campoPesquisa.setText("");
        campoPesquisa.textProperty().addListener((obs, oldValue, newValue) -> {
            construirAbasDeProdutos(newValue);
        });

        this.observableCarrinho = FXCollections.observableArrayList();
        this.listaCarrinho.setItems(this.observableCarrinho);

        construirAbasDeProdutos("");

        atualizarArvoreDePedidos();

        atualizarTotal();
    }

    private void atualizarArvoreDePedidos() {
        TreeItem<Object> root = new TreeItem<>("Raiz");
        root.setExpanded(true);

        Map<Integer, TreeItem<Object>> gruposVisuais = new HashMap<>();

        for (Pedido p : this.comanda.getPedidos()) {
            int lote = p.getNumeroLote();

            if (lote == 0) lote = 0;

            if (!gruposVisuais.containsKey(lote)) {
                String nomeGrupo = (lote == 0) ? "Pedidos Antigos" : "Pedido #" + lote;
                TreeItem<Object> grupoItem = new TreeItem<>(nomeGrupo);
                grupoItem.setExpanded(true);
                gruposVisuais.put(lote, grupoItem);
                root.getChildren().add(grupoItem);
            }

            TreeItem<Object> itemFolha = new TreeItem<>(p);
            gruposVisuais.get(lote).getChildren().add(itemFolha);
        }

        arvorePedidos.setRoot(root);
        arvorePedidos.setShowRoot(false);
    }

    @FXML
    private void confirmarPedidoCompleto() {
        if (observableCarrinho.isEmpty()) {
            mostrarAlerta("Vazio", "Adicione itens à lista provisória antes de enviar.");
            return;
        }

        List<Pedido> pedidosConfirmados = new ArrayList<>();
        StringBuilder erros = new StringBuilder();

        int novoLote = this.comanda.gerarNovoNumeroLote();

        for (Pedido pedido : observableCarrinho) {
            ItemVendavel item = pedido.getItem();

            if (item instanceof Produto) {
                Produto p = (Produto) item;
                if (p.getEstoque() >= pedido.getQuantidade()) {
                    p.setEstoque(p.getEstoque() - pedido.getQuantidade());

                    pedido.setNumeroLote(novoLote);

                    pedidosConfirmados.add(pedido);
                } else {
                    erros.append("- ").append(p.getNome()).append(": Estoque insuficiente.\n");
                }
            } else {
                pedido.setNumeroLote(novoLote);
                pedidosConfirmados.add(pedido);
            }
        }

        for (Pedido p : pedidosConfirmados) {
            this.comanda.adicionarPedido(p);
        }

        this.observableCarrinho.removeAll(pedidosConfirmados);

        atualizarArvoreDePedidos();
        atualizarTotal();

        if (erros.length() > 0) {
            mostrarAlerta("Alguns itens não foram adicionados", erros.toString());
        } else {
            System.out.println("Pedido enviado com sucesso!");
        }
    }

    @FXML
    private void removerPedido() {

        TreeItem<Object> itemSelecionado = arvorePedidos.getSelectionModel().getSelectedItem();

        if (itemSelecionado == null) {
            mostrarAlerta("Erro", "Selecione um item na árvore para remover.");
            return;
        }

        Object valor = itemSelecionado.getValue();

        if (valor instanceof Pedido) {
            Pedido pedido = (Pedido) valor;

            ItemVendavel itemCancelado = pedido.getItem();
            if (itemCancelado instanceof Produto) {
                Produto produtoCancelado = (Produto) itemCancelado;
                produtoCancelado.setEstoque(produtoCancelado.getEstoque() + pedido.getQuantidade());
            }

            this.comanda.getPedidos().remove(pedido);

            atualizarArvoreDePedidos();
            atualizarTotal();
        } else {
            mostrarAlerta("Erro", "Selecione o item (produto), não o título do grupo.");
        }
    }

    @FXML
    private void adicionarAoCarrinho() {
        if (this.produtoSelecionado == null) {
            mostrarAlerta("Erro", "Nenhum produto selecionado.");
            return;
        }

        int qtdParaAdicionar = spinnerQuantidade.getValue();
        String obsAtual = (campoObservacao.getText() == null) ? "" : campoObservacao.getText().trim();

        Pedido pedidoExistente = null;

        for (Pedido itemCarrinho : observableCarrinho) {
            boolean mesmoNome = itemCarrinho.getItem().getNome().equals(this.produtoSelecionado.getNome());
            String obsItem = (itemCarrinho.getObservacao() == null) ? "" : itemCarrinho.getObservacao().trim();
            boolean mesmaObs = obsItem.equalsIgnoreCase(obsAtual);

            if (mesmoNome && mesmaObs) {
                pedidoExistente = itemCarrinho;
                break;
            }
        }

        if (pedidoExistente != null) {
            int qtdTotal = pedidoExistente.getQuantidade() + qtdParaAdicionar;
            if (this.produtoSelecionado.getEstoque() < qtdTotal) {
                mostrarAlerta("Estoque", "Estoque insuficiente. Total no carrinho seria: " + qtdTotal);
                return;
            }
            pedidoExistente.setQuantidade(qtdParaAdicionar);
            listaCarrinho.refresh();
        } else {
            if (this.produtoSelecionado.getEstoque() < qtdParaAdicionar) {
                mostrarAlerta("Estoque", "Estoque insuficiente.");
                return;
            }
            Pedido novoPedido = new Pedido(this.produtoSelecionado, qtdParaAdicionar, obsAtual, this.atendente);
            observableCarrinho.add(novoPedido);
            listaCarrinho.scrollTo(novoPedido);
        }

        spinnerQuantidade.getValueFactory().setValue(1);
    }

    @FXML
    private void removerDoCarrinho() {
        Pedido selecionado = listaCarrinho.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            observableCarrinho.remove(selecionado);
        }
    }

    @FXML
    private void fecharComanda() {
        comanda.fechar();
        mostrarAlerta("Comanda Fechada", "Comanda fechada com sucesso!\nTotal: R$ " + comanda.calcularTotal());
        Stage stage = (Stage) labelTotal.getScene().getWindow();
        stage.close();
    }

    private void atualizarTotal() {
        labelTotal.setText(String.format("Total: R$ %.2f", comanda.calcularTotal()));
    }

    private void construirAbasDeProdutos(String termoPesquisa) {
        tabPaneCategorias.getTabs().clear();

        if (!termoPesquisa.isEmpty()) {
            this.produtoSelecionado = null;
            labelItemSelecionado.setText("Selecione um item...");
        }
        this.produtoSelecionado = null;
        Map<String, List<Produto>> produtosPorCategoria = new HashMap<>();
        String termo = termoPesquisa.toLowerCase().trim();

        for (ItemVendavel item : this.itensDisponiveis) {
            if (item instanceof Produto) {
                Produto p = (Produto) item;

                if (!termo.isEmpty() && !p.getNome().toLowerCase().startsWith(termo)) {
                    continue;
                }

                String nomeCat = p.getCategoriaNome();
                if (nomeCat == null || nomeCat.trim().isEmpty()) {
                    continue;
                }

                produtosPorCategoria
                        .computeIfAbsent(nomeCat, k -> new ArrayList<>())
                        .add(p);
            }
        }

        for (Map.Entry<String, List<Produto>> entry : produtosPorCategoria.entrySet()) {
            String nomeCategoria = entry.getKey();
            List<Produto> produtosDaAba = entry.getValue();

            Tab tab = new Tab(nomeCategoria);
            tab.setClosable(false);

            TilePane grid = new TilePane();
            grid.setPadding(new Insets(10));
            grid.setHgap(8);
            grid.setVgap(8);

            for (Produto p : produtosDaAba) {
                Button btnProduto = new Button(p.getNome());
                btnProduto.setPrefSize(100, 80);
                btnProduto.setWrapText(true);

                btnProduto.setOnAction(e -> {
                    if (this.produtoSelecionado != null && this.produtoSelecionado.getNome().equals(p.getNome())) {
                        spinnerQuantidade.increment(1);
                    } else {
                        this.produtoSelecionado = p;
                        spinnerQuantidade.getValueFactory().setValue(1);
                        campoObservacao.clear();
                    }
                    labelItemSelecionado.setText("Selecionado: " + p.getNome());
                });

                grid.getChildren().add(btnProduto);
            }

            tab.setContent(grid);
            tabPaneCategorias.getTabs().add(tab);
        }
    }
}