package App.Controles;

import App.Persistencia.InterfacePersistencia;
import App.Persistencia.SaldoInsuficienteException;
import App.Validacao.MovimentacaoEstoqueValidator;
import Model.Estoque.ItemEstoque;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class EstoqueController extends BaseController {

    private static final String TODAS_AS_SITUACOES = "Todas as situações";

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroSituacao;
    @FXML private Button botaoMovimentacao;
    @FXML private TableView<ItemEstoque> tabelaEstoque;
    @FXML private TableColumn<ItemEstoque, String> colunaItem;
    @FXML private TableColumn<ItemEstoque, String> colunaUnidade;
    @FXML private TableColumn<ItemEstoque, Double> colunaQuantidade;
    @FXML private TableColumn<ItemEstoque, String> colunaSituacao;
    @FXML private Label labelMensagem;

    private InterfacePersistencia persistenceService;
    private ObservableList<ItemEstoque> itens;
    private FilteredList<ItemEstoque> itensFiltrados;

    @FXML
    private void initialize() {
        tabelaEstoque.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colunaItem.setCellValueFactory(dado -> new SimpleStringProperty(dado.getValue().getNome()));
        colunaUnidade.setCellValueFactory(dado -> new SimpleStringProperty(dado.getValue().getUnidadeMedida()));
        colunaQuantidade.setCellValueFactory(dado -> new SimpleDoubleProperty(dado.getValue().getQuantidade()).asObject());
        colunaSituacao.setCellValueFactory(dado -> new SimpleStringProperty(dado.getValue().getSituacao()));

        DecimalFormat formato = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        colunaQuantidade.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Double quantidade, boolean vazio) {
                super.updateItem(quantidade, vazio);
                setText(vazio || quantidade == null ? null : formato.format(quantidade));
            }
        });

        colunaSituacao.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(String situacao, boolean vazio) {
                super.updateItem(situacao, vazio);
                getStyleClass().removeAll("status-normal", "status-baixo", "status-vazio");
                if (vazio || situacao == null) {
                    setText(null);
                    return;
                }
                setText(situacao);
                getStyleClass().add(switch (situacao) {
                    case "Normal" -> "status-normal";
                    case "Estoque baixo" -> "status-baixo";
                    default -> "status-vazio";
                });
            }
        });

        filtroSituacao.getItems().setAll(
                TODAS_AS_SITUACOES, "Normal", "Estoque baixo", "Sem estoque"
        );
        filtroSituacao.setValue(TODAS_AS_SITUACOES);
        tabelaEstoque.setPlaceholder(new Label("Nenhum item no estoque."));
    }

    public void inicializar(InterfacePersistencia service) {
        this.persistenceService = service;
        this.itens = FXCollections.observableArrayList(service.carregarItensEstoque());
        this.itensFiltrados = new FilteredList<>(itens, item -> true);

        SortedList<ItemEstoque> ordenados = new SortedList<>(itensFiltrados);
        ordenados.comparatorProperty().bind(tabelaEstoque.comparatorProperty());
        tabelaEstoque.setItems(ordenados);

        campoBusca.textProperty().addListener((obs, anterior, atual) -> aplicarFiltros());
        filtroSituacao.valueProperty().addListener((obs, anterior, atual) -> aplicarFiltros());
    }

    @FXML
    private void abrirMovimentacao() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/RegistrarMovimentacao.fxml"));
            Node formulario = loader.load();
            RegistrarMovimentacaoController controller = loader.getController();
            controller.inicializar(List.copyOf(itens));

            ButtonType registrar = new ButtonType("REGISTRAR", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelar = new ButtonType("CANCELAR", ButtonBar.ButtonData.CANCEL_CLOSE);
            Dialog<ButtonType> dialogo = new Dialog<>();
            dialogo.setTitle("Registrar movimentação");
            dialogo.setHeaderText("Registrar movimentação");
            dialogo.initStyle(StageStyle.TRANSPARENT);
            dialogo.initOwner(botaoMovimentacao.getScene().getWindow());
            dialogo.getDialogPane().setContent(formulario);
            dialogo.getDialogPane().getButtonTypes().addAll(cancelar, registrar);
            dialogo.getDialogPane().getStylesheets().add(
                    getClass().getResource("/App/Estoque.css").toExternalForm()
            );

            Button botaoRegistrar = (Button) dialogo.getDialogPane().lookupButton(registrar);
            Button botaoCancelar = (Button) dialogo.getDialogPane().lookupButton(cancelar);
            botaoRegistrar.getStyleClass().add("register-button");
            botaoCancelar.getStyleClass().add("cancel-button");
            botaoRegistrar.addEventFilter(ActionEvent.ACTION, evento -> {
                MovimentacaoEstoqueValidator.Resultado resultado = controller.validar();
                if (!resultado.valido()) {
                    evento.consume();
                    return;
                }

                try {
                    persistenceService.registrarMovimentacaoEstoque(
                            controller.getItemIdSelecionado(),
                            resultado.nomeItem(),
                            resultado.unidadeMedida(),
                            resultado.tipo(),
                            resultado.quantidade()
                    );
                    recarregarItens();
                    labelMensagem.setText("Movimentação registrada com sucesso.");
                } catch (SaldoInsuficienteException erro) {
                    evento.consume();
                    controller.exibirErro(erro.getMessage());
                } catch (RuntimeException erro) {
                    evento.consume();
                    controller.exibirErro("Não foi possível registrar a movimentação. Tente novamente.");
                }
            });

            dialogo.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("Erro", "Não foi possível abrir o registro de movimentação.");
        }
    }

    private void recarregarItens() {
        itens.setAll(persistenceService.carregarItensEstoque());
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        if (itensFiltrados == null) {
            return;
        }
        String termo = campoBusca.getText() == null ? "" : campoBusca.getText().trim().toLowerCase();
        String situacao = filtroSituacao.getValue();
        itensFiltrados.setPredicate(item -> {
            boolean correspondeBusca = termo.isEmpty()
                    || item.getNome().toLowerCase().contains(termo)
                    || item.getUnidadeMedida().toLowerCase().contains(termo);
            boolean correspondeSituacao = situacao == null
                    || TODAS_AS_SITUACOES.equals(situacao)
                    || situacao.equals(item.getSituacao());
            return correspondeBusca && correspondeSituacao;
        });
    }
}
