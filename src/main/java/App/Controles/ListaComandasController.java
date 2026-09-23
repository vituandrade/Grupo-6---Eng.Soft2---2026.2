package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Produtos.ItemVendavel;
import Model.Usuarios.Usuario;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class ListaComandasController extends BaseController {

    @FXML
    private TextField campoPesquisa;

    @FXML
    private TilePane painelComandas;

    private List<Mesa> todasAsMesas;
    private List<Comanda> comandasSemMesa;
    private Usuario usuarioLogado;
    private List<ItemVendavel> produtosDisponiveis;
    private MesaController navegador;

    public void inicializar(
            List<Mesa> mesas,
            Usuario usuario,
            List<Comanda> comandasSemMesa,
            List<ItemVendavel> produtos,
            MesaController navegador
    ) {

        this.todasAsMesas = mesas;
        this.usuarioLogado = usuario;
        this.comandasSemMesa = comandasSemMesa;
        this.produtosDisponiveis = produtos;
        this.navegador = navegador;

        atualizarVisual();

        campoPesquisa.textProperty().addListener(
                (obs, oldVal, newVal) -> atualizarVisual()
        );
    }

    @FXML
    private void atualizarVisual() {

        painelComandas.getChildren().clear();

        String termo =
                (campoPesquisa.getText() == null)
                        ? ""
                        : campoPesquisa.getText()
                                .toLowerCase()
                                .trim();

        for (Mesa m : todasAsMesas) {

            for (Comanda c : m.getComandas()) {

                if (c.isFechada()) {
                    continue;
                }

                boolean matchCliente =
                        c.getClienteNome()
                                .toLowerCase()
                                .startsWith(termo);

                boolean matchMesa =
                        String.valueOf(m.getNumMesa())
                                .contains(termo);

                if (termo.isEmpty()
                        || matchCliente
                        || matchMesa) {

                    VBox card =
                            criarCardComanda(c, m);

                    painelComandas
                            .getChildren()
                            .add(card);
                }
            }
        }

        if (comandasSemMesa != null) {

            for (Comanda c : comandasSemMesa) {

                if (c.isFechada()) {
                    continue;
                }

                boolean matchCliente =
                        c.getClienteNome()
                                .toLowerCase()
                                .startsWith(termo);

                boolean matchSemMesa =
                        "sem mesa".contains(termo)
                        || "cliente sem mesa".contains(termo);

                if (termo.isEmpty()
                        || matchCliente
                        || matchSemMesa) {

                    VBox card =
                            criarCardComanda(c, null);

                    painelComandas
                            .getChildren()
                            .add(card);
                }
            }
        }
    }

    private VBox criarCardComanda(
            Comanda c,
            Mesa m
    ) {

        VBox card = new VBox(8);

        card.setPrefWidth(200);
        card.setPrefHeight(190);

        String corBorda =
                c.isFechada()
                        ? "#ffc107"
                        : "#28a745";

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 8;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);" +
                "-fx-padding: 15;" +
                "-fx-border-color: " + corBorda + ";" +
                "-fx-border-width: 0 0 0 5;" +
                "-fx-border-radius: 8;"
        );

        Label lblMesa =
                new Label(
                        m == null
                                ? "Cliente sem mesa"
                                : "Mesa " + m.getNumMesa()
                );

        lblMesa.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;"
        );

        Label lblStatus =
                new Label(
                        c.isFechada()
                                ? "FECHADA"
                                : "ABERTA"
                );

        String styleStatus =
                c.isFechada()
                        ? "-fx-text-fill: #ffc107;" +
                          "-fx-font-size: 10px;" +
                          "-fx-font-weight: bold;"
                        : "-fx-text-fill: #28a745;" +
                          "-fx-font-size: 10px;" +
                          "-fx-font-weight: bold;";

        lblStatus.setStyle(
                styleStatus
        );

        HBox containerTopo =
                new HBox(
                        10,
                        lblMesa,
                        lblStatus
                );

        containerTopo.setAlignment(
                Pos.CENTER_LEFT
        );

        Label lblCliente =
                new Label(
                        c.getClienteNome()
                );

        lblCliente.setStyle(
                "-fx-font-size: 16px;"
        );

        lblCliente.setWrapText(
                true
        );

        Label lblTotal =
                new Label(
                        "Total: R$ " +
                        String.format(
                                "%.2f",
                                c.calcularTotal()
                        )
                );

        lblTotal.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #555;"
        );

        card.getChildren().addAll(
                containerTopo,
                lblCliente,
                lblTotal
        );

        if (!c.isFechada()) {

            Button botaoPagar =
                    new Button("PAGAR");

            botaoPagar.setMaxWidth(
                    Double.MAX_VALUE
            );

            botaoPagar.setStyle(
                    "-fx-background-color: #28a745;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;"
            );

            botaoPagar.setOnAction(
                    e -> pagarComanda(c, m)
            );

            botaoPagar.setOnMouseClicked(
                    e -> e.consume()
            );

            card.getChildren().add(
                    botaoPagar
            );
        }

        card.setOnMouseEntered(e ->
                card.setStyle(
                        card.getStyle() +
                        "-fx-background-color: #f8f9fa;"
                )
        );

        card.setOnMouseExited(e ->
                card.setStyle(
                        card.getStyle().replace(
                                "-fx-background-color: #f8f9fa;",
                                "-fx-background-color: white;"
                        )
                )
        );

        card.setOnMouseClicked(e -> {
            abrirComandaEspecifica(
                    c,
                    m
            );
        });

        return card;
    }

    private void pagarComanda(
            Comanda comanda,
            Mesa mesaDaComanda
    ) {

        if (comanda == null || comanda.isFechada()) {

            mostrarAlerta(
                    "Erro",
                    "A comanda já está fechada."
            );

            return;
        }

        if (navegador != null) {

            navegador.abrirFechamento(
                    comanda
            );
        }
    }

    private void abrirComandaEspecifica(
            Comanda comanda,
            Mesa mesaDaComanda
    ) {

        if (navegador != null) {

            try {

                navegador.abrirTelaDaComanda(
                        mesaDaComanda,
                        comanda
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
}