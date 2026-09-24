package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Pedido;
import Model.Atendimento.Mesa;
import Model.Pagamento.Pagamento;
import Model.Pagamento.PagamentoCartaoCredito;
import Model.Pagamento.PagamentoCartaoDebito;
import Model.Pagamento.PagamentoDinheiro;
import Model.Pagamento.PagamentoPixPresencial;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Locale;

public class FechamentoContaController extends BaseController {

    @FXML
    private Label labelComanda;

    @FXML
    private ListView<String> listaItens;

    @FXML
    private Label labelSubtotal;

    @FXML
    private Label labelDesconto;

    @FXML
    private Label labelTotal;

    @FXML
    private TextField campoDesconto;

    @FXML
    private ComboBox<String> comboFormaPagamento;

    private Comanda comanda;
    private Usuario usuario;
    private MesaController navegador;
    private Mesa mesa;
    private boolean fechamentoConcluido;

    public void inicializar(
            Comanda comanda,
            Usuario usuario,
            MesaController navegador
    ) {
        inicializar(
                comanda,
                usuario,
                navegador,
                null
        );
    }

    public void inicializar(
            Comanda comanda,
            Usuario usuario,
            MesaController navegador,
            Mesa mesa
    ) {
        this.comanda = comanda;
        this.usuario = usuario;
        this.navegador = navegador;
        this.mesa = mesa;
        this.fechamentoConcluido = false;

        configurarListaItens();
        preencherResumo();
        configurarDesconto();
        configurarFormasPagamento();
    }

    private void configurarListaItens() {
        listaItens.setFixedCellSize(30);

        listaItens.setCellFactory(param ->
                new ListCell<String>() {

                    private final Label descricao =
                            new Label();

                    private final Label valor =
                            new Label();

                    private final HBox caixa =
                            new HBox(10);

                    {
                        HBox.setHgrow(
                                descricao,
                                Priority.ALWAYS
                        );

                        descricao.setStyle(
                                "-fx-font-size: 12px;" +
                                "-fx-text-fill: #526069;"
                        );

                        valor.setStyle(
                                "-fx-font-size: 12px;" +
                                "-fx-text-fill: #526069;"
                        );

                        caixa.setAlignment(
                                Pos.CENTER_LEFT
                        );

                        caixa.getChildren().addAll(
                                descricao,
                                valor
                        );

                        setPadding(
                                new Insets(0)
                        );

                        setStyle(
                                "-fx-background-color: white;"
                        );
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty
                    ) {
                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {
                            setGraphic(null);
                            return;
                        }

                        String[] partes =
                                item.split(
                                        "\\s\\|\\s",
                                        2
                                );

                        descricao.setText(
                                partes[0]
                        );

                        valor.setText(
                                partes.length > 1
                                        ? partes[1]
                                        : ""
                        );

                        setGraphic(caixa);
                    }
                }
        );
    }

    private void preencherResumo() {
        if (comanda == null) {
            return;
        }

        String textoComanda =
                comanda.toString();

        String numeroComanda =
                textoComanda.replaceAll(
                        ".*?Comanda\\s+#?(\\d+).*",
                        "$1"
                );

        if (mesa != null) {
            labelComanda.setText(
                    "Comanda #" +
                    numeroComanda +
                    " • Mesa " +
                    String.format(
                            "%02d",
                            mesa.getNumMesa()
                    )
            );
        } else {
            labelComanda.setText(
                    "Comanda #" +
                    numeroComanda +
                    " • Cliente sem mesa"
            );
        }

        listaItens.getItems().clear();

        for (Pedido pedido :
                comanda.getPedidos()) {

            String linha =
                    String.format(
                            "%d × %s | R$ %.2f",
                            pedido.getQuantidade(),
                            pedido.getItem().getNome(),
                            pedido.getSubtotal()
                    );

            listaItens.getItems().add(
                    linha
            );
        }

        atualizarValores();
    }

    private void configurarDesconto() {
        boolean autorizado =
                usuario instanceof Interno;

        campoDesconto.setDisable(
                !autorizado
        );

        campoDesconto.setText("0");

        campoDesconto.textProperty().addListener(
                (obs, valorAntigo, valorNovo) ->
                        atualizarValores()
        );
    }

    private void configurarFormasPagamento() {
        comboFormaPagamento.getItems().setAll(
                "Dinheiro",
                "Cartão de Débito",
                "Cartão de Crédito",
                "Pix presencial"
        );
    }

    private void atualizarValores() {
        if (comanda == null) {
            return;
        }

        double subtotal =
                comanda.calcularSubtotal();

        double desconto =
                lerDesconto();

        double total =
                Math.max(
                        0.0,
                        subtotal - desconto
                );

        labelSubtotal.setText(
                String.format(
                        Locale.forLanguageTag("pt-BR"),
                        "R$ %.2f",
                        subtotal
                )
        );

        labelDesconto.setText(
                String.format(
                        Locale.forLanguageTag("pt-BR"),
                        "R$ %.2f",
                        desconto
                )
        );

        labelTotal.setText(
                String.format(
                        Locale.forLanguageTag("pt-BR"),
                        "R$ %.2f",
                        total
                )
        );
    }

    private double lerDesconto() {
        if (campoDesconto == null
                || campoDesconto.isDisabled()) {
            return 0.0;
        }

        String texto =
                campoDesconto.getText();

        if (texto == null
                || texto.trim().isEmpty()) {
            return 0.0;
        }

        try {
            double desconto =
                    Double.parseDouble(
                            texto
                                    .replace(",", ".")
                                    .trim()
                    );

            if (desconto < 0) {
                return 0.0;
            }

            return Math.min(
                    desconto,
                    comanda.calcularSubtotal()
            );

        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    private void confirmarFechamento() {
        if (comanda == null) {
            mostrarAlerta(
                    "Erro",
                    "Nenhuma comanda foi carregada."
            );
            return;
        }

        if (comanda.isFechada()) {
            mostrarAlerta(
                    "Comanda fechada",
                    "Esta comanda já foi fechada."
            );
            return;
        }

        if (comanda.getPedidos().isEmpty()) {
            mostrarAlerta(
                    "Comanda vazia",
                    "A conta não pode ser fechada sem itens registrados."
            );
            return;
        }

        String forma =
                comboFormaPagamento.getValue();

        if (forma == null
                || forma.isBlank()) {
            mostrarAlerta(
                    "Forma de pagamento",
                    "Selecione uma forma de pagamento."
            );
            return;
        }

        double subtotal =
                comanda.calcularSubtotal();

        double desconto = 0.0;

        try {
            if (!campoDesconto.isDisabled()) {
                String textoDesconto =
                        campoDesconto.getText();

                if (textoDesconto != null
                        && !textoDesconto.trim().isEmpty()) {

                    desconto =
                            Double.parseDouble(
                                    textoDesconto
                                            .replace(",", ".")
                                            .trim()
                            );
                }
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(
                    "Desconto inválido",
                    "Digite um valor numérico para o desconto."
            );
            return;
        }

        if (desconto < 0) {
            mostrarAlerta(
                    "Desconto inválido",
                    "O desconto não pode ser negativo."
            );
            return;
        }

        if (desconto > subtotal) {
            mostrarAlerta(
                    "Desconto inválido",
                    "O desconto não pode ser maior que o subtotal."
            );
            return;
        }

        double total =
                subtotal - desconto;

        Pagamento pagamento;

        try {
            pagamento =
                    criarPagamento(
                            forma,
                            total
                    );

        } catch (IllegalArgumentException e) {
            mostrarAlerta(
                    "Pagamento",
                    e.getMessage()
            );
            return;
        }

        if (!pagamento.processar()) {
            mostrarAlerta(
                    "Pagamento recusado",
                    "Não foi possível confirmar o pagamento."
            );
            return;
        }

        try {
            if (navegador == null) {
                throw new IllegalStateException("A sessão do sistema não está disponível.");
            }

            // Primeiro grava a venda no banco. Assim o fechamento só é concluído
            // depois que o registro exigido pelo RF06 estiver persistido.
            navegador.registrarVendaNoHistorico(
                    comanda,
                    mesa,
                    pagamento,
                    desconto
            );

            comanda.registrarFechamento(
                    pagamento,
                    desconto,
                    usuario
            );

            // O fechamento devolve a mesa ao estado Livre.
            if (mesa != null) {
                mesa.setAguardandoPagamento(false);
                navegador.persistirMesa(mesa);
            }

        } catch (
                IllegalStateException |
                IllegalArgumentException e
        ) {
            mostrarAlerta(
                    "Não foi possível fechar",
                    e.getMessage()
            );
            return;
        }

        fechamentoConcluido = true;

        mostrarAlerta(
                "Fechamento concluído",
                "Conta fechada com sucesso!\n\n"
                        + "Subtotal: R$ "
                        + String.format(
                                "%.2f",
                                subtotal
                        )
                        + "\n"
                        + "Desconto: R$ "
                        + String.format(
                                "%.2f",
                                desconto
                        )
                        + "\n"
                        + "Total: R$ "
                        + String.format(
                                "%.2f",
                                total
                        )
                        + "\n"
                        + "Pagamento: "
                        + pagamento.getTipo()
        );

        if (navegador != null) {
            navegador.abrirListaComandas();
        }
    }

    private Pagamento criarPagamento(
            String forma,
            double total
    ) {
        switch (forma) {

            case "Dinheiro":
                return new PagamentoDinheiro(
                        total,
                        total
                );

            case "Cartão de Débito":
                return new PagamentoCartaoDebito(
                        total,
                        "Não informado"
                );

            case "Cartão de Crédito":
                return new PagamentoCartaoCredito(
                        total,
                        "Não informado",
                        1
                );

            case "Pix presencial":
                return new PagamentoPixPresencial(
                        total
                );

            default:
                throw new IllegalArgumentException(
                        "Forma de pagamento inválida."
                );
        }
    }

    @FXML
    private void voltar() {
        if (navegador != null) {
            navegador.abrirListaComandas();
        }
    }

    public boolean isFechamentoConcluido() {
        return fechamentoConcluido;
    }
}
