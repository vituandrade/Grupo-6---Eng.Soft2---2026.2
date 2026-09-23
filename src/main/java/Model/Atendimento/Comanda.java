package Model.Atendimento;
import java.util.ArrayList;
import java.util.List;

import Model.Pagamento.Pagamento;

public class Comanda {

    private static int proximoId = 1;
    private final int id;
    private String clienteNome;
    private boolean fechada;
    private List<Pedido> pedidos;
    private double desconto;
    private Pagamento pagamento;

    private int proximoLote = 1;

    public Comanda() {
        this.id = proximoId++;
        this.pedidos = new ArrayList<>();
        this.fechada = false;
        this.clienteNome = "Cliente " + this.id;
        this.desconto = 0.0;
    }

    public int getId() {
        return id;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public double getDesconto() {
        return desconto;
    }

    public void setDesconto(double desconto) {

        if (desconto < 0) {
            throw new IllegalArgumentException("Desconto não pode ser negativo");
        }

        if (desconto > calcularSubtotal()) {
            throw new IllegalArgumentException("Desconto não pode ser maior que o subtotal");
        }

        this.desconto = desconto;
    }

        public Pagamento getPagamento() {
            return pagamento;
    }

        public void setPagamento(Pagamento pagamento) {
            this.pagamento = pagamento;
    }

    public boolean isFechada() {
        return fechada;
    }

    public int gerarNovoNumeroLote() {
        return proximoLote++;
    }

    public void adicionarPedido(Pedido p){
        if(fechada) throw new IllegalArgumentException("Comanda já está fechada");
        pedidos.add(p);
    }

    public double calcularSubtotal(){

        double subtotal = 0.0;

        for(Pedido e : pedidos){
            subtotal += e.getSubtotal();
        }

        return subtotal;
    }
    public double calcularTotal(){
        return calcularSubtotal() - desconto;
    }

    public void fechar(){
        if(fechada) throw new IllegalStateException("Comando já estava fechada");
        fechada = true;
    }


    public List<Pedido> getPedidos(){
        return this.pedidos;
    }

    public void registrarFechamento(Pagamento pagamento, double desconto) {

        if (fechada) {
            throw new IllegalStateException(
                    "A comanda já está fechada"
            );
        }

        if (pagamento == null) {
            throw new IllegalArgumentException(
                    "Pagamento não pode ser nulo"
            );
        }

        if (!pagamento.getStatusConfirmado()) {
            throw new IllegalStateException(
                    "O pagamento ainda não foi confirmado"
            );
        }

        double subtotal = calcularSubtotal();

        if (desconto < 0) {
            throw new IllegalArgumentException(
                    "Desconto não pode ser negativo"
            );
        }

        if (desconto > subtotal) {
            throw new IllegalArgumentException(
                    "Desconto não pode ser maior que o subtotal"
            );
        }

        this.desconto = desconto;
        this.pagamento = pagamento;
        this.fechada = true;
    }

    @Override
    public String toString() {
        return String.format("Comanda %d - Cliente: %s (%s)", id, getClienteNome(), fechada ? "FECHADA" : "ABERTA");
    }
}