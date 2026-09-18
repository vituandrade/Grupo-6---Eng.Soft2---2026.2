package Model.Reservas;
import Model.Atendimento.Mesa;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Reserva implements Agendavel {
    protected String nomeCliente;
    protected Mesa mesa;
    protected LocalDateTime dataHora;

    public Reserva(String nomeCliente, Mesa mesa, LocalDateTime dataHora) {
        this.nomeCliente = nomeCliente;
        this.mesa = mesa;
        this.dataHora = dataHora;
    }

    @Override
    public LocalDateTime getDataHoraFim() {
        return getDataHoraInicio().plusMinutes(getDuracaoEmMinutos());
    }

    @Override
    public LocalDateTime getDataHoraInicio() {
        return dataHora;
    }

    public abstract double calcularValorAdiantamento();

    public String getNomeCliente() {
        return nomeCliente;
    }

    public Mesa getMesa() {
        return mesa;
    }

    @Override
    public String toString() {
        return "Cliente: " + nomeCliente + " (" + dataHora.format(DateTimeFormatter.ofPattern("HH:mm")) + ")";
    }
}