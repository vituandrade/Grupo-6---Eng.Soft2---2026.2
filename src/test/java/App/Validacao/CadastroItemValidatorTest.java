package App.Validacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CadastroItemValidatorTest {

    @Test
    void aceitaCadastroValidoComDescricaoOpcionalEPrecoComVirgula() {
        var resultado = CadastroItemValidator.validar(
                "X-Burger", "Lanche", "", "24,90", "Disponível"
        );

        assertTrue(resultado.valido());
        assertEquals(24.90, resultado.preco(), 0.001);
        assertTrue(resultado.disponivel());
    }

    @Test
    void recusaCamposObrigatoriosVaziosEPrecoZero() {
        var resultado = CadastroItemValidator.validar("", null, "", "0", null);

        assertFalse(resultado.valido());
        assertTrue(resultado.possuiErro(CadastroItemValidator.NOME));
        assertTrue(resultado.possuiErro(CadastroItemValidator.CATEGORIA));
        assertTrue(resultado.possuiErro(CadastroItemValidator.PRECO));
        assertTrue(resultado.possuiErro(CadastroItemValidator.DISPONIBILIDADE));
    }

    @Test
    void recusaPrecoNaoNumerico() {
        var resultado = CadastroItemValidator.validar(
                "Pudim", "Sobremesa", "", "dez reais", "Indisponível"
        );

        assertFalse(resultado.valido());
        assertTrue(resultado.possuiErro(CadastroItemValidator.PRECO));
    }
}
