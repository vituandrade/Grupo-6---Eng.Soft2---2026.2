package App.Persistencia;

import Model.Atendimento.Venda;
import Model.Atendimento.Mesa;
import Model.Estoque.ItemEstoque;
import Model.Estoque.TipoMovimentacao;
import Model.Sistema.Config;
import Model.Produtos.Produto;
import Model.Usuarios.Usuario;
import java.time.LocalDate;
import java.util.List;

public interface InterfacePersistencia {

    Config carregarConfig();
    void salvarConfig(Config config);

    List<Usuario> carregarUsuarios();
    void salvarUsuarios(List<Usuario> usuarios);

    List<Produto> carregarProdutos();
    void salvarProdutos(List<Produto> produtos);

    default List<ItemEstoque> carregarItensEstoque() {
        return List.of();
    }

    default ItemEstoque registrarMovimentacaoEstoque(
            Integer itemId,
            String nomeNovoItem,
            String unidadeMedida,
            TipoMovimentacao tipo,
            double quantidade
    ) {
        throw new PersistenciaException("O controle de estoque requer persistência em banco de dados.", null);
    }

    default Venda registrarVenda(Model.Atendimento.Comanda comanda, Model.Atendimento.Mesa mesa, Usuario funcionario, Model.Pagamento.Pagamento pagamento, double desconto) {
        throw new PersistenciaException("O histórico de vendas requer persistência em banco de dados.", null);
    }

    default List<Venda> carregarHistoricoVendas(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String formaPagamento
    ) {
        return List.of();
    }

    default void salvarComandaAberta(
            Model.Atendimento.Comanda comanda,
            Model.Atendimento.Mesa mesa,
            Model.Usuarios.Usuario funcionario
    ) {
        throw new PersistenciaException(
                "A persistência de comandas abertas requer banco de dados.",
                null
        );
    }

    default void carregarComandasAbertas(
            List<Model.Atendimento.Mesa> mesas,
            List<Model.Atendimento.Comanda> comandasSemMesa,
            List<Produto> produtos,
            List<Usuario> usuarios
    ) {
    }

    // ── UC03 – Mesas ─────────────────────────────────────────────────────────

    /** Retorna as mesas cadastradas, com seus estados, em ordem numérica. */
    default List<Mesa> carregarMesas() {
        return List.of();
    }

    /** Cria ou atualiza uma mesa e seu estado atual. */
    default void salvarMesa(Mesa mesa) {
        // implementação opcional para adaptadores sem banco
    }

    /**
     * Remove a mesa com o número informado do banco de dados.
     */
    default void removerMesa(int numero) {
        // implementação opcional para adaptadores sem banco
    }
}
