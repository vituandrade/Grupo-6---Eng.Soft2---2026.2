package App.Validacao;

import Model.Usuarios.Interno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginValidatorTest {

    @Test
    void rejeitaUsuarioESenhaVazios() {
        LoginValidator.Resultado resultado = LoginValidator.validar("", "");

        assertFalse(resultado.valido());
        assertTrue(resultado.usuarioVazio());
        assertTrue(resultado.senhaVazia());
    }

    @Test
    void rejeitaSenhaVazia() {
        LoginValidator.Resultado resultado = LoginValidator.validar("admin", " ");

        assertFalse(resultado.valido());
        assertTrue(resultado.senhaVazia());
    }

    @Test
    void aceitaCamposPreenchidos() {
        assertTrue(LoginValidator.validar("admin", "admin").valido());
    }

    @Test
    void autenticaSomenteCredenciaisCorretas() {
        Interno usuario = new Interno("admin", "admin");

        assertTrue(usuario.autenticar("admin", "admin"));
        assertFalse(usuario.autenticar("admin", "senha-errada"));
    }
}
