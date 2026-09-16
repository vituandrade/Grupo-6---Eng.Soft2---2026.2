module restaurante.sistemagestaorestaurante {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.google.gson;

    opens App to javafx.fxml;
    exports App;

    exports App.Controles;
    opens App.Controles to javafx.fxml;

    opens Model.Usuarios to com.google.gson;
    opens Model.Produtos to com.google.gson;
    exports App.Persistencia;
    opens App.Persistencia to javafx.fxml;
    exports Model.Atendimento;
    opens Model.Atendimento to com.google.gson;
    exports Model.Sistema;
    opens Model.Sistema to com.google.gson;
    exports App.Utils;
    opens App.Utils to com.google.gson;
    opens Model.Produtos.Alimentos to com.google.gson;
    opens Model.Produtos.Bedidas to com.google.gson;
    opens Model.Produtos.Outros to com.google.gson;
}