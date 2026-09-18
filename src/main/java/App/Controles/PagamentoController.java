package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Pagamento.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PagamentoController {

    @FXML
    private Label labelValorTotal;
    @FXML
    private ComboBox<String> comboMetodo;

    @FXML
    private VBox boxDinheiro;
    @FXML
    private VBox boxCartao;
    @FXML
    private VBox boxParcelas;

    @FXML
    private TextField campoValorRecebido;
    @FXML
    private TextField campoBandeira;
    @FXML
    private TextField campoParcelas;

    private double valorDaConta;
    private boolean pagamentoRealizado = false;
    private Mesa mesaSendoPaga;
    private Comanda comandaAlvo;
    private InterfacePersistencia persistenceService;

    public void inicializar(double v, Mesa mesa, Comanda comanda) {
        inicializar(v, mesa, comanda, null);
    }

    public void inicializar(double v, Mesa mesa, Comanda comanda, InterfacePersistencia service) {
        this.valorDaConta = comanda.calcularTotal();
        this.pagamentoRealizado = false;
        this.mesaSendoPaga = mesa;
        this.comandaAlvo = comanda;
        this.persistenceService = service;

        labelValorTotal.setText("R$ " + String.format("%.2f", this.valorDaConta));

        comboMetodo.getItems().addAll("Dinheiro", "Cartão de Débito", "Cartão de Crédito");
        comboMetodo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> atualizarCamposVisiveis(newVal)
        );
    }

    private void atualizarCamposVisiveis(String metodo) {
        boxDinheiro.setVisible(false);
        boxDinheiro.setManaged(false);
        boxCartao.setVisible(false);
        boxCartao.setManaged(false);
        boxParcelas.setVisible(false);
        boxParcelas.setManaged(false);

        if (metodo == null) return;

        switch (metodo) {
            case "Dinheiro":
                boxDinheiro.setVisible(true);
                boxDinheiro.setManaged(true);
                break;
            case "Cartão de Débito":
                boxCartao.setVisible(true);
                boxCartao.setManaged(true);
                break;
            case "Cartão de Crédito":
                boxCartao.setVisible(true);
                boxCartao.setManaged(true);
                boxParcelas.setVisible(true);
                boxParcelas.setManaged(true);
                break;
        }
    }

    @FXML
    private void confirmarPagamento() {
        String metodo = comboMetodo.getValue();
        if (metodo == null) {
            mostrarAlerta("Erro", "Selecione uma forma de pagamento.");
            return;
        }

        Pagavel pagamento = null;

        try {
            switch (metodo) {
                case "Dinheiro":
                    double recebido = Double.parseDouble(campoValorRecebido.getText().replace(",", "."));
                    pagamento = new PagamentoDinheiro(recebido, valorDaConta);
                    break;
                case "Cartão de Débito":
                    pagamento = new PagamentoCartaoDebito(valorDaConta, campoBandeira.getText());
                    break;
                case "Cartão de Crédito":
                    int parcelas = Integer.parseInt(campoParcelas.getText());
                    pagamento = new PagamentoCartaoCredito(valorDaConta, campoBandeira.getText(), parcelas);
                    break;
            }

            if (pagamento != null && pagamento.processar()) {

                String comprovante = ((Pagamento) pagamento).gerarComprovante();

                // Registra pagamento no banco de dados
                if (persistenceService != null && mesaSendoPaga != null) {
                    String clienteNome = comandaAlvo != null ? comandaAlvo.getClienteNome() : "Desconhecido";
                    String detalhes = comprovante.replace("\n", " | ");
                    persistenceService.registrarPagamento(
                            mesaSendoPaga.getNumMesa(),
                            clienteNome,
                            this.valorDaConta,
                            metodo,
                            detalhes
                    );
                }

                mostrarAlerta("Pagamento Registrado", comprovante);
                this.pagamentoRealizado = true;
                fecharJanela();

            } else {
                mostrarAlerta("Erro", "Dados inválidos. Verifique valores ou campos vazios.");
            }

        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Digite apenas números nos campos de valor e parcelas.");
        }
    }

    @FXML
    private void cancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) labelValorTotal.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(msg);
        alerta.showAndWait();
    }

    public boolean isPagamentoRealizado() {
        return pagamentoRealizado;
    }


    private void finalizarMesa() {
        if (this.mesaSendoPaga == null) return;

        if (this.comandaAlvo != null) {
            return;
        }

        this.mesaSendoPaga.getComandas().clear();
        this.mesaSendoPaga.setAguardandoPagamento(false);
        this.mesaSendoPaga.encerrarReservaAtual();
    }
    }