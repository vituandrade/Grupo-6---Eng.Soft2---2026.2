package App.Controles;

import javafx.geometry.Pos;
import App.Persistencia.InterfacePersistencia;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class RelatoriosController {

    private static final DateTimeFormatter DATA_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Button botaoVendas;
    @FXML private Button botaoItensMaisVendidos;
    @FXML private Button botaoConsumo;
    @FXML private Button botaoEstoque;
    @FXML private Label labelTituloRelatorio;
    @FXML private Label labelSubtituloRelatorio;
    @FXML private Label labelMensagem;
    @FXML private Label labelDataInicial;
    @FXML private Label labelAte;
    @FXML private Label labelDataFinal;
    @FXML private TextField campoDataInicial;
    @FXML private TextField campoDataFinal;
    @FXML private Button botaoGerar;
    @FXML private Label labelResumo1;
    @FXML private Label labelResumo2;
    @FXML private Label labelResumo3;
    @FXML private Label labelValor1;
    @FXML private Label labelValor2;
    @FXML private Label labelValor3;
    @FXML private Label labelColuna1;
    @FXML private Label labelColuna2;
    @FXML private Label labelColuna3;
    @FXML private TableView<ObjetoRelatorio> tabela;
    @FXML private TableColumn<ObjetoRelatorio, String> coluna1;
    @FXML private TableColumn<ObjetoRelatorio, String> coluna2;
    @FXML private TableColumn<ObjetoRelatorio, String> coluna3;

    private InterfacePersistencia persistencia;
    private TipoRelatorio tipoAtual = TipoRelatorio.VENDAS;

    public enum TipoRelatorio {
        VENDAS,
        ITENS_MAIS_VENDIDOS,
        CONSUMO_POR_PERIODO,
        ESTOQUE
    }

    public static class ObjetoRelatorio {
        private final String valor1;
        private final String valor2;
        private final String valor3;

        public ObjetoRelatorio(String valor1, String valor2, String valor3) {
            this.valor1 = valor1;
            this.valor2 = valor2;
            this.valor3 = valor3;
        }

        public String getValor1() {
            return valor1;
        }

        public String getValor2() {
            return valor2;
        }

        public String getValor3() {
            return valor3;
        }
    }

    @FXML
    private void initialize() {
        configurarTabela();
        LocalDate hoje = LocalDate.now();
        campoDataInicial.setText(DATA_INPUT.format(hoje.withDayOfMonth(1)));
        campoDataFinal.setText(DATA_INPUT.format(hoje));
        selecionarTipo(TipoRelatorio.VENDAS);
    }

    public void inicializar(InterfacePersistencia persistencia) {
        this.persistencia = persistencia;
        gerarRelatorio();
    }

    @FXML
    private void selecionarVendas() {
        selecionarTipo(TipoRelatorio.VENDAS);
    }

    @FXML
    private void selecionarItensMaisVendidos() {
        selecionarTipo(TipoRelatorio.ITENS_MAIS_VENDIDOS);
    }

    @FXML
    private void selecionarConsumo() {
        selecionarTipo(TipoRelatorio.CONSUMO_POR_PERIODO);
    }

    @FXML
    private void selecionarEstoque() {
        selecionarTipo(TipoRelatorio.ESTOQUE);
    }

    @FXML
    private void gerarRelatorio() {
        if (persistencia == null) {
            return;
        }

        labelMensagem.setText("");

        try {
            switch (tipoAtual) {
                case VENDAS -> gerarVendas();
                case ITENS_MAIS_VENDIDOS -> gerarItensMaisVendidos();
                case CONSUMO_POR_PERIODO -> gerarConsumoPorPeriodo();
                case ESTOQUE -> gerarEstoque();
            }
        } catch (IllegalArgumentException e) {
            tabela.getItems().clear();
            labelMensagem.setText(e.getMessage());
        } catch (RuntimeException e) {
            tabela.getItems().clear();
            labelMensagem.setText("Não foi possível gerar o relatório.");
        }
    }

    private void selecionarTipo(TipoRelatorio tipo) {
        tipoAtual = tipo;
        atualizarCartoes();
        boolean usaPeriodo = tipo == TipoRelatorio.VENDAS || tipo == TipoRelatorio.CONSUMO_POR_PERIODO;

        labelDataInicial.setVisible(usaPeriodo);
        labelDataInicial.setManaged(usaPeriodo);
        campoDataInicial.setVisible(usaPeriodo);
        campoDataInicial.setManaged(usaPeriodo);
        labelDataFinal.setVisible(usaPeriodo);
        labelDataFinal.setManaged(usaPeriodo);
        campoDataFinal.setVisible(usaPeriodo);
        campoDataFinal.setManaged(usaPeriodo);
        labelAte.setVisible(usaPeriodo);
        labelAte.setManaged(usaPeriodo);
        botaoGerar.setVisible(true);
        botaoGerar.setManaged(true);

        switch (tipo) {
            case VENDAS -> {
                labelTituloRelatorio.setText("Relatório de vendas");
                labelSubtituloRelatorio.setText("Resumo das vendas realizadas no período");
            }
            case ITENS_MAIS_VENDIDOS -> {
                labelTituloRelatorio.setText("Itens mais vendidos");
                labelSubtituloRelatorio.setText("Ranking dos produtos mais vendidos");
            }
            case CONSUMO_POR_PERIODO -> {
                labelTituloRelatorio.setText("Consumo por período");
                labelSubtituloRelatorio.setText("Itens consumidos no período informado");
            }
            case ESTOQUE -> {
                labelTituloRelatorio.setText("Relatório de estoque");
                labelSubtituloRelatorio.setText("Saldo atual e situação dos itens");
            }
        }

        tabela.getItems().clear();
        limparResumo();
        if (persistencia != null && tipo != TipoRelatorio.ESTOQUE) {
            gerarRelatorio();
        } else if (persistencia != null) {
            gerarRelatorio();
        }
    }

    private void gerarVendas() {
        Periodo periodo = lerPeriodo();
        InterfacePersistencia.ResumoVendas resumo =
                persistencia.gerarResumoVendas(periodo.inicio(), periodo.fim());

        labelResumo1.setText("Total vendido");
        labelResumo2.setText("Comandas fechadas");
        labelResumo3.setText("Ticket médio");
        labelValor1.setText(formatarMoeda(resumo.totalVendido()));
        labelValor2.setText(String.valueOf(resumo.comandasFechadas()));
        labelValor3.setText(formatarMoeda(resumo.ticketMedio()));

        configurarCabecalho("Comanda", "Atendimento", "Total");
        tabela.getItems().setAll(
                persistencia.carregarHistoricoVendas(periodo.inicio(), periodo.fim(), "Todas")
                        .stream()
                        .map(venda -> new ObjetoRelatorio(
                                "#" + venda.getComandaId(),
                                venda.getAtendimento(),
                                formatarMoeda(venda.getTotal())
                        ))
                        .toList()
        );

        if (tabela.getItems().isEmpty()) {
            labelMensagem.setText("Nenhum dado encontrado.");
        }
    }

    private void gerarItensMaisVendidos() {
        labelResumo1.setText("Itens registrados");
        labelResumo2.setText("Unidades vendidas");
        labelResumo3.setText("Faturamento");
        var itens = persistencia.carregarItensMaisVendidos();

        double quantidade = itens.stream().mapToDouble(InterfacePersistencia.ItemRelatorio::quantidade).sum();
        double faturamento = itens.stream().mapToDouble(InterfacePersistencia.ItemRelatorio::valorTotal).sum();

        labelValor1.setText(String.valueOf(itens.size()));
        labelValor2.setText(formatarQuantidade(quantidade));
        labelValor3.setText(formatarMoeda(faturamento));

        configurarCabecalho("Produto", "Quantidade", "Faturamento");
        tabela.getItems().setAll(
                itens.stream()
                        .map(item -> new ObjetoRelatorio(
                                item.produto(),
                                formatarQuantidade(item.quantidade()),
                                formatarMoeda(item.valorTotal())
                        ))
                        .toList()
        );

        if (tabela.getItems().isEmpty()) {
            labelMensagem.setText("Nenhum dado encontrado.");
        }
    }

    private void gerarConsumoPorPeriodo() {
        Periodo periodo = lerPeriodo();
        var itens = persistencia.carregarConsumoPorPeriodo(periodo.inicio(), periodo.fim());

        double quantidade = itens.stream().mapToDouble(InterfacePersistencia.ItemRelatorio::quantidade).sum();
        double valor = itens.stream().mapToDouble(InterfacePersistencia.ItemRelatorio::valorTotal).sum();

        labelResumo1.setText("Itens consumidos");
        labelResumo2.setText("Unidades consumidas");
        labelResumo3.setText("Valor movimentado");
        labelValor1.setText(String.valueOf(itens.size()));
        labelValor2.setText(formatarQuantidade(quantidade));
        labelValor3.setText(formatarMoeda(valor));

        configurarCabecalho("Produto", "Quantidade", "Valor");
        tabela.getItems().setAll(
                itens.stream()
                        .map(item -> new ObjetoRelatorio(
                                item.produto(),
                                formatarQuantidade(item.quantidade()),
                                formatarMoeda(item.valorTotal())
                        ))
                        .toList()
        );

        if (tabela.getItems().isEmpty()) {
            labelMensagem.setText("Nenhum dado encontrado.");
        }
    }

    private void gerarEstoque() {
        var itens = persistencia.carregarRelatorioEstoque();
        double quantidadeTotal = itens.stream()
                .mapToDouble(InterfacePersistencia.EstoqueRelatorio::quantidade)
                .sum();

        long criticos = itens.stream()
                .filter(item -> !"Normal".equals(item.situacao()))
                .count();

        labelResumo1.setText("Itens cadastrados");
        labelResumo2.setText("Saldo total");
        labelResumo3.setText("Itens críticos");
        labelValor1.setText(String.valueOf(itens.size()));
        labelValor2.setText(formatarQuantidade(quantidadeTotal));
        labelValor3.setText(String.valueOf(criticos));

        configurarCabecalho("Produto", "Quantidade", "Situação");
        tabela.getItems().setAll(
                itens.stream()
                        .map(item -> new ObjetoRelatorio(
                                item.produto(),
                                formatarQuantidade(item.quantidade()) + " " + item.unidadeMedida(),
                                item.situacao()
                        ))
                        .toList()
        );

        if (tabela.getItems().isEmpty()) {
            labelMensagem.setText("Nenhum dado encontrado.");
        }
    }

    private Periodo lerPeriodo() {
        LocalDate inicio;
        LocalDate fim;

        try {
            inicio = LocalDate.parse(campoDataInicial.getText().trim(), DATA_INPUT);
            fim = LocalDate.parse(campoDataFinal.getText().trim(), DATA_INPUT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Informe as datas no formato DD/MM/AAAA.");
        }

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final.");
        }

        return new Periodo(inicio, fim);
    }

    private void configurarTabela() {
        coluna1.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValor1()));
        coluna2.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValor2()));
        coluna3.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValor3()));

        configurarAlinhamento(coluna1, Pos.CENTER_LEFT);
        configurarAlinhamento(coluna2, Pos.CENTER);
        configurarAlinhamento(coluna3, Pos.CENTER);
    }

    private void configurarAlinhamento(TableColumn<ObjetoRelatorio, String> coluna, Pos alinhamento) {
        coluna.setCellFactory(col -> new TableCell<>() {
            {
                setAlignment(alinhamento);
            }

            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
            }
        });
    }

    private void configurarCabecalho(String titulo1, String titulo2, String titulo3) {
        coluna1.setText(titulo1);
        coluna2.setText(titulo2);
        coluna3.setText(titulo3);
    }

    private void limparResumo() {
        labelResumo1.setText("-");
        labelResumo2.setText("-");
        labelResumo3.setText("-");
        labelValor1.setText("-");
        labelValor2.setText("-");
        labelValor3.setText("-");
    }

    private void atualizarCartoes() {
        botaoVendas.getStyleClass().remove("report-card-active");
        botaoItensMaisVendidos.getStyleClass().remove("report-card-active");
        botaoConsumo.getStyleClass().remove("report-card-active");
        botaoEstoque.getStyleClass().remove("report-card-active");

        switch (tipoAtual) {
            case VENDAS -> botaoVendas.getStyleClass().add("report-card-active");
            case ITENS_MAIS_VENDIDOS -> botaoItensMaisVendidos.getStyleClass().add("report-card-active");
            case CONSUMO_POR_PERIODO -> botaoConsumo.getStyleClass().add("report-card-active");
            case ESTOQUE -> botaoEstoque.getStyleClass().add("report-card-active");
        }
    }

    private String formatarMoeda(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    private String formatarQuantidade(double quantidade) {
        if (Math.abs(quantidade - Math.rint(quantidade)) < 0.000001) {
            return String.valueOf((long) Math.rint(quantidade));
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", quantidade);
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {
    }
}
