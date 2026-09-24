package Model.Atendimento;
import java.util.ArrayList;
import java.util.List;

public class Mesa {
    private final int numMesa;
    private final List<Comanda> comandas;
    private EstadoMesa estado;

    public Mesa(int numMesa){
        this(numMesa, EstadoMesa.LIVRE);
    }

    public Mesa(int numMesa, EstadoMesa estado) {
        if (numMesa <= 0) {
            throw new IllegalArgumentException("O número da mesa deve ser maior que zero.");
        }
        this.numMesa = numMesa;
        this.comandas = new ArrayList<>();
        this.estado = estado == null ? EstadoMesa.LIVRE : estado;
    }

    public int getNumMesa() {
        return numMesa;
    }

    public List<Comanda> getComandas() {
        return comandas;
    }

    public void adicionarComanda(Comanda c) {
        if (c == null) {
            throw new IllegalArgumentException("Comanda não pode ser nula");
        }

        if (temComandaAberta()) {
            throw new IllegalStateException("A mesa já possui uma comanda aberta");
        }

        this.comandas.add(c);
        if (this.estado != EstadoMesa.AGUARDANDO_FECHAMENTO) {
            this.estado = EstadoMesa.OCUPADA;
        }
    }

    public boolean temComandaAberta() {
        return this.comandas.stream().anyMatch(c -> !c.isFechada());
    }

    public void removerComanda(Comanda c){
        if (c == null) return;
        this.comandas.remove(c);
        if (!temComandaAberta() && estado != EstadoMesa.AGUARDANDO_FECHAMENTO) {
            this.estado = EstadoMesa.LIVRE;
        }
    }

    public boolean isOcupada() {
        return estado != EstadoMesa.LIVRE;
    }

    public boolean isAguardandoPagamento() {
        return estado == EstadoMesa.AGUARDANDO_FECHAMENTO;
    }

    public void setAguardandoPagamento(boolean aguardandoPagamento) {
        if (aguardandoPagamento) {
            this.estado = EstadoMesa.AGUARDANDO_FECHAMENTO;
        } else {
            this.estado = temComandaAberta() ? EstadoMesa.OCUPADA : EstadoMesa.LIVRE;
        }
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    /**
     * Altera manualmente o estado seguindo o ciclo previsto no UC03.
     * Não é permitido pular etapas, nem liberar uma mesa com comanda aberta.
     */
    public void alterarEstado(EstadoMesa novoEstado) {
        validarAlteracao(novoEstado);
        this.estado = novoEstado;
    }

    public void validarAlteracao(EstadoMesa novoEstado) {
        if (novoEstado == null) {
            throw new IllegalArgumentException("Selecione um estado para a mesa.");
        }
        if (novoEstado == estado) {
            return;
        }

        boolean transicaoValida =
                (estado == EstadoMesa.LIVRE && novoEstado == EstadoMesa.OCUPADA)
                || (estado == EstadoMesa.OCUPADA && novoEstado == EstadoMesa.AGUARDANDO_FECHAMENTO)
                || (estado == EstadoMesa.AGUARDANDO_FECHAMENTO
                    && novoEstado == EstadoMesa.LIVRE
                    && !temComandaAberta());

        if (!transicaoValida) {
            throw new IllegalStateException(
                    "Não é possível alterar a mesa de " + estado + " para " + novoEstado + "."
            );
        }
    }

    @Override
    public String toString() {
        return String.format("Mesa %d ", numMesa);
    }

}
