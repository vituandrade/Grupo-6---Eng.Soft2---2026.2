package App.Controles;

import App.Validacao.MovimentacaoEstoqueValidator;
import Model.Estoque.ItemEstoque;
import Model.Estoque.TipoMovimentacao;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class RegistrarMovimentacaoController {

    @FXML private ComboBox<String> campoItem;
    @FXML private ComboBox<String> campoTipo;
    @FXML private TextField campoQuantidade;
    @FXML private TextField campoUnidade;
    @FXML private Label mensagemValidacao;

    private final List<ItemEstoque> itens = new ArrayList<>();

    @FXML
    private void initialize() {
        campoTipo.getItems().setAll(
                TipoMovimentacao.ENTRADA.getDescricao(),
                TipoMovimentacao.SAIDA.getDescricao()
        );
        campoTipo.setValue(TipoMovimentacao.ENTRADA.getDescricao());

        campoItem.valueProperty().addListener((obs, anterior, atual) -> atualizarUnidade());
        campoItem.getEditor().textProperty().addListener((obs, anterior, atual) -> atualizarUnidade());
    }

    public void inicializar(List<ItemEstoque> itensCadastrados) {
        itens.clear();
        itens.addAll(itensCadastrados);
        campoItem.getItems().setAll(itens.stream().map(ItemEstoque::getNome).toList());
        atualizarUnidade();
    }

    public MovimentacaoEstoqueValidator.Resultado validar() {
        limparErros();
        ItemEstoque itemExistente = encontrarItem();
        MovimentacaoEstoqueValidator.Resultado resultado = MovimentacaoEstoqueValidator.validar(
                nomeInformado(),
                itemExistente == null,
                campoTipo.getValue(),
                campoQuantidade.getText(),
                campoUnidade.getText()
        );

        marcarInvalido(campoItem, resultado.possuiErro(MovimentacaoEstoqueValidator.ITEM));
        marcarInvalido(campoTipo, resultado.possuiErro(MovimentacaoEstoqueValidator.TIPO));
        marcarInvalido(campoQuantidade, resultado.possuiErro(MovimentacaoEstoqueValidator.QUANTIDADE));
        marcarInvalido(campoUnidade, resultado.possuiErro(MovimentacaoEstoqueValidator.UNIDADE));

        if (!resultado.valido()) {
            mensagemValidacao.setText(resultado.primeiraMensagem());
        }
        return resultado;
    }

    public Integer getItemIdSelecionado() {
        ItemEstoque item = encontrarItem();
        return item == null ? null : item.getId();
    }

    public void exibirErro(String mensagem) {
        mensagemValidacao.setText(mensagem);
    }

    private String nomeInformado() {
        String textoEditor = campoItem.getEditor().getText();
        return textoEditor == null || textoEditor.isBlank() ? campoItem.getValue() : textoEditor;
    }

    private ItemEstoque encontrarItem() {
        String nome = nomeInformado();
        if (nome == null) {
            return null;
        }
        return itens.stream()
                .filter(item -> item.getNome().equalsIgnoreCase(nome.trim()))
                .findFirst()
                .orElse(null);
    }

    private void atualizarUnidade() {
        ItemEstoque item = encontrarItem();
        boolean existente = item != null;
        campoUnidade.setDisable(existente);
        campoUnidade.setText(existente ? item.getUnidadeMedida() : "");
    }

    private void limparErros() {
        mensagemValidacao.setText("");
        marcarInvalido(campoItem, false);
        marcarInvalido(campoTipo, false);
        marcarInvalido(campoQuantidade, false);
        marcarInvalido(campoUnidade, false);
    }

    private void marcarInvalido(Control campo, boolean invalido) {
        campo.getStyleClass().remove("campo-invalido");
        if (invalido) {
            campo.getStyleClass().add("campo-invalido");
        }
    }
}
