package Model.Estoque;

public class ItemEstoque {

    private final int id;
    private final String nome;
    private final String unidadeMedida;
    private final double quantidade;

    public ItemEstoque(int id, String nome, String unidadeMedida, double quantidade) {
        this.id = id;
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.quantidade = quantidade;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public boolean precisaReposicao() {
        return quantidade <= 5;
    }

    public String getSituacao() {
        if (quantidade <= 0) {
            return "Sem estoque";
        }
        if (quantidade <= 5) {
            return "Estoque baixo";
        }
        return "Normal";
    }

    @Override
    public String toString() {
        return nome;
    }
}
