package App.Mesas;

import Model.Atendimento.Comanda;
import Model.Atendimento.EstadoMesa;
import Model.Atendimento.Mesa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do modelo Mesa (UC03 – estados e transições).
 *
 * Cobre:
 *  - Estado inicial: Livre
 *  - FA03: Ocupada (com comanda aberta)
 *  - FA03: Aguardando Fechamento
 */
class MesaTest {

    // ── Estado inicial ───────────────────────────────────────────────────────

    @Test
    void mesaNovaEstaLivre() {
        Mesa mesa = new Mesa(1);

        assertFalse(mesa.isOcupada(),              "Mesa nova não deve estar ocupada");
        assertFalse(mesa.isAguardandoPagamento(),  "Mesa nova não deve estar aguardando pagamento");
        assertFalse(mesa.temComandaAberta(),       "Mesa nova não deve ter comanda aberta");
    }

    // ── Estado Ocupada ───────────────────────────────────────────────────────

    @Test
    void mesaComComandaAbertaEstaOcupada() {
        Mesa mesa = new Mesa(2);
        Comanda comanda = new Comanda();
        mesa.adicionarComanda(comanda);

        assertTrue(mesa.isOcupada(),        "Mesa com comanda aberta deve estar ocupada");
        assertTrue(mesa.temComandaAberta(), "Mesa deve reconhecer a comanda como aberta");
        assertFalse(mesa.isAguardandoPagamento());
    }

    @Test
    void mesaNaoPodeAdicionarSegundaComandaAberta() {
        Mesa mesa = new Mesa(3);
        mesa.adicionarComanda(new Comanda());

        assertThrows(IllegalStateException.class, () -> mesa.adicionarComanda(new Comanda()),
                "Deve lançar exceção ao tentar adicionar comanda em mesa já ocupada");
    }

    @Test
    void adicionarComandaNulaLancaExcecao() {
        Mesa mesa = new Mesa(4);

        assertThrows(IllegalArgumentException.class, () -> mesa.adicionarComanda(null));
    }

    // ── Estado Aguardando Fechamento ─────────────────────────────────────────

    @Test
    void mesaAguardandoPagamentoNaoEstaLivre() {
        Mesa mesa = new Mesa(5);
        Comanda comanda = new Comanda();
        mesa.adicionarComanda(comanda);
        mesa.setAguardandoPagamento(true);

        assertTrue(mesa.isAguardandoPagamento(), "Mesa deve estar no estado aguardando pagamento");
        assertTrue(mesa.isOcupada(),             "Mesa com comanda ainda está ocupada");
    }

    @Test
    void mesaLiberadaAposRemoverComanda() {
        Mesa mesa = new Mesa(6);
        Comanda comanda = new Comanda();
        mesa.adicionarComanda(comanda);
        mesa.removerComanda(comanda);
        mesa.setAguardandoPagamento(false);

        assertFalse(mesa.isOcupada(),             "Mesa sem comandas deve ficar livre");
        assertFalse(mesa.isAguardandoPagamento(), "Estado aguardando deve ser limpo");
    }

    // ── Transição de estados (regra de negócio completa) ─────────────────────

    @Test
    void transicaoCompletaLivreOcupadaAguardandoLivre() {
        Mesa mesa = new Mesa(7);

        // 1. Livre
        assertFalse(mesa.isOcupada());

        // 2. Ocupada
        Comanda comanda = new Comanda();
        mesa.adicionarComanda(comanda);
        assertTrue(mesa.isOcupada());

        // 3. Aguardando Fechamento
        comanda.fechar();
        mesa.setAguardandoPagamento(true);
        assertTrue(mesa.isAguardandoPagamento());

        // 4. Livre novamente
        mesa.removerComanda(comanda);
        mesa.setAguardandoPagamento(false);
        assertFalse(mesa.isOcupada());
        assertFalse(mesa.isAguardandoPagamento());
    }

    @Test
    void naoPermitePularEstadoIntermediario() {
        Mesa mesa = new Mesa(8);

        assertThrows(
                IllegalStateException.class,
                () -> mesa.alterarEstado(EstadoMesa.AGUARDANDO_FECHAMENTO)
        );
        assertEquals(EstadoMesa.LIVRE, mesa.getEstado());
    }

    @Test
    void naoPermiteLiberarMesaComComandaAberta() {
        Mesa mesa = new Mesa(9);
        mesa.adicionarComanda(new Comanda());
        mesa.alterarEstado(EstadoMesa.AGUARDANDO_FECHAMENTO);

        assertThrows(
                IllegalStateException.class,
                () -> mesa.alterarEstado(EstadoMesa.LIVRE)
        );
        assertEquals(EstadoMesa.AGUARDANDO_FECHAMENTO, mesa.getEstado());
    }
}
