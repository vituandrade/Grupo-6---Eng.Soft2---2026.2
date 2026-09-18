package App.Persistencia;

import Model.Atendimento.Mesa;
import Model.Reservas.Reserva;
import Model.Sistema.Config;
import Model.Produtos.Produto;
import Model.Usuarios.Usuario;
import java.util.List;

public interface InterfacePersistencia {

    Config carregarConfig();
    void salvarConfig(Config config);

    List<Usuario> carregarUsuarios();
    void salvarUsuarios(List<Usuario> usuarios);

    List<Produto> carregarProdutos();
    void salvarProdutos(List<Produto> produtos);

    // Reservas
    void salvarReserva(Reserva reserva);
    List<Reserva> carregarReservas(List<Mesa> mesas);
    void removerReserva(int reservaId);

    // Histórico de pagamentos
    void registrarPagamento(int numMesa, String clienteNome, double valor,
                            String tipoPagamento, String detalhes);
}
