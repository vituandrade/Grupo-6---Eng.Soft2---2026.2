package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Estoque.ItemEstoque;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumoPainelInicialTest {

    @Test
    void calculaIndicadoresComDadosReaisDaSessao() {
        Mesa mesaLivre = new Mesa(1);
        Mesa mesaOcupada = new Mesa(2);
        Mesa mesaAguardando = new Mesa(3);

        Comanda comandaDaMesa = novaComanda(101, "Cliente da mesa", 1_000);
        mesaOcupada.adicionarComanda(comandaDaMesa);
        mesaAguardando.setAguardandoPagamento(true);

        Comanda clienteSemMesa = novaComanda(102, "Maria", 2_000);
        List<ItemEstoque> estoque = List.of(
                new ItemEstoque(1, "Arroz", "kg", 20),
                new ItemEstoque(2, "Farinha", "kg", 5),
                new ItemEstoque(3, "Óleo", "L", 0)
        );

        ResumoPainelInicial resumo = ResumoPainelInicial.calcular(
                List.of(mesaLivre, mesaOcupada, mesaAguardando),
                List.of(clienteSemMesa),
                estoque
        );

        assertEquals(1, resumo.mesasLivres());
        assertEquals(2, resumo.comandasAbertas());
        assertEquals(2, resumo.itensComEstoqueBaixo());
        assertEquals("Maria", resumo.comandasEmAndamento().get(0).atendimento());
        assertEquals("Mesa 2", resumo.comandasEmAndamento().get(1).atendimento());
    }

    @Test
    void exibeSomenteAsTresComandasMaisRecentes() {
        List<Comanda> comandas = List.of(
                novaComanda(201, "Primeira", 1_000),
                novaComanda(202, "Segunda", 2_000),
                novaComanda(203, "Terceira", 3_000),
                novaComanda(204, "Quarta", 4_000)
        );

        ResumoPainelInicial resumo = ResumoPainelInicial.calcular(
                List.of(), comandas, List.of()
        );

        assertEquals(4, resumo.comandasAbertas());
        assertEquals(3, resumo.comandasEmAndamento().size());
        assertEquals(204, resumo.comandasEmAndamento().get(0).comanda().getId());
        assertEquals(202, resumo.comandasEmAndamento().get(2).comanda().getId());
    }

    private Comanda novaComanda(int id, String cliente, long instante) {
        return new Comanda(id, cliente, new Date(instante));
    }
}
