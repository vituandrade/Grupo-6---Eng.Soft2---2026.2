package App.Controles;

import App.Validacao.CadastroItemValidator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class CadastrarItemCardapioController {

    @FXML private TextField campoNome;
    @FXML private ComboBox<String> campoCategoria;
    @FXML private TextArea campoDescricao;
    @FXML private TextField campoPreco;
    @FXML private ComboBox<String> campoDisponibilidade;
    @FXML private Label mensagemValidacao;

    @FXML
    private void initialize() {
        campoCategoria.getItems().setAll("Lanche", "Bebida", "Prato", "Sobremesa");
        campoDisponibilidade.getItems().setAll("Disponível", "Indisponível");
        campoDisponibilidade.setValue("Disponível");
    }

    public CadastroItemValidator.Resultado validar() {
        limparErros();

        CadastroItemValidator.Resultado resultado = CadastroItemValidator.validar(
                campoNome.getText(),
                campoCategoria.getValue(),
                campoDescricao.getText(),
                campoPreco.getText(),
                campoDisponibilidade.getValue()
        );

        marcarInvalido(campoNome, resultado.possuiErro(CadastroItemValidator.NOME));
        marcarInvalido(campoCategoria, resultado.possuiErro(CadastroItemValidator.CATEGORIA));
        marcarInvalido(campoPreco, resultado.possuiErro(CadastroItemValidator.PRECO));
        marcarInvalido(campoDisponibilidade, resultado.possuiErro(CadastroItemValidator.DISPONIBILIDADE));

        if (!resultado.valido()) {
            mensagemValidacao.setText(resultado.primeiraMensagem());
        }
        return resultado;
    }

    public void exibirFalhaDePersistencia(String mensagem) {
        mensagemValidacao.setText(mensagem);
    }

    private void limparErros() {
        mensagemValidacao.setText("");
        marcarInvalido(campoNome, false);
        marcarInvalido(campoCategoria, false);
        marcarInvalido(campoPreco, false);
        marcarInvalido(campoDisponibilidade, false);
    }

    private void marcarInvalido(Control campo, boolean invalido) {
        campo.getStyleClass().remove("campo-invalido");
        if (invalido) {
            campo.getStyleClass().add("campo-invalido");
        }
    }
}
