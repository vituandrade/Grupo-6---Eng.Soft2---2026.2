package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controla o diálogo de abertura de uma nova comanda (UC04).
 */
public class AbrirComandaController extends BaseController {

    @FXML private ComboBox<String> comboTipoAtendimento;
    @FXML private ComboBox<Mesa> comboMesa;
    @FXML private TextField campoNomeCliente;
    @FXML private Label labelMesa;
    @FXML private Label labelNomeCliente;
    @FXML private Label labelErro;
    @FXML private Button botaoAbrir;

    private List<Mesa> mesas;
    private Comanda comandaCriada;
    private Mesa mesaSelecionada;
    private boolean confirmada;

    public void inicializar(List<Mesa> mesas) {
        this.mesas = mesas;
        this.confirmada = false;
        this.comandaCriada = null;
        this.mesaSelecionada = null;

        comboTipoAtendimento.getItems().setAll(
                "Mesa",
                "Cliente sem mesa"
        );

        comboTipoAtendimento.getSelectionModel().select("Mesa");

        comboTipoAtendimento.valueProperty().addListener(
                (obs, antigo, novo) -> atualizarCampos()
        );

        atualizarMesasLivres();
        atualizarCampos();
    }

    public void inicializar(List<Mesa> mesas, Mesa mesaPreSelecionada) {
        inicializar(mesas);

        if (mesaPreSelecionada != null && !mesaPreSelecionada.isOcupada()) {
            comboMesa.getSelectionModel().select(mesaPreSelecionada);
        }
    }

    private void atualizarMesasLivres() {
        comboMesa.getItems().clear();

        if (mesas == null) {
            return;
        }

        for (Mesa mesa : mesas) {
            if (!mesa.isOcupada() && !mesa.temComandaAberta()) {
                comboMesa.getItems().add(mesa);
            }
        }

        if (!comboMesa.getItems().isEmpty()) {
            comboMesa.getSelectionModel().selectFirst();
        }
    }

    private void atualizarCampos() {

        boolean porMesa =
                "Mesa".equals(comboTipoAtendimento.getValue());

        labelMesa.setVisible(porMesa);
        labelMesa.setManaged(porMesa);

        comboMesa.setVisible(porMesa);
        comboMesa.setManaged(porMesa);


        labelNomeCliente.setVisible(true);
        labelNomeCliente.setManaged(true);

        campoNomeCliente.setVisible(true);
        campoNomeCliente.setManaged(true);

        limparErro();
}

    @FXML
    private void confirmar() {
        limparErro();

        if ("Mesa".equals(comboTipoAtendimento.getValue())) {

            Mesa mesa =
                    comboMesa.getSelectionModel().getSelectedItem();

            if (mesa == null) {
                atualizarMesasLivres();
                mostrarErro(
                        "Não há mesas livres disponíveis para abrir a comanda."
                );
                return;
            }

            // Revalidação antes da criação.
            if (mesa.isOcupada() || mesa.temComandaAberta()) {
                atualizarMesasLivres();

                mostrarErro(
                        "A mesa selecionada não está mais livre. Selecione outra mesa."
                );

                return;
            }

            String nome =
                    campoNomeCliente.getText() == null
                            ? ""
                            : campoNomeCliente.getText().trim();

            if (nome.isEmpty()) {
                nome = null;
            }

            comandaCriada = new Comanda();

            if (nome != null) {
                comandaCriada.setClienteNome(nome);
            }

            try {
                mesa.adicionarComanda(comandaCriada);

            } catch (IllegalStateException ex) {
                comandaCriada = null;

                mostrarErro(
                        "A mesa já possui uma comanda aberta. Selecione outra mesa."
                );

                atualizarMesasLivres();
                return;
            }

            mesaSelecionada = mesa;

        } else {

            String nome =
                    campoNomeCliente.getText() == null
                            ? ""
                            : campoNomeCliente.getText().trim();

            if (nome.isEmpty()) {
                mostrarErro("Informe o nome do cliente.");
                campoNomeCliente.requestFocus();
                return;
            }

            comandaCriada = new Comanda();
            comandaCriada.setClienteNome(nome);

            mesaSelecionada = null;
        }

        confirmada = true;
        fechar();
    }

    @FXML
    private void cancelar() {
        confirmada = false;
        comandaCriada = null;
        mesaSelecionada = null;
        fechar();
    }

    private void mostrarErro(String mensagem) {
        labelErro.setText(mensagem);
        labelErro.setVisible(true);
        labelErro.setManaged(true);
    }

    private void limparErro() {
        labelErro.setText("");
        labelErro.setVisible(false);
        labelErro.setManaged(false);
    }

    private void fechar() {
        Stage stage =
                (Stage) botaoAbrir.getScene().getWindow();

        stage.close();
    }

    public boolean isConfirmada() {
        return confirmada;
    }

    public Comanda getComandaCriada() {
        return comandaCriada;
    }

    public Mesa getMesaSelecionada() {
        return mesaSelecionada;
    }
}