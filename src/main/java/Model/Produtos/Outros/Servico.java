package Model.Produtos.Outros;

public class Servico extends Outros { // <--- Herda de Outros
    public Servico(String nome, String descricao, double preco, Integer estoque, String categoriaNome) {
        super(nome, descricao, preco, estoque, categoriaNome);
    }
}