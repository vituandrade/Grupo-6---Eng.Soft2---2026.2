package App.Controles;

import App.Persistencia.InterfacePersistencia;
import App.Persistencia.DatabaseService;
import App.Persistencia.PersistenceService;
import App.Validacao.LoginValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import Model.Usuarios.*;
import Model.Produtos.Produto;
import Model.Sistema.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoSenha;
    @FXML
    private Label mensagemErro;

    private InterfacePersistencia persistenceService;
    private Config config;

    private List<Usuario> listaDeUsuarios = new ArrayList<>();
    private List<Produto> listaDeProdutos = new ArrayList<>();

    @FXML
    public void initialize() {
        this.persistenceService = new DatabaseService();
        this.config = persistenceService.carregarConfig();
        carregarUsuarios();
        carregarProdutos();

        campoUsuario.textProperty().addListener((obs, anterior, atual) -> {
            if (atual != null && !atual.isBlank()) {
                limparErro(campoUsuario);
            }
        });
        campoSenha.textProperty().addListener((obs, anterior, atual) -> {
            if (atual != null && !atual.isBlank()) {
                limparErro(campoSenha);
            }
        });
    }

    private void carregarUsuarios() {
        this.listaDeUsuarios = persistenceService.carregarUsuarios();
    }

    private void carregarProdutos() {
        this.listaDeProdutos.clear();
        this.listaDeProdutos = persistenceService.carregarProdutos();
    }

    @FXML
    private void fazerLogin(ActionEvent event) {
        String nome = campoUsuario.getText().trim();
        String senha = campoSenha.getText();

        limparErros();
        LoginValidator.Resultado validacao = LoginValidator.validar(nome, senha);

        if (!validacao.valido()) {
            marcarInvalido(campoUsuario, validacao.usuarioVazio());
            marcarInvalido(campoSenha, validacao.senhaVazia());
            mensagemErro.setText("Preencha os campos obrigatórios.");
            (validacao.usuarioVazio() ? campoUsuario : campoSenha).requestFocus();
            return;
        }

        Usuario usuarioEncontrado = null;

        for (Usuario u : this.listaDeUsuarios) {
            if (u.autenticar(nome, senha)) {
                usuarioEncontrado = u;
                break;
            }
        }

        if (usuarioEncontrado != null) {
            abrirTelaMesas(usuarioEncontrado);
        } else {
            mensagemErro.setText("Credenciais inválidas.");
            marcarInvalido(campoUsuario, true);
            marcarInvalido(campoSenha, true);
            campoSenha.clear();
            campoSenha.requestFocus();
        }
    }

    private void limparErros() {
        mensagemErro.setText("");
        marcarInvalido(campoUsuario, false);
        marcarInvalido(campoSenha, false);
    }

    private void limparErro(Control campo) {
        campo.getStyleClass().remove("campo-invalido");
        if (!campoUsuario.getText().isBlank() && !campoSenha.getText().isBlank()) {
            mensagemErro.setText("");
        }
    }

    private void marcarInvalido(Control campo, boolean invalido) {
        campo.getStyleClass().remove("campo-invalido");
        if (invalido) {
            campo.getStyleClass().add("campo-invalido");
        }
    }

    private void abrirTelaMesas(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/App/Mesa.fxml"));
            Parent root = loader.load();

            MesaController mesaController = loader.getController();

            mesaController.setUsuarioLogado(
                    usuario,
                    this.listaDeProdutos,
                    this.config,
                    this.persistenceService
            );

            Stage stage = (Stage) campoUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle("Sistema Restaurante");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
