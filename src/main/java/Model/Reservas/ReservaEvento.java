package Model.Reservas;
import Model.Atendimento.Mesa;
import java.time.LocalDateTime;

public class ReservaEvento extends Reserva {

    public ReservaEvento(String nomeCliente, Mesa mesa, LocalDateTime dataHora) {
        super(nomeCliente, mesa, dataHora);
    }

    @Override
    public int getDuracaoEmMinutos() {
        return 1440;
    }

    @Override
    public double calcularValorAdiantamento() {
        return 80.00;
    }
}