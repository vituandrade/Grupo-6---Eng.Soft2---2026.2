    package App.Controles;

    import Model.Atendimento.Comanda;
    import Model.Atendimento.Mesa;
    import Model.Produtos.ItemVendavel;
    import Model.Usuarios.Usuario;

    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.Alert;
    import javafx.scene.control.Button;
    import javafx.scene.control.Label;
    import javafx.scene.control.ListView;
    import javafx.stage.Modality;
    import javafx.stage.Stage;

    import java.io.IOException;
    import java.util.List;

    public class GerenciarMesaController extends BaseController {

        @FXML
        private Label labelTituloMesa;

        @FXML
        private ListView<Comanda> listaComandas;

        @FXML
        private Button botaoAbrirComanda;

        @FXML
        private Button botaoPagar;

        private Mesa mesa;
        private Usuario atendente;
        private List<ItemVendavel> produtosDisponiveis;
        private List<Comanda> comandasSemMesa;
        private MesaController navegador;

        public void inicializar(
                Mesa mesa,
                Usuario atendente,
                List<Comanda> comandasSemMesa,
                List<ItemVendavel> itens,
                MesaController navegador
        ) {

            this.mesa = mesa;
            this.atendente = atendente;
            this.produtosDisponiveis = itens;
            this.comandasSemMesa = comandasSemMesa;
            this.navegador = navegador;

            labelTituloMesa.setText(
                    "Gerenciando Mesa " + mesa.getNumMesa()
            );

            atualizarListaComandas();

            this.listaComandas
                    .getSelectionModel()
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

            atualizarVisibilidadeBotoes(
                    listaComandas
                            .getSelectionModel()
                            .getSelectedItem()
            );
        }

        private void atualizarVisibilidadeBotoes(
                Comanda selecionada
        ) {

            boolean possuiComandaAberta =
                    mesa != null
                    && mesa.temComandaAberta();

            boolean comandaSelecionadaAberta =
                    selecionada != null
                    && !selecionada.isFechada();

            boolean podeAbrirNovaComanda =
                    mesa != null
                    && !possuiComandaAberta;

            botaoAbrirComanda.setVisible(
                    podeAbrirNovaComanda
            );

            botaoAbrirComanda.setManaged(
                    podeAbrirNovaComanda
            );

            botaoPagar.setVisible(
                    comandaSelecionadaAberta
            );

            botaoPagar.setManaged(
                    comandaSelecionadaAberta
            );
        }

        private void atualizarListaComandas() {

            listaComandas.getItems().clear();

            if (mesa == null || mesa.getComandas() == null) {

                atualizarVisibilidadeBotoes(null);
                return;
            }

            for (Comanda comanda : mesa.getComandas()) {

                if (!comanda.isFechada()) {

                    listaComandas
                            .getItems()
                            .add(comanda);
                }
            }

            if (!listaComandas.getItems().isEmpty()) {

                listaComandas
                        .getSelectionModel()
                        .selectFirst();
            }

            atualizarVisibilidadeBotoes(
                    listaComandas
                            .getSelectionModel()
                            .getSelectedItem()
            );
        }

        @FXML
        private void pagarComandaSelecionada() {

            Comanda selecionada =
                    listaComandas
                            .getSelectionModel()
                            .getSelectedItem();

            if (selecionada == null) {

                mostrarAlerta(
                        "Comanda",
                        "Selecione uma comanda aberta."
                );

                return;
            }

            if (selecionada.isFechada()) {

                mostrarAlerta(
                        "Comanda fechada",
                        "Esta comanda já foi fechada."
                );

                atualizarListaComandas();
                return;
            }

            if (navegador != null) {

                navegador.abrirFechamento(
                        selecionada
                );
            }
        }

        @FXML
        private void adicionarNovaComanda() {

            if (mesa.isOcupada()
                    || mesa.temComandaAberta()) {

                mostrarAlerta(
                        "Mesa ocupada",
                        "Esta mesa já possui uma comanda aberta."
                );

                atualizarVisibilidadeBotoes(
                        listaComandas
                                .getSelectionModel()
                                .getSelectedItem()
                );

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

                stage.setTitle(
                        "Abrir comanda"
                );

                stage.setScene(
                        new Scene(root)
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

                        if (comandasSemMesa != null) {

                            comandasSemMesa.add(
                                    comandaCriada
                            );
                        }

                    } else {

                        atualizarListaComandas();

                        listaComandas
                                .getSelectionModel()
                                .select(comandaCriada);
                    }

                    if (navegador != null) {

                        navegador.abrirTelaDaComanda(
                                mesaDaComanda,
                                comandaCriada
                        );
                    }
                }

            } catch (IOException e) {

                e.printStackTrace();

                mostrarAlerta(
                        "Erro",
                        "Não foi possível abrir o diálogo de nova comanda."
                );
            }
        }

        @FXML
        private void abrirComanda() {
            Comanda selecionada =
                    listaComandas
                            .getSelectionModel()
                            .getSelectedItem();

            if (selecionada == null) {
                return;
            }

            if (selecionada.isFechada()) {
                atualizarListaComandas();
                return;
            }

            if (navegador != null) {
                try {
                    navegador.abrirTelaDaComanda(
                            this.mesa,
                            selecionada
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                    mostrarAlerta(
                            "Erro",
                            "Não foi possível abrir a comanda."
                    );
                }
            }
        }
        @Override
        protected void mostrarAlerta(
                String titulo,
                String msg
        ) {

            Alert alerta =
                    new Alert(
                            Alert.AlertType.INFORMATION
                    );

            alerta.setTitle(titulo);
            alerta.setHeaderText(null);
            alerta.setContentText(msg);
            alerta.showAndWait();
        }
    }