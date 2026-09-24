package App.Controles;

import Model.Atendimento.Comanda;
import Model.Atendimento.Mesa;
import Model.Estoque.ItemEstoque;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reúne os dados exibidos no painel inicial.
 *
 * Manter os cálculos fora do controller facilita a leitura da tela e permite
 * testar os números sem precisar iniciar o JavaFX.
 */
public record ResumoPainelInicial(
        long mesasLivres,
        long comandasAbertas,
        long itensComEstoqueBaixo,
        List<ComandaEmAndamento> comandasEmAndamento
) {

    private static final int LIMITE_COMANDAS_NO_PAINEL = 3;

    public static ResumoPainelInicial calcular(
            List<Mesa> mesas,
            List<Comanda> comandasSemMesa,
            List<ItemEstoque> itensEstoque
    ) {
        List<Mesa> mesasSeguras = mesas == null ? List.of() : mesas;
        List<Comanda> comandasSemMesaSeguras = comandasSemMesa == null
                ? List.of()
                : comandasSemMesa;
        List<ItemEstoque> estoqueSeguro = itensEstoque == null ? List.of() : itensEstoque;

        long mesasLivres = mesasSeguras.stream()
                .filter(mesa -> !mesa.isOcupada() && !mesa.isAguardandoPagamento())
                .count();

        List<ComandaEmAndamento> comandasAbertas = new ArrayList<>();
        for (Mesa mesa : mesasSeguras) {
            mesa.getComandas().stream()
                    .filter(comanda -> !comanda.isFechada())
                    .map(comanda -> new ComandaEmAndamento(comanda, "Mesa " + mesa.getNumMesa()))
                    .forEach(comandasAbertas::add);
        }

        comandasSemMesaSeguras.stream()
                .filter(comanda -> !comanda.isFechada())
                .map(comanda -> new ComandaEmAndamento(comanda, comanda.getClienteNome()))
                .forEach(comandasAbertas::add);

        comandasAbertas.sort(
                Comparator.comparing(
                        (ComandaEmAndamento item) -> item.comanda().getDataAbertura()
                ).reversed()
        );

        long estoqueBaixo = estoqueSeguro.stream()
                .filter(ItemEstoque::precisaReposicao)
                .count();

        return new ResumoPainelInicial(
                mesasLivres,
                comandasAbertas.size(),
                estoqueBaixo,
                List.copyOf(comandasAbertas.stream()
                        .limit(LIMITE_COMANDAS_NO_PAINEL)
                        .toList())
        );
    }

    public record ComandaEmAndamento(Comanda comanda, String atendimento) {
    }
}
