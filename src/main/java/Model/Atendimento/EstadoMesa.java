package Model.Atendimento;

public enum EstadoMesa {
    LIVRE("Livre"),
    OCUPADA("Ocupada"),
    AGUARDANDO_FECHAMENTO("Aguardando fechamento");

    private final String descricao;

    EstadoMesa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
