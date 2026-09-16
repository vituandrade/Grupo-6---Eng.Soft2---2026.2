package Model.Atendimento;
import java.util.ArrayList;
import java.util.List;

public class Mesa {
    private int numMesa;
    private List<Comanda> comandas;
    private boolean aguardandoPagamento;

    public Mesa(int numMesa){
        this.numMesa = numMesa;
        this.comandas = new ArrayList<>();
        this.aguardandoPagamento = false;
    }

    public int getNumMesa() {
        return numMesa;
    }

    public List<Comanda> getComandas() {
        return comandas;
    }

    public void adicionarComanda(Comanda c){
        if (c == null) throw new IllegalArgumentException("Comanda não pode ser nula");
        this.comandas.add(c);
    }

    public void removerComanda(Comanda c){
        if (c == null) return;
        this.comandas.remove(c);
    }

    public boolean isOcupada() {
        return !this.comandas.isEmpty();
    }

    public boolean isAguardandoPagamento() {
        return aguardandoPagamento;
    }

    public void setAguardandoPagamento(boolean aguardandoPagamento) {
        this.aguardandoPagamento = aguardandoPagamento;
    }

    @Override
    public String toString() {
        return String.format("Mesa %d - %s (%d comandas)", numMesa, isOcupada() ? "Ocupada" : "Livre", comandas.size());
    }

}