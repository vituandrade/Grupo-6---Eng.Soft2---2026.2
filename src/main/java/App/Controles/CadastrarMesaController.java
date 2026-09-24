package App.Controles;

import App.Validacao.MesaValidator;
import Model.Atendimento.Mesa;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;

public class CadastrarMesaController {

    @FXML private TextField campoNumero;
    @FXML private Label mensagemValidacao;

    public MesaValidator.Resultado validar(List<Mesa> mesasExistentes) {
        limparErro();
        MesaValidator.Resultado resultado =
                MesaValidator.validarTexto(campoNumero.getText(), mesasExistentes);

        if (!resultado.valido()) {
            campoNumero.getStyleClass().add("campo-invalido");
            mensagemValidacao.setText(resultado.mensagem());
        }
        return resultado;
    }

    public int getNumero() {
        return Integer.parseInt(campoNumero.getText().trim());
    }

    public void exibirFalhaDePersistencia() {
        mensagemValidacao.setText(
                "Não foi possível gravar a mesa. Tente novamente ou selecione Cancelar."
        );
    }

    private void limparErro() {
        campoNumero.getStyleClass().remove("campo-invalido");
        mensagemValidacao.setText("");
    }
}
