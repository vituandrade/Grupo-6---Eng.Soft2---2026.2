package App.Controles;

import App.Persistencia.InterfacePersistencia;
import App.Validacao.CadastroItemValidator;
import Model.Produtos.ItemCardapio;
import Model.Produtos.Produto;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProdutosController extends BaseController {

    private static final String TODAS_AS_CATEGORIAS = "Todas as categorias";

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroCategoria;
    @FXML private Button botaoNovoItem;
    @FXML private TableView<Produto> tabelaItens;
    @FXML private TableColumn<Produto, String> colunaNome;
    @FXML private TableColumn<Produto, String> colunaCategoria;
    @FXML private TableColumn<Produto, Double> colunaPreco;
    @FXML private TableColumn<Produto, String> colunaDisponibilidade;
    @FXML private Label labelMensagem;

    private InterfacePersistencia persistenceService;
    private List<Produto> listaProdutosCentral;
    private ObservableList<Produto> itens;
    private FilteredList<Produto> itensFiltrados;

    @FXML
    private void initialize() {
        tabelaItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colunaNome.setCellValueFactory(dado -> new SimpleStringProperty(dado.getValue().getNome()));
        colunaCategoria.setCellValueFactory(dado -> new SimpleStringProperty(dado.getValue().getCategoriaNome()));
        colunaPreco.setCellValueFactory(dado -> new SimpleDoubleProperty(dado.getValue().getPreco()).asObject());
        colunaDisponibilidade.setCellValueFactory(dado -> new SimpleStringProperty(
                dado.getValue().isDisponivel() ? "Disponível" : "Indisponível"
        ));

        colunaPreco.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Double preco, boolean vazio) {
                super.updateItem(preco, vazio);
                setText(vazio || preco == null
                        ? null
                        : String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", preco));
            }
        });

        colunaDisponibilidade.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(String disponibilidade, boolean vazio) {
                super.updateItem(disponibilidade, vazio);
                getStyleClass().removeAll("status-disponivel", "status-indisponivel");
                if (vazio || disponibilidade == null) {
                    setText(null);
                    return;
                }
                setText(disponibilidade);
                getStyleClass().add("Disponível".equals(disponibilidade)
                        ? "status-disponivel"
                        : "status-indisponivel");
            }
        });

        tabelaItens.setPlaceholder(new Label("Nenhum item cadastrado."));
    }

    public void inicializar(List<Produto> listaProdutosCentral, InterfacePersistencia service) {
        this.listaProdutosCentral = listaProdutosCentral;
        this.persistenceService = service;
        this.itens = FXCollections.observableArrayList(listaProdutosCentral);
        this.itensFiltrados = new FilteredList<>(itens, item -> true);

        SortedList<Produto> itensOrdenados = new SortedList<>(itensFiltrados);
        itensOrdenados.comparatorProperty().bind(tabelaItens.comparatorProperty());
        tabelaItens.setItems(itensOrdenados);

        atualizarCategorias();
        campoBusca.textProperty().addListener((obs, anterior, atual) -> aplicarFiltros());
        filtroCategoria.valueProperty().addListener((obs, anterior, atual) -> aplicarFiltros());
    }

    @FXML
    private void abrirCadastro() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/CadastrarItemCardapio.fxml"));
            Node formulario = loader.load();
            CadastrarItemCardapioController controller = loader.getController();

            ButtonType salvar = new ButtonType("SALVAR", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);

            Dialog<ButtonType> dialogo = new Dialog<>();
            dialogo.setTitle("Cadastrar item do cardápio");
            dialogo.setHeaderText("Cadastrar item do cardápio");
            dialogo.initStyle(StageStyle.TRANSPARENT);
            dialogo.initOwner(botaoNovoItem.getScene().getWindow());
            dialogo.getDialogPane().setContent(formulario);
            dialogo.getDialogPane().getButtonTypes().addAll(cancelar, salvar);
            dialogo.getDialogPane().getStylesheets().add(
                    getClass().getResource("/App/Cardapio.css").toExternalForm()
            );

            Button botaoSalvar = (Button) dialogo.getDialogPane().lookupButton(salvar);
            Button botaoCancelar = (Button) dialogo.getDialogPane().lookupButton(cancelar);
            botaoSalvar.getStyleClass().add("save-button");
            botaoCancelar.getStyleClass().add("cancel-button");
            botaoSalvar.addEventFilter(ActionEvent.ACTION, evento -> {
                CadastroItemValidator.Resultado resultado = controller.validar();
                if (!resultado.valido()) {
                    evento.consume();
                    return;
                }

                Produto novoItem = new ItemCardapio(
                        resultado.nome(),
                        resultado.categoria(),
                        resultado.descricao(),
                        resultado.preco(),
                        resultado.disponivel()
                );

                List<Produto> listaAtualizada = new ArrayList<>(listaProdutosCentral);
                listaAtualizada.add(novoItem);

                try {
                    persistenceService.salvarProdutos(listaAtualizada);
                    listaProdutosCentral.add(novoItem);
                    itens.add(novoItem);
                    atualizarCategorias();
                    aplicarFiltros();
                    labelMensagem.setText("Item cadastrado com sucesso.");
                } catch (RuntimeException erro) {
                    evento.consume();
                    controller.exibirFalhaDePersistencia(
                            "Não foi possível gravar o item. Tente novamente ou selecione Cancelar."
                    );
                }
            });

            dialogo.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("Erro", "Não foi possível abrir o cadastro de item.");
        }
    }

    private void atualizarCategorias() {
        String categoriaSelecionada = filtroCategoria.getValue();
        Set<String> categorias = new LinkedHashSet<>();
        categorias.add(TODAS_AS_CATEGORIAS);
        categorias.add("Lanche");
        categorias.add("Bebida");
        categorias.add("Prato");
        categorias.add("Sobremesa");
        itens.stream()
                .map(Produto::getCategoriaNome)
                .filter(categoria -> categoria != null && !categoria.isBlank())
                .forEach(categorias::add);

        filtroCategoria.getItems().setAll(categorias);
        filtroCategoria.setValue(categorias.contains(categoriaSelecionada)
                ? categoriaSelecionada
                : TODAS_AS_CATEGORIAS);
    }

    private void aplicarFiltros() {
        if (itensFiltrados == null) {
            return;
        }

        String termo = campoBusca.getText() == null ? "" : campoBusca.getText().trim().toLowerCase();
        String categoria = filtroCategoria.getValue();

        itensFiltrados.setPredicate(item -> {
            String nome = item.getNome() == null ? "" : item.getNome().toLowerCase();
            String descricao = item.getDescricao() == null ? "" : item.getDescricao().toLowerCase();
            String categoriaItem = item.getCategoriaNome() == null
                    ? ""
                    : item.getCategoriaNome().toLowerCase();
            boolean correspondeBusca = termo.isEmpty()
                    || nome.contains(termo)
                    || descricao.contains(termo)
                    || categoriaItem.contains(termo);
            boolean correspondeCategoria = categoria == null
                    || TODAS_AS_CATEGORIAS.equals(categoria)
                    || categoria.equals(item.getCategoriaNome());
            return correspondeBusca && correspondeCategoria;
        });
    }
}
