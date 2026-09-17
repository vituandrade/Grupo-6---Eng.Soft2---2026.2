package App.Persistencia;

import App.Utils.RuntimeTypeAdapterFactory;
import Model.Produtos.*;
import Model.Produtos.Alimentos.Refeicao;
import Model.Produtos.Alimentos.TiraGosto;
import Model.Produtos.Bedidas.ComAlcool;
import Model.Produtos.Bedidas.SemAlcool;
import Model.Produtos.Outros.Descartaveis;
import Model.Produtos.Outros.Servico;
import Model.Sistema.Config;
import Model.Usuarios.Garcom;
import Model.Usuarios.Interno;
import Model.Usuarios.Usuario;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class PersistenceService implements InterfacePersistencia {

    private RuntimeTypeAdapterFactory<Produto> produtoAdapter = RuntimeTypeAdapterFactory
            .of(Produto.class, "tipo_classe")
            .registerSubtype(ComAlcool.class, "com_alcool")
            .registerSubtype(SemAlcool.class, "sem_alcool")
            .registerSubtype(Refeicao.class, "refeicao")
            .registerSubtype(TiraGosto.class, "tira_gosto")
            .registerSubtype(Servico.class, "servico")
            .registerSubtype(Descartaveis.class, "descartavel")
            .registerSubtype(ItemCardapio.class, "item_cardapio");

    private Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(produtoAdapter)
            .setPrettyPrinting()
            .create();

    private static final String CONFIG_FILE = "Dados/config.json";
    private static final String USUARIOS_FILE = "Dados/usuarios.json";
    private static final String PRODUTOS_FILE = "Dados/produtos.json";


    @Override
    public Config carregarConfig() {
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                Config config = gson.fromJson(reader, Config.class);
                System.out.println("Configurações carregadas de " + CONFIG_FILE);
                return config;
            } catch (IOException e) {
                System.out.println("Erro ao ler config.json, usando padrão. Erro: " + e.getMessage());
                return new Config();
            }
        } else {
            System.out.println(CONFIG_FILE + " não encontrado, criando um novo com valores padrão.");
            Config configPadrao = new Config();
            salvarConfig(configPadrao);
            return configPadrao;
        }
    }

    @Override
    public void salvarConfig(Config config) {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(config, writer);
            System.out.println("Configurações salvas em " + CONFIG_FILE);
        } catch (IOException e) {
            System.out.println("Erro ao salvar config.json: " + e.getMessage());
        }
    }

    @Override
    public void salvarProdutos(List<Produto> produtos) {
        try (Writer writer = new FileWriter(PRODUTOS_FILE)) {
            Type listaTipo = new TypeToken<List<Produto>>() {}.getType();
            gson.toJson(produtos, listaTipo, writer);
            System.out.println("Produtos salvos em " + PRODUTOS_FILE);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível salvar os itens do cardápio.", e);
        }
    }

    @Override
    public List<Produto> carregarProdutos() {
        File produtoFile = new File(PRODUTOS_FILE);

        if (!produtoFile.exists()) {
            System.out.println(PRODUTOS_FILE + " não encontrado, retornando lista vazia.");
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(produtoFile)) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root == null || root.isJsonNull()) return new ArrayList<>();
            if (!root.isJsonArray()) return new ArrayList<>();

            JsonArray jsonArray = root.getAsJsonArray();
            boolean precisouMigrar = false;

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                if (!obj.has("tipo_classe")) {
                    obj.addProperty("tipo_classe", inferirTipoClasse(obj));
                    precisouMigrar = true;
                }
                if (!obj.has("disponivel")) {
                    obj.addProperty("disponivel", true);
                    precisouMigrar = true;
                }
            }
            Type listaTipo = new TypeToken<ArrayList<Produto>>() {}.getType();
            List<Produto> produtos = gson.fromJson(jsonArray, listaTipo);

            System.out.println("Produtos carregados de " + PRODUTOS_FILE);
            if (precisouMigrar) {
                System.out.println("Migração de dados antigos detectada. Atualizando arquivo...");
                salvarProdutos(produtos);
            }

            return produtos;

        } catch (IOException | IllegalStateException | com.google.gson.JsonSyntaxException e) {
            System.out.println("Erro crítico ao ler/migrar " + PRODUTOS_FILE + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String inferirTipoClasse(JsonObject produto) {
        if (!produto.has("categoriaNome")) {
            return "refeicao";
        }

        String categoria = produto.get("categoriaNome").getAsString().toLowerCase();
        if (categoria.contains("sem") && categoria.contains("lcool")) return "sem_alcool";
        if (categoria.contains("com") && categoria.contains("lcool")) return "com_alcool";
        if (categoria.contains("tira")) return "tira_gosto";
        if (categoria.contains("servi")) return "servico";
        if (categoria.contains("descart")) return "descartavel";
        return "refeicao";
    }

    @Override
    public List<Usuario> carregarUsuarios() {
        File usuariosFile = new File(USUARIOS_FILE);

        if (usuariosFile.exists()) {
            try (Reader reader = new FileReader(usuariosFile)) {
                JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
                List<Usuario> usuarios = new ArrayList<>();

                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    boolean isInterno = jsonObject.get("acessoConfig").getAsBoolean();

                    if (isInterno) {
                        usuarios.add(gson.fromJson(jsonObject, Interno.class));
                    } else {
                        usuarios.add(gson.fromJson(jsonObject, Garcom.class));
                    }
                }
                System.out.println("Usuários carregados de " + USUARIOS_FILE);
                return usuarios;

            } catch (IOException | IllegalStateException e) {
                System.out.println("Erro ao ler " + USUARIOS_FILE + ", usando padrão. Erro: " + e.getMessage());
                return criarUsuariosPadraoESalvar();
            }
        } else {
            System.out.println(USUARIOS_FILE + " não encontrado, criando padrão.");
            return criarUsuariosPadraoESalvar();
        }
    }

    @Override
    public void salvarUsuarios(List<Usuario> usuarios) {
        try (Writer writer = new FileWriter(USUARIOS_FILE)) {
            gson.toJson(usuarios, writer);
            System.out.println("Usuários salvos em " + USUARIOS_FILE);
        } catch (IOException e) {
            System.out.println("Erro ao salvar usuarios.json: " + e.getMessage());
        }
    }


    private List<Usuario> criarUsuariosPadraoESalvar() {
        List<Usuario> padrao = new ArrayList<>();
        padrao.add(new Interno("admin", "admin"));

        salvarUsuarios(padrao);
        return padrao;
    }
}
