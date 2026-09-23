package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Pedido;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CancelarItemController {

    @FXML
    private Label labelDescricao;

    private Pedido pedido;
    private Comanda comanda;
    private boolean confirmado;

    public void inicializar(
            Pedido pedido,
            Comanda comanda
    ) {
        this.pedido = pedido;
        this.comanda = comanda;
        this.confirmado = false;

        String numeroComanda =
                extrairNumeroComanda(
                        comanda
                );

        labelDescricao.setText(
                "Deseja cancelar " +
                pedido.getQuantidade() +
                " × " +
                pedido.getItem().getNome() +
                " da comanda #" +
                numeroComanda +
                "?"
        );
    }

    private String extrairNumeroComanda(
            Comanda comanda
    ) {
        if (comanda == null) {
            return "";
        }

        String texto =
                comanda.toString();

        String numero =
                texto.replaceAll(
                        ".*?Comanda\\s+#?(\\d+).*",
                        "$1"
                );

        if (numero.equals(texto)) {
            return texto;
        }

        return numero;
    }

    @FXML
    private void voltar() {
        fechar();
    }

    @FXML
    private void confirmar() {
        confirmado = true;
        fechar();
    }

    private void fechar() {
        Stage stage =
                (Stage) labelDescricao
                        .getScene()
                        .getWindow();

        stage.close();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}