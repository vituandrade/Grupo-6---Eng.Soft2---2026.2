package App.Validacao;

import Model.Estoque.TipoMovimentacao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MovimentacaoEstoqueValidator {

    public static final String ITEM = "item";
    public static final String TIPO = "tipo";
    public static final String QUANTIDADE = "quantidade";
    public static final String UNIDADE = "unidade";

    private MovimentacaoEstoqueValidator() {
    }

    public static Resultado validar(
            String nomeItem,
            boolean itemNovo,
            String tipoInformado,
            String quantidadeInformada,
            String unidadeMedida
    ) {
        Map<String, String> erros = new LinkedHashMap<>();
        String nomeNormalizado = nomeItem == null ? "" : nomeItem.trim();
        String unidadeNormalizada = unidadeMedida == null ? "" : unidadeMedida.trim();
        TipoMovimentacao tipo = TipoMovimentacao.porDescricao(tipoInformado);

        if (nomeNormalizado.isEmpty()) {
            erros.put(ITEM, "Selecione ou informe um item.");
        }
        if (tipo == null) {
            erros.put(TIPO, "Selecione Entrada ou Saída.");
        }

        Double quantidade = null;
        try {
            String valorNormalizado = quantidadeInformada == null
                    ? ""
                    : quantidadeInformada.trim().replace(",", ".");
            quantidade = Double.parseDouble(valorNormalizado);
            if (!Double.isFinite(quantidade) || quantidade <= 0) {
                erros.put(QUANTIDADE, "A quantidade deve ser maior que zero.");
            }
        } catch (NumberFormatException e) {
            erros.put(QUANTIDADE, "Informe uma quantidade numérica válida.");
        }

        if (itemNovo && unidadeNormalizada.isEmpty()) {
            erros.put(UNIDADE, "Informe a unidade de medida do item novo.");
        }
        if (itemNovo && tipo == TipoMovimentacao.SAIDA) {
            erros.put(TIPO, "O primeiro registro de um item deve ser uma entrada.");
        }

        return new Resultado(
                nomeNormalizado,
                itemNovo,
                tipo,
                quantidade,
                unidadeNormalizada,
                Collections.unmodifiableMap(erros)
        );
    }

    public record Resultado(
            String nomeItem,
            boolean itemNovo,
            TipoMovimentacao tipo,
            Double quantidade,
            String unidadeMedida,
            Map<String, String> erros
    ) {
        public boolean valido() {
            return erros.isEmpty();
        }

        public boolean possuiErro(String campo) {
            return erros.containsKey(campo);
        }

        public String primeiraMensagem() {
            return erros.values().stream().findFirst().orElse("");
        }
    }
}
