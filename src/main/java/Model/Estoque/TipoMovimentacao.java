package Model.Estoque;

public enum TipoMovimentacao {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String descricao;

    TipoMovimentacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoMovimentacao porDescricao(String descricao) {
        for (TipoMovimentacao tipo : values()) {
            if (tipo.descricao.equals(descricao)) {
                return tipo;
            }
        }
        return null;
    }
}
