package Model.Reservas;
import Model.Atendimento.Mesa;
import java.time.LocalDateTime;

public class ReservaComum extends Reserva {
    public ReservaComum(String nomeCliente, Mesa mesa, LocalDateTime dataHora) {
        super(nomeCliente, mesa, dataHora);
    }

    @Override
    public int getDuracaoEmMinutos() {
        return 120;
    }

    @Override
    public double calcularValorAdiantamento() {
        return 0.0;
    }
}