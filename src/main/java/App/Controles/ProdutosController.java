package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Produtos.*;
import Model.Produtos.Alimentos.Alimentos;
import Model.Produtos.Alimentos.Refeicao;
import Model.Produtos.Alimentos.TiraGosto;
import Model.Produtos.Bedidas.Bebidas;
import Model.Produtos.Bedidas.ComAlcool;
import Model.Produtos.Bedidas.SemAlcool;
import Model.Produtos.Outros.Descartaveis;
import Model.Produtos.Outros.Outros;
import Model.Produtos.Outros.Servico;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class ProdutosController extends BaseController {

    @FXML private ListView<Produto> listaProdutos;
    @FXML private Label labelFormulario;
    @FXML private TextField campoNome;
    @FXML private TextField campoDescricao;
    @FXML private TextField campoPreco;
    @FXML private TextField campoEstoque;
    @FXML private Label labelEstoque;
    @FXML private ComboBox<String> comboTipoPrincipal;
    @FXML private ComboBox<String> comboSubTipo;

    private InterfacePersistencia persistenceService;
    private List<Produto> listaProdutosCentral;
    private ObservableList<Produto> observableListProdutos;
    private Produto produtoSelecionado = null;

    public void inicializar(List<Produto> listaProdutosCentral, InterfacePersistencia service) {
        this.listaProdutosCentral = listaProdutosCentral;
        this.persistenceService = service;

        comboTipoPrincipal.getItems().setAll("Alimento", "Bebida", "Outros");

        comboTipoPrincipal.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            atualizarComboSubTipo(newVal);
        });

        comboSubTipo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            ajustarVisibilidadeEstoque(newVal);
        });

        this.observableListProdutos = FXCollections.observableArrayList(listaProdutosCentral);
        this.listaProdutos.setItems(observableListProdutos);

        this.listaProdutos.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> selecionarProduto(newValue)
        );

        limparCampos();
    }

    private void ajustarVisibilidadeEstoque(String subTipo) {
        boolean ehServico = "Serviço".equals(subTipo);

        if (ehServico) {
            labelEstoque.setVisible(false);
            campoEstoque.setVisible(false);
            campoEstoque.setText("9999");
        } else {
            labelEstoque.setVisible(true);
            campoEstoque.setVisible(true);
            if ("9999".equals(campoEstoque.getText())) {
                campoEstoque.setText("");
            }
        }
    }

    private void atualizarComboSubTipo(String principal) {
        comboSubTipo.getItems().clear();
        campoEstoque.setDisable(false);

        if (principal == null) {
            comboSubTipo.setDisable(true);
            return;
        }
        comboSubTipo.setDisable(false);

        if (principal.equals("Alimento")) {
            comboSubTipo.getItems().setAll("Refeição", "Tira Gosto");
        } else if (principal.equals("Bebida")) {
            comboSubTipo.getItems().setAll("Com Álcool", "Sem Álcool");
        } else if (principal.equals("Outros")) {
            comboSubTipo.getItems().setAll("Serviço", "Descartável");
        }
    }

    private void selecionarProduto(Produto p) {
        this.produtoSelecionado = p;
        if (p == null) {
            limparCampos();
            return;
        }

        labelFormulario.setText("Editando: " + p.getNome());
        campoNome.setText(p.getNome());
        campoDescricao.setText(p.getDescricao());
        campoPreco.setText(String.format("%.2f", p.getPreco()));
        campoEstoque.setText(String.valueOf(p.getEstoque()));


        if (p instanceof Bebidas) {
            comboTipoPrincipal.setValue("Bebida");
            if (p instanceof ComAlcool) comboSubTipo.setValue("Com Álcool");
            if (p instanceof SemAlcool) comboSubTipo.setValue("Sem Álcool");
        } else if (p instanceof Alimentos) {
            comboTipoPrincipal.setValue("Alimento");
            if (p instanceof Refeicao) comboSubTipo.setValue("Refeição");
            if (p instanceof TiraGosto) comboSubTipo.setValue("Tira Gosto");
        }
        else if (p instanceof Outros) {
            comboTipoPrincipal.setValue("Outros");
            if (p instanceof Servico) {
                comboSubTipo.setValue("Serviço");
            } else if (p instanceof Descartaveis) {
                comboSubTipo.setValue("Descartável");
            }
        }
        ajustarVisibilidadeEstoque(comboSubTipo.getValue());
    }

    private Produto criarInstanciaDaClasse(String tipo, String nome, String desc, double preco, int est, String grupo) {
        switch (tipo) {
            case "Com Álcool": return new ComAlcool(nome, desc, preco, est, grupo);
            case "Sem Álcool": return new SemAlcool(nome, desc, preco, est, grupo);
            case "Refeição":   return new Refeicao(nome, desc, preco, est, grupo);
            case "Tira Gosto": return new TiraGosto(nome, desc, preco, est, grupo);
            case "Serviço":

                return new Servico(nome, desc, preco, null, grupo);
            case "Descartável":
                return new Descartaveis(nome, desc, preco, est, grupo);

            default:
                return new Refeicao(nome, desc, preco, est, grupo);
        }
    }

    @FXML
    private void salvar() {
        try {
            String nome = campoNome.getText();
            String desc = campoDescricao.getText();
            double preco = Double.parseDouble(campoPreco.getText().replace(",", "."));
            int est = Integer.parseInt(campoEstoque.getText());

            String subTipo = comboSubTipo.getValue();

            String grupo = subTipo;

            if (nome.isEmpty() || subTipo == null) {
                mostrarAlerta("Erro", "Preencha todos os campos obrigatórios.");
                return;
            }

            if (this.produtoSelecionado == null) {
                Produto novoProduto = criarInstanciaDaClasse(subTipo, nome, desc, preco, est, grupo);
                this.listaProdutosCentral.add(novoProduto);
                this.observableListProdutos.add(novoProduto);
            } else {
                this.produtoSelecionado.setNome(nome);
                this.produtoSelecionado.setDescricao(desc);
                this.produtoSelecionado.setPreco(preco);
                this.produtoSelecionado.setEstoque(est);
                this.produtoSelecionado.setCategoriaNome(grupo);

                this.listaProdutos.refresh();
            }

            this.persistenceService.salvarProdutos(this.listaProdutosCentral);
            limparCampos();

        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Preço e Estoque devem ser números.");
        }
    }


    @FXML
    private void removerProduto() {
        if (this.produtoSelecionado == null) {
            mostrarAlerta("Erro", "Nenhum produto selecionado para remover.");
            return;
        }
        this.listaProdutosCentral.remove(this.produtoSelecionado);
        this.observableListProdutos.remove(this.produtoSelecionado);
        this.persistenceService.salvarProdutos(this.listaProdutosCentral);
        limparCampos();
    }

    @FXML
    private void limparCampos() {
        this.produtoSelecionado = null;
        labelFormulario.setText("Adicionar Novo Produto");
        campoNome.clear();
        campoDescricao.clear();
        campoPreco.clear();
        campoEstoque.clear();
        comboTipoPrincipal.setValue(null);
        comboSubTipo.setValue(null);
        comboSubTipo.setDisable(true);

        labelEstoque.setVisible(true);
        campoEstoque.setVisible(true);

    }
}