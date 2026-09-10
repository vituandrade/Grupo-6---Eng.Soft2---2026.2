package Model.Atendimento;
import java.util.Date;

import Model.Produtos.ItemVendavel;
import Model.Usuarios.Usuario;

public class Pedido {
    private ItemVendavel item;
    private int quantidade;
    private String observacao;
    private Date horario;
    private int numeroLote;

    public Pedido(ItemVendavel item, int quantidade, String observacao, Usuario atendente){
        if(quantidade <= 0 ) throw new IllegalArgumentException("Quantidade invalida");
        this.item = item;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.horario = new Date();
    }

    public ItemVendavel getItem() {
        return item;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade += quantidade;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getObservacao() {
        return observacao;
    }

    public Date getHorario() {
        return horario;
    }

    public double getSubtotal(){
        return  item.getPreco() * quantidade;
    }

    public int getNumeroLote() {
        return numeroLote;
    }

    public void setNumeroLote(int numeroLote) {
        this.numeroLote = numeroLote;
    }

    @Override
    public String toString() {
        return String.format("%s x%d = R$ %.2f %s", item.getNome(), quantidade, getSubtotal(),
                (observacao == null || observacao.isEmpty()) ? "" : ("(" + observacao + ")"));
    }


}