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
    private final Usuario atendente;

    public Pedido(ItemVendavel item, int quantidade, String observacao, Usuario atendente){
        this(item, quantidade, observacao, atendente, new Date(), 0);
    }

    public Pedido(
            ItemVendavel item,
            int quantidade,
            String observacao,
            Usuario atendente,
            Date horario,
            int numeroLote
    ){
        if(quantidade <= 0) throw new IllegalArgumentException("Quantidade invalida");
        this.item = item;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.horario = horario == null ? new Date() : horario;
        this.atendente = atendente;
        this.numeroLote = numeroLote;
    }

    public ItemVendavel getItem() {
        return item;
    }

    /**
     * Define a quantidade final do item.
     * A operação é uma atribuição, não um incremento.
     */
    public void setQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade invalida");
        }
        this.quantidade = quantidade;
    }

    public Usuario getAtendente() {
        return atendente;
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