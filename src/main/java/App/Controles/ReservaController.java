package App.Controles;

import App.Persistencia.InterfacePersistencia;
import Model.Atendimento.Mesa;
import Model.Reservas.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReservaController {

    @FXML private TextField campoCliente;
    @FXML private ListView<Mesa> listaMesas;
    @FXML private DatePicker campoData;
    @FXML private TextField campoHora;
    @FXML private ComboBox<String> comboTipo;

    private Runnable onReservaSalva;
    private InterfacePersistencia persistenceService;

    public void inicializar(List<Mesa> todasMesas, Runnable callback,
                            InterfacePersistencia persistenceService) {
        this.onReservaSalva = callback;
        this.persistenceService = persistenceService;

        listaMesas.getItems().addAll(todasMesas);
        listaMesas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        listaMesas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Mesa item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String statusTexto;
                    String corTexto;

                    if (item.isOcupada()) {
                        statusTexto = " (Ocupada)";
                        corTexto = "-fx-text-fill: #dc3545;"; // Vermelho
                    }
                    else if (item.estaReservadaAgora()) {
                        statusTexto = " (Reservada)";
                        corTexto = "-fx-text-fill: #007bff;"; // Azul
                    }
                    else {
                        statusTexto = " (Livre)";
                        corTexto = "-fx-text-fill: #28a745;"; // Verde
                    }

                    setText("Mesa " + item.getNumMesa() + statusTexto);

                    setStyle(corTexto + "-fx-font-weight: bold;");
                }
            }
        });

        comboTipo.getItems().addAll("Comum", "Evento");
        comboTipo.getSelectionModel().selectFirst();
    }

    @FXML
    private void salvarReserva() {
        try {
            String cliente = campoCliente.getText();
            LocalDate data = campoData.getValue();
            String tipo = comboTipo.getValue();
            List<Mesa> mesasSelecionadas = listaMesas.getSelectionModel().getSelectedItems();

            String horaStr = campoHora.getText();
            if (horaStr != null) {
                horaStr = horaStr.trim();
                if (horaStr.length() == 4 && horaStr.indexOf(':') == 1) {
                    horaStr = "0" + horaStr;
                }
            }

            if (cliente == null || cliente.trim().isEmpty()) {
                mostrarAlerta("Erro", "O nome do cliente é obrigatório."); return;
            }
            if (mesasSelecionadas.isEmpty()) {
                mostrarAlerta("Erro", "Selecione pelo menos uma mesa."); return;
            }
            if (data == null) {
                mostrarAlerta("Erro", "Selecione uma data."); return;
            }
            if (horaStr == null || horaStr.isEmpty()) {
                mostrarAlerta("Erro", "Digite a hora."); return;
            }

            LocalTime hora = LocalTime.parse(horaStr);
            LocalDateTime dataHora = LocalDateTime.of(data, hora);

            StringBuilder resumo = new StringBuilder("Reservas Criadas:\n");

            for (Mesa mesa : mesasSelecionadas) {
                Reserva novaReserva;
                if ("Evento".equals(tipo)) {
                    novaReserva = new ReservaEvento(cliente, mesa, dataHora);
                } else {
                    novaReserva = new ReservaComum(cliente, mesa, dataHora);
                }

                mesa.adicionarReserva(novaReserva);

                // Persiste no banco de dados
                if (persistenceService != null) {
                    persistenceService.salvarReserva(novaReserva);
                }

                resumo.append("Mesa ").append(mesa.getNumMesa())
                        .append(" - Sinal: R$ ").append(String.format("%.2f", novaReserva.calcularValorAdiantamento()))
                        .append("\n");
            }

            mostrarAlerta("Sucesso", resumo.toString());
            if (onReservaSalva != null) onReservaSalva.run();
            ((Stage) campoCliente.getScene().getWindow()).close();

        } catch (DateTimeParseException e) {
            mostrarAlerta("Erro na Hora", "Digite a hora no formato HH:mm (Ex: 08:00 ou 20:30).");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro Inesperado", "Ocorreu um erro: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}