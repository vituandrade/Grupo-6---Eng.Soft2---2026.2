package App.Validacao;

public final class LoginValidator {

    private LoginValidator() {
    }

    public static Resultado validar(String usuario, String senha) {
        boolean usuarioVazio = usuario == null || usuario.isBlank();
        boolean senhaVazia = senha == null || senha.isBlank();
        return new Resultado(!usuarioVazio && !senhaVazia, usuarioVazio, senhaVazia);
    }

    public record Resultado(boolean valido, boolean usuarioVazio, boolean senhaVazia) {
    }
}
