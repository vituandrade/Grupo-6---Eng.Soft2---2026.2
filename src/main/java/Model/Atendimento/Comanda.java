package Model.Atendimento;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import Model.Pagamento.Pagamento;
import Model.Usuarios.Usuario;

public class Comanda {

    private static int proximoId = 1;
    private final int id;
    private String clienteNome;
    private boolean fechada;
    private List<Pedido> pedidos;
    private double desconto;
    private Pagamento pagamento;
    private final Date dataAbertura;
    private Date dataFechamento;
    private Usuario funcionarioFechamento;

    private int proximoLote = 1;

    public Comanda() {
        this(gerarNovoId(), null, new Date());
    }

    public Comanda(int id, String clienteNome, Date dataAbertura) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID da comanda inválido");
        }
        this.id = id;
        ajustarProximoId(id);
        this.pedidos = new ArrayList<>();
        this.fechada = false;
        this.clienteNome = clienteNome == null || clienteNome.isBlank()
                ? "Cliente " + this.id
                : clienteNome;
        this.desconto = 0.0;
        this.dataAbertura = dataAbertura == null ? new Date() : dataAbertura;
    }

    private static int gerarNovoId() {
        return proximoId++;
    }

    private static void ajustarProximoId(int id) {
        if (proximoId <= id) {
            proximoId = id + 1;
        }
    }

    public static void ajustarProximoIdPersistido(int id) {
        if (id > 0) {
            ajustarProximoId(id);
        }
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

    public Date getDataAbertura() {
        return dataAbertura;
    }

    public Date getDataFechamento() {
        return dataFechamento;
    }

    public Usuario getFuncionarioFechamento() {
        return funcionarioFechamento;
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

    public void recalcularProximoLote() {
        int maior = 0;
        for (Pedido pedido : pedidos) {
            maior = Math.max(maior, pedido.getNumeroLote());
        }
        proximoLote = maior + 1;
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
        registrarFechamento(pagamento, desconto, null);
    }

    public void registrarFechamento(Pagamento pagamento, double desconto, Usuario funcionario) {

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

        if (pedidos.isEmpty()) {
            throw new IllegalStateException(
                    "A comanda precisa ter ao menos um item para ser fechada"
            );
        }

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
        this.funcionarioFechamento = funcionario;
        this.dataFechamento = new Date();
        this.fechada = true;
    }

    @Override
    public String toString() {
        return String.format("Comanda %d - Cliente: %s (%s)", id, getClienteNome(), fechada ? "FECHADA" : "ABERTA");
    }
}