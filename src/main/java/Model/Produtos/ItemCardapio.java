package Model.Produtos;

public class ItemCardapio extends Produto {

    public ItemCardapio(String nome, String categoria, String descricao, double preco, boolean disponivel) {
        super(nome, descricao, preco, 0, categoria);
        setDisponivel(disponivel);
    }
}
