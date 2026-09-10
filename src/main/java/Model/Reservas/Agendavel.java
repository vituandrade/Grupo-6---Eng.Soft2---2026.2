package Model.Reservas;
import java.time.LocalDateTime;

public interface Agendavel {
    LocalDateTime getDataHoraInicio();
    int getDuracaoEmMinutos();
    LocalDateTime getDataHoraFim();
}

