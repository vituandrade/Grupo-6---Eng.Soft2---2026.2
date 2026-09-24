package App.Validacao;

import Model.Atendimento.Mesa;
import java.util.List;

/**
 * Validações de negócio para o cadastro de mesas (UC03).
 * FA01 – número não positivo  → mensagem de erro.
 * FA02 – número já existente  → mensagem de erro.
 */
public class MesaValidator {

    private MesaValidator() { /* utilitária */ }

    /**
     * Valida o número informado para uma nova mesa.
     *
     * @param numero       número digitado pelo atendente
     * @param mesasExistentes lista atual de mesas já cadastradas
     * @return {@link Resultado} com {@code valido=true} em caso de sucesso,
     *         ou {@code valido=false} com a mensagem de erro correspondente.
     */
    public static Resultado validar(int numero, List<Mesa> mesasExistentes) {

        // FA01 – número deve ser positivo
        if (numero <= 0) {
            return new Resultado(false, "O número da mesa deve ser um valor positivo (maior que zero).");
        }

        // FA02 – número deve ser único
        boolean duplicado = mesasExistentes.stream()
                .anyMatch(m -> m.getNumMesa() == numero);
        if (duplicado) {
            return new Resultado(false, "Já existe uma mesa com o número " + numero + ". Informe um número único.");
        }

        return new Resultado(true, "");
    }

    /**
     * Conveniência: valida uma String digitada pelo usuário antes de parsear.
     * Retorna FA01 se o texto não for um inteiro positivo.
     */
    public static Resultado validarTexto(String texto, List<Mesa> mesasExistentes) {
        if (texto == null || texto.isBlank()) {
            return new Resultado(false, "O número da mesa não pode estar vazio.");
        }
        int numero;
        try {
            numero = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return new Resultado(false, "O número da mesa deve ser um valor numérico inteiro positivo.");
        }
        return validar(numero, mesasExistentes);
    }

    /**
     * Resultado imutável de uma validação.
     *
     * @param valido    {@code true} quando a entrada é válida
     * @param mensagem  mensagem de erro (vazia quando válido)
     */
    public record Resultado(boolean valido, String mensagem) {}
}
