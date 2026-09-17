package App.Validacao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CadastroItemValidator {

    public static final String NOME = "nome";
    public static final String CATEGORIA = "categoria";
    public static final String PRECO = "preco";
    public static final String DISPONIBILIDADE = "disponibilidade";

    private CadastroItemValidator() {
    }

    public static Resultado validar(
            String nome,
            String categoria,
            String descricao,
            String precoInformado,
            String disponibilidade
    ) {
        Map<String, String> erros = new LinkedHashMap<>();
        String nomeNormalizado = nome == null ? "" : nome.trim();
        String descricaoNormalizada = descricao == null ? "" : descricao.trim();

        if (nomeNormalizado.isEmpty()) {
            erros.put(NOME, "Informe o nome do item.");
        }
        if (categoria == null || categoria.isBlank()) {
            erros.put(CATEGORIA, "Selecione uma categoria.");
        }
        if (disponibilidade == null || disponibilidade.isBlank()) {
            erros.put(DISPONIBILIDADE, "Selecione a disponibilidade.");
        }

        Double preco = null;
        try {
            String valorNormalizado = precoInformado == null
                    ? ""
                    : precoInformado.trim().replace(",", ".");
            preco = Double.parseDouble(valorNormalizado);
            if (!Double.isFinite(preco) || preco <= 0) {
                erros.put(PRECO, "O preço deve ser maior que zero.");
            }
        } catch (NumberFormatException e) {
            erros.put(PRECO, "Informe um preço numérico válido.");
        }

        return new Resultado(
                nomeNormalizado,
                categoria,
                descricaoNormalizada,
                preco,
                "Disponível".equals(disponibilidade),
                Collections.unmodifiableMap(erros)
        );
    }

    public record Resultado(
            String nome,
            String categoria,
            String descricao,
            Double preco,
            boolean disponivel,
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
