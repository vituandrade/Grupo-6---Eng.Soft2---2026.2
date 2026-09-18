package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Produtos.ItemVendavel;
import Model.Usuarios.Usuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GerenciarMesaController extends BaseController {

    @FXML private Label labelTituloMesa;
    @FXML private ListView<Comanda> listaComandas;

    @FXML private Button botaoAbrirComanda;
    @FXML private HBox boxBotoesFechada;

    private Mesa mesa;
    private Usuario atendente;
    private List<ItemVendavel> produtosDisponiveis;
    private InterfacePersistencia persistenceService;

    public void inicializar(Mesa mesa, Usuario atendente, List<ItemVendavel> itens) {
        inicializar(mesa, atendente, itens, null);
    }

    public void inicializar(Mesa mesa, Usuario atendente, List<ItemVendavel> itens,
                            InterfacePersistencia service) {
        this.mesa = mesa;
        this.atendente = atendente;
        this.produtosDisponiveis = itens;
        this.persistenceService = service;
        labelTituloMesa.setText("Gerenciando Mesa " + mesa.getNumMesa());
        atualizarListaComandas();
        this.listaComandas.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> atualizarVisibilidadeBotoes(newValue)
        );
        atualizarVisibilidadeBotoes(null);
    }

    private void atualizarVisibilidadeBotoes(Comanda selecionada) {
        if (selecionada == null) {
            botaoAbrirComanda.setVisible(false);
            boxBotoesFechada.setVisible(false);
        } else if (selecionada.isFechada()) {
            botaoAbrirComanda.setVisible(false);
            boxBotoesFechada.setVisible(true);
        } else {
            botaoAbrirComanda.setVisible(true);
            boxBotoesFechada.setVisible(false);
        }
    }

    private void atualizarListaComandas() {
        listaComandas.getItems().clear();
        listaComandas.getItems().addAll(mesa.getComandas());
    }


    @FXML
    private void pagarComandaSelecionada() {
        Comanda selecionada = listaComandas.getSelectionModel().getSelectedItem();

        if (selecionada == null || !selecionada.isFechada()) {
            mostrarAlerta("Erro", "Selecione uma comanda FECHADA para pagar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/PagamentoView.fxml"));
            Parent root = loader.load();

            PagamentoController pgtoController = loader.getController();
            pgtoController.inicializar(selecionada.calcularTotal(), this.mesa, selecionada,
                    this.persistenceService);

            Stage stage = new Stage();
            stage.setTitle("Pagamento Individual");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            if (pgtoController.isPagamentoRealizado()) {
                this.mesa.getComandas().remove(selecionada);
                atualizarListaComandas();
                if (this.mesa.getComandas().isEmpty()) {
                    System.out.println("Mesa vazia! Liberando...");
                    this.mesa.setAguardandoPagamento(false);
                    this.mesa.encerrarReservaAtual();
                    ((Stage) labelTituloMesa.getScene().getWindow()).close();

                } else {
                    atualizarVisibilidadeBotoes(null);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro", "Não foi possível abrir o pagamento.");
        }
    }

    @FXML
    private void adicionarNovaComanda() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Comanda");
        dialog.setHeaderText("Mesa " + mesa.getNumMesa());
        dialog.setContentText("Nome do cliente:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()){
            Comanda novaComanda = new Comanda();
            novaComanda.setClienteNome(result.get());
            this.mesa.adicionarComanda(novaComanda);

            atualizarListaComandas();
            abrirComanda();

        }
    }

    @FXML
    private void abrirComanda(){
        Comanda selecionada = listaComandas.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/ComandaView.fxml"));
                Parent root = loader.load();
                ComandaController comandaController = loader.getController();
                comandaController.carregarComanda(this.mesa, selecionada, this.atendente, this.produtosDisponiveis);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Editando " + selecionada.toString());
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.showAndWait();

                atualizarListaComandas();

            } catch (IOException e) {
                e.printStackTrace();
                mostrarAlerta("Erro", "Não foi possível abrir a tela da comanda.");
            }
        }
    }


    @FXML
    private void reabrirComandaSelecionada() {
        Comanda selecionada = listaComandas.getSelectionModel().getSelectedItem();
        if (selecionada != null && selecionada.isFechada()) {
            selecionada.reabrir();
            atualizarListaComandas();
            atualizarVisibilidadeBotoes(selecionada);
            mostrarAlerta("Sucesso", "Comanda reaberta.");
        }
    }

    @Override
    protected void mostrarAlerta(String titulo, String msg) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(msg);
        alerta.showAndWait();
    }
}