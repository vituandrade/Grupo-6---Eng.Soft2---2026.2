package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Atendimento.Pedido;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.text.SimpleDateFormat;
import java.util.*;

public class RelatorioPedidosController extends BaseController {

    @FXML private TilePane painelPedidos;

    private List<Mesa> listaDeMesas;

    public void inicializar(List<Mesa> mesas) {
        this.listaDeMesas = mesas;
        carregarDados();
    }

    @FXML
    private void carregarDados() {
        painelPedidos.getChildren().clear();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        List<CardDados> listaDeCards = new ArrayList<>();

        for (Mesa m : listaDeMesas) {
            for (Comanda c : m.getComandas()) {
                Map<Integer, List<Pedido>> grupos = new HashMap<>();

                for (Pedido p : c.getPedidos()) {
                    int lote = p.getNumeroLote();
                    if (lote == 0) lote = -1; // Antigos
                    grupos.computeIfAbsent(lote, k -> new ArrayList<>()).add(p);
                }

                for (Map.Entry<Integer, List<Pedido>> entry : grupos.entrySet()) {
                    List<Pedido> itens = entry.getValue();

                    Date horario = (itens.isEmpty()) ? new Date() : itens.get(0).getHorario();

                    String tituloPedido = (entry.getKey() == -1) ? "Antigos" : "Pedido #" + entry.getKey();

                    listaDeCards.add(new CardDados(m, c, tituloPedido, horario, itens));
                }
            }
        }

        listaDeCards.sort((card1, card2) -> card2.horario.compareTo(card1.horario));

        for (CardDados card : listaDeCards) {
            VBox cardVisual = criarCardVisual(card, sdf);
            painelPedidos.getChildren().add(cardVisual);
        }
    }

    private VBox criarCardVisual(CardDados dados, SimpleDateFormat sdf) {
        VBox card = new VBox(5);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 8;");

        Label lblMesaCliente = new Label("Mesa " + dados.mesa.getNumMesa() + " - " + dados.comanda.getClienteNome());
        lblMesaCliente.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        lblMesaCliente.setWrapText(true);

        HBox topoInfo = new HBox(10);
        Label lblHora = new Label(sdf.format(dados.horario));
        lblHora.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        Label lblIdPedido = new Label(dados.tituloLote);
        lblIdPedido.setStyle("-fx-background-color: #e2e6ea; -fx-padding: 2 5; -fx-background-radius: 4; -fx-font-size: 11px;");

        topoInfo.getChildren().addAll(lblHora, lblIdPedido);

        Label separador = new Label("-----------------------");
        separador.setStyle("-fx-text-fill: #ccc;");

        VBox listaItens = new VBox(2);
        for (Pedido p : dados.itens) {
            String txtItem = String.format("%dx %s", p.getQuantidade(), p.getItem().getNome());
            Label lblItem = new Label(txtItem);
            lblItem.setStyle("-fx-font-size: 13px;");
            listaItens.getChildren().add(lblItem);
            if (p.getObservacao() != null && !p.getObservacao().trim().isEmpty()) {
                Label lblObs = new Label("   (" + p.getObservacao() + ")");
                lblObs.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-style: italic;");
                listaItens.getChildren().add(lblObs);
            }
        }

        card.getChildren().addAll(lblMesaCliente, topoInfo, separador, listaItens);
        return card;
    }

    private static class CardDados {
        Mesa mesa;
        Comanda comanda;
        String tituloLote;
        Date horario;
        List<Pedido> itens;

        public CardDados(Mesa m, Comanda c, String t, Date h, List<Pedido> i) {
            this.mesa = m; this.comanda = c; this.tituloLote = t; this.horario = h; this.itens = i;
        }
    }
}