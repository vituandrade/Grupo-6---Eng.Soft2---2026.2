package App.Estoque;

import App.Validacao.MovimentacaoEstoqueValidator;
import Model.Estoque.TipoMovimentacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovimentacaoEstoqueValidatorTest {

    @Test
    void aceitaEntradaDeItemExistente() {
        var resultado = MovimentacaoEstoqueValidator.validar(
                "Arroz", false, "Entrada", "2,5", "kg"
        );

        assertTrue(resultado.valido());
        assertEquals(TipoMovimentacao.ENTRADA, resultado.tipo());
        assertEquals(2.5, resultado.quantidade());
    }

    @Test
    void rejeitaCamposObrigatoriosEQuantidadeInvalida() {
        var resultado = MovimentacaoEstoqueValidator.validar("", false, null, "0", "");

        assertFalse(resultado.valido());
        assertTrue(resultado.possuiErro(MovimentacaoEstoqueValidator.ITEM));
        assertTrue(resultado.possuiErro(MovimentacaoEstoqueValidator.TIPO));
        assertTrue(resultado.possuiErro(MovimentacaoEstoqueValidator.QUANTIDADE));
    }

    @Test
    void exigeUnidadeParaItemNovo() {
        var resultado = MovimentacaoEstoqueValidator.validar(
                "Farinha", true, "Entrada", "10", ""
        );

        assertFalse(resultado.valido());
        assertTrue(resultado.possuiErro(MovimentacaoEstoqueValidator.UNIDADE));
    }

    @Test
    void primeiroRegistroDeItemNovoPrecisaSerEntrada() {
        var resultado = MovimentacaoEstoqueValidator.validar(
                "Farinha", true, "Saída", "1", "kg"
        );

        assertFalse(resultado.valido());
        assertTrue(resultado.possuiErro(MovimentacaoEstoqueValidator.TIPO));
    }
}
