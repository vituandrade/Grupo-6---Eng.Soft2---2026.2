package App.Controles;

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

public class GerenciarMesaController extends BaseController {

    @FXML private Label labelTituloMesa;
    @FXML private ListView<Comanda> listaComandas;

    @FXML private Button botaoAbrirComanda;
    @FXML private HBox boxBotoesFechada;

    private Mesa mesa;
    private Usuario atendente;
    private List<ItemVendavel> produtosDisponiveis;
    private List<Comanda> comandasSemMesa;

    public void inicializar(
        Mesa mesa,
        Usuario atendente,
        List<Comanda> comandasSemMesa,
        List<ItemVendavel> itens
) {

    this.mesa = mesa;
    this.atendente = atendente;
    this.produtosDisponiveis = itens;
    this.comandasSemMesa = comandasSemMesa;

    labelTituloMesa.setText(
            "Gerenciando Mesa " + mesa.getNumMesa()
    );

    atualizarListaComandas();

    this.listaComandas.getSelectionModel()
            .selectedItemProperty()
            .addListener(
                    (observable, oldValue, newValue) ->
                            atualizarVisibilidadeBotoes(newValue)
            );

    this.listaComandas.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2) {
            abrirComanda();
        }
    });

    atualizarVisibilidadeBotoes(null);
}

    private void atualizarVisibilidadeBotoes(Comanda selecionada) {

        boolean mesaLivre =
                mesa != null
                && !mesa.isOcupada()
                && !mesa.temComandaAberta();

        botaoAbrirComanda.setVisible(mesaLivre);
        botaoAbrirComanda.setManaged(mesaLivre);

        boolean comandaFechada =
                selecionada != null
                && selecionada.isFechada();

        boxBotoesFechada.setVisible(comandaFechada);
        boxBotoesFechada.setManaged(comandaFechada);
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
            pgtoController.inicializar(selecionada);

            Stage stage = new Stage();
            stage.setTitle("Pagamento Individual");
            stage.setScene(new Scene(root, 800, 700));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            if (pgtoController.isPagamentoRealizado()) {
                this.mesa.getComandas().remove(selecionada);
                atualizarListaComandas();
                if (this.mesa.getComandas().isEmpty()) {
                    System.out.println("Mesa vazia! Liberando...");
                    this.mesa.setAguardandoPagamento(false);
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

        if (mesa.isOcupada() || mesa.temComandaAberta()) {

            mostrarAlerta(
                    "Mesa ocupada",
                    "Esta mesa já possui uma comanda aberta."
            );

            atualizarVisibilidadeBotoes(null);
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/App/AbrirComandaView.fxml"
                            )
                    );

            Parent root = loader.load();

            AbrirComandaController dialogController =
                    loader.getController();

            dialogController.inicializar(
                    List.of(mesa),
                    mesa
            );

            Stage stage = new Stage();

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );

            stage.setTitle("Abrir comanda");

            stage.setScene(
                    new Scene(root)
            );

            stage.setResizable(false);

            stage.showAndWait();

            if (dialogController.isConfirmada() && dialogController.getComandaCriada() != null) {

                Comanda comandaCriada =
                        dialogController.getComandaCriada();

                Mesa mesaDaComanda =
                        dialogController.getMesaSelecionada();

                // Cliente sem mesa
                if (mesaDaComanda == null) {

                    if (comandasSemMesa != null) {
                        comandasSemMesa.add(comandaCriada);
                    }

                } else {

                    atualizarListaComandas();

                    listaComandas
                            .getSelectionModel()
                            .select(comandaCriada);
                }

                abrirComanda();
            }
        }catch (IOException e) {

            e.printStackTrace();

            mostrarAlerta(
                    "Erro",
                    "Não foi possível abrir o diálogo de nova comanda."
            );
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



    @Override
    protected void mostrarAlerta(String titulo, String msg) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(msg);
        alerta.showAndWait();
    }
}