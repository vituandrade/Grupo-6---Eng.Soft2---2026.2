package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Atendimento.Venda;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.List;

/** Tela UC08 / RF07: consulta das vendas já fechadas. */
public class HistoricoVendasController extends BaseController {

    private static final DateTimeFormatter DATA_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField campoDataInicial;
    @FXML private TextField campoDataFinal;
    @FXML private ComboBox<String> comboFormaPagamento;
    @FXML private Button botaoFiltrar;
    @FXML private Label labelMensagem;
    @FXML private TableView<Venda> tabelaVendas;
    @FXML private TableColumn<Venda, Integer> colunaComanda;
    @FXML private TableColumn<Venda, LocalDateTime> colunaDataHora;
    @FXML private TableColumn<Venda, String> colunaAtendimento;
    @FXML private TableColumn<Venda, String> colunaPagamento;
    @FXML private TableColumn<Venda, Double> colunaTotal;
    @FXML private TableColumn<Venda, String> colunaFuncionario;

    private final ObservableList<Venda> vendas = FXCollections.observableArrayList();
    private InterfacePersistencia persistenceService;

    @FXML
    private void initialize() {
        configurarTabela();
        comboFormaPagamento.getItems().setAll(
                "Todas",
                "Dinheiro",
                "Cartão de Débito",
                "Cartão de Crédito",
                "Pix presencial"
        );
        comboFormaPagamento.setValue("Todas");
    }

    public void inicializar(InterfacePersistencia service) {
        this.persistenceService = service;
        carregarVendas(null, null, "Todas");
    }

    @FXML
    private void filtrar() {
        labelMensagem.setText("");

        LocalDate dataInicial;
        LocalDate dataFinal;

        try {
            dataInicial = lerData(campoDataInicial.getText());
            dataFinal = lerData(campoDataFinal.getText());
        } catch (DateTimeParseException e) {
            labelMensagem.setText("Informe as datas no formato DD/MM/AAAA.");
            return;
        }

        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            labelMensagem.setText("Data inicial não pode ser posterior à data final.");
            return;
        }

        carregarVendas(dataInicial, dataFinal, comboFormaPagamento.getValue());
    }

    private LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return LocalDate.parse(texto.trim(), DATA_INPUT);
    }

    private void carregarVendas(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String formaPagamento
    ) {
        try {
            List<Venda> resultado = persistenceService.carregarHistoricoVendas(
                    dataInicial,
                    dataFinal,
                    formaPagamento
            );
            vendas.setAll(resultado);
            tabelaVendas.setItems(vendas);
            if (resultado.isEmpty()) {
                labelMensagem.setText("Nenhuma venda encontrada.");
            } else {
                labelMensagem.setText("");
            }
        } catch (RuntimeException e) {
            labelMensagem.setText("Não foi possível consultar o histórico de vendas.");
        }
    }

    private void configurarTabela() {
        colunaComanda.setCellValueFactory(new PropertyValueFactory<>("comandaId"));
        colunaDataHora.setCellValueFactory(new PropertyValueFactory<>("dataHora"));
        colunaAtendimento.setCellValueFactory(new PropertyValueFactory<>("atendimento"));
        colunaPagamento.setCellValueFactory(new PropertyValueFactory<>("formaPagamento"));
        colunaTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colunaFuncionario.setCellValueFactory(new PropertyValueFactory<>("funcionario"));

        colunaDataHora.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATA_HORA.format(item));
            }
        });

        colunaTotal.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", item));
            }
        });

        tabelaVendas.setPlaceholder(new Label("Nenhuma venda encontrada."));
    }

}
