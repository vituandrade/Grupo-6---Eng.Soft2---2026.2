package App.Persistencia;

import Model.Estoque.ItemEstoque;
import Model.Estoque.TipoMovimentacao;
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
}
