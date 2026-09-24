package App.Validacao;

import Model.Atendimento.Mesa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do MesaValidator (UC03).
 *
 * Cobre:
 *  - Fluxo principal: número positivo e único → válido
 *  - FA01: número não positivo (0 ou negativo) → inválido
 *  - FA02: número já existente → inválido
 */
class MesaValidatorTest {

    // ── Fluxo principal ──────────────────────────────────────────────────────

    @Test
    void numeroPositivoEUnicoEhValido() {
        List<Mesa> mesas = List.of(new Mesa(1), new Mesa(2));
        MesaValidator.Resultado resultado = MesaValidator.validar(3, mesas);

        assertTrue(resultado.valido(), "Número positivo e único deve ser aceito");
        assertEquals("", resultado.mensagem());
    }

    @Test
    void listaMesasVaziaAceitaQualquerPositivo() {
        MesaValidator.Resultado resultado = MesaValidator.validar(99, List.of());

        assertTrue(resultado.valido());
    }

    // ── FA01 – número não positivo ────────────────────────────────────────────

    @Test
    void numeroZeroEhInvalido() {
        MesaValidator.Resultado resultado = MesaValidator.validar(0, List.of());

        assertFalse(resultado.valido(), "Zero não deve ser aceito");
        assertFalse(resultado.mensagem().isBlank(), "Deve retornar mensagem de erro");
    }

    @Test
    void numeroNegativoEhInvalido() {
        MesaValidator.Resultado resultado = MesaValidator.validar(-5, List.of());

        assertFalse(resultado.valido(), "Número negativo não deve ser aceito");
        assertFalse(resultado.mensagem().isBlank());
    }

    // ── FA02 – número duplicado ───────────────────────────────────────────────

    @Test
    void numeroDuplicadoEhInvalido() {
        List<Mesa> mesas = List.of(new Mesa(1), new Mesa(5), new Mesa(10));
        MesaValidator.Resultado resultado = MesaValidator.validar(5, mesas);

        assertFalse(resultado.valido(), "Número já existente não deve ser aceito");
        assertTrue(resultado.mensagem().contains("5"), "Mensagem deve mencionar o número duplicado");
    }

    // ── validarTexto ─────────────────────────────────────────────────────────

    @Test
    void textoVazioEhInvalido() {
        MesaValidator.Resultado resultado = MesaValidator.validarTexto("  ", List.of());

        assertFalse(resultado.valido());
    }

    @Test
    void textoNaoNumericoEhInvalido() {
        MesaValidator.Resultado resultado = MesaValidator.validarTexto("abc", List.of());

        assertFalse(resultado.valido());
    }

    @Test
    void textoNumericoPositivoUnicoEhValido() {
        List<Mesa> mesas = new ArrayList<>();
        mesas.add(new Mesa(1));
        MesaValidator.Resultado resultado = MesaValidator.validarTexto("7", mesas);

        assertTrue(resultado.valido());
    }

    @Test
    void textoComEspacosEhTratadoCorretamente() {
        MesaValidator.Resultado resultado = MesaValidator.validarTexto("  4  ", List.of());

        assertTrue(resultado.valido(), "Texto com espaços deve ser tratado com trim()");
    }
}
