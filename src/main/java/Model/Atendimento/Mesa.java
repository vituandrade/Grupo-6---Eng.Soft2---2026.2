package Model.Atendimento;
import java.util.ArrayList;
import java.util.List;
import Model.Reservas.Reserva; // ADICIONE ESTE
import java.time.LocalDateTime; // ADICIONE ESTE
import java.util.ArrayList;     // ADICIONE ESTE

public class Mesa {
    private int numMesa;
    private List<Comanda> comandas;
    private boolean aguardandoPagamento;
    private java.util.List<Reserva> reservas = new ArrayList<>();

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

    public void adicionarReserva(Reserva r) {
        this.reservas.add(r);
    }

    public boolean estaReservadaAgora() {
        LocalDateTime agora = LocalDateTime.now();
        for (Reserva r : reservas) {
            LocalDateTime inicio = r.getDataHoraInicio();
            LocalDateTime fim = r.getDataHoraFim();

            boolean acontecendo = !agora.isBefore(inicio) && !agora.isAfter(fim);
            boolean chegando = agora.isBefore(inicio) && agora.plusMinutes(30).isAfter(inicio);

            if (acontecendo || chegando) return true;
        }
        return false;
    }

    public void encerrarReservaAtual() {
        LocalDateTime agora = LocalDateTime.now();
        this.reservas.removeIf(r -> {
            LocalDateTime inicio = r.getDataHoraInicio();
            LocalDateTime fim = r.getDataHoraFim();
            boolean acontecendo = !agora.isBefore(inicio) && !agora.isAfter(fim);
            boolean chegando = agora.isBefore(inicio) && agora.plusMinutes(40).isAfter(inicio);
            return acontecendo || chegando;
        });
    }

    public String getNomeClienteReserva() {
        LocalDateTime agora = LocalDateTime.now();

        for (Reserva r : this.reservas) {
            if (r.getDataHoraInicio().isBefore(agora.plusMinutes(30)) &&
                    r.getDataHoraFim().isAfter(agora.minusMinutes(120))) {

                return r.getNomeCliente();
            }
        }
        return "";
    }
}