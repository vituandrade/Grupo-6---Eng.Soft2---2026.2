package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Usuarios.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public class PainelInicialController {

    @FXML private Label labelMesasLivres;
    @FXML private Label labelComandasAbertas;
    @FXML private Label labelEstoqueBaixo;
    @FXML private VBox listaComandas;
    @FXML private Label labelSemComandas;
    @FXML private Button botaoMovimentarEstoque;

    private MesaController navegador;

    public void inicializar(
            List<Mesa> mesas,
            List<Comanda> comandasSemMesa,
            InterfacePersistencia persistencia,
            Usuario usuarioLogado,
            MesaController navegador
    ) {
        this.navegador = navegador;

        ResumoPainelInicial resumo = ResumoPainelInicial.calcular(
                mesas,
                comandasSemMesa,
                persistencia.carregarItensEstoque()
        );

        labelMesasLivres.setText(formatarContador(resumo.mesasLivres()));
        labelComandasAbertas.setText(formatarContador(resumo.comandasAbertas()));
        labelEstoqueBaixo.setText(formatarContador(resumo.itensComEstoqueBaixo()));
        preencherComandas(resumo.comandasEmAndamento());

        boolean podeMovimentarEstoque = usuarioLogado != null && usuarioLogado.AcessoEstoque();
        botaoMovimentarEstoque.setVisible(podeMovimentarEstoque);
        botaoMovimentarEstoque.setManaged(podeMovimentarEstoque);
    }

    private void preencherComandas(List<ResumoPainelInicial.ComandaEmAndamento> comandas) {
        listaComandas.getChildren().clear();
        labelSemComandas.setVisible(comandas.isEmpty());
        labelSemComandas.setManaged(comandas.isEmpty());

        for (ResumoPainelInicial.ComandaEmAndamento item : comandas) {
            listaComandas.getChildren().add(criarLinhaComanda(item));
        }
    }

    private Label criarLinhaComanda(ResumoPainelInicial.ComandaEmAndamento item) {
        Comanda comanda = item.comanda();
        String texto = String.format(
                Locale.forLanguageTag("pt-BR"),
                "#%03d • %s • R$ %.2f",
                comanda.getId(),
                item.atendimento(),
                comanda.calcularTotal()
        );

        Label linha = new Label(texto);
        linha.setMaxWidth(Double.MAX_VALUE);
        linha.getStyleClass().add("comanda-row");
        return linha;
    }

    private String formatarContador(long valor) {
        return String.format("%02d", valor);
    }

    @FXML
    private void abrirComanda() {
        navegador.abrirNovaComanda();
    }

    @FXML
    private void registrarPedido() {
        navegador.abrirListaComandas();
    }

    @FXML
    private void movimentarEstoque() {
        navegador.abrirEstoque();
    }

    @FXML
    private void fecharConta() {
        navegador.abrirListaComandas();
    }
}
