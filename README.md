# 🍽️ Sistema de Gestão de Restaurante

Aplicação desktop acadêmica desenvolvida para a disciplina de **Engenharia de Software II — Projeto 06**, com o objetivo de auxiliar na gestão interna de um restaurante.

O sistema permite o gerenciamento de **cardápio, mesas, comandas, pedidos, estoque, fechamento de contas e histórico de vendas**, com persistência local das informações.

---

## Funcionalidades

### Autenticação
- Login de funcionários
- Controle de acesso conforme o perfil do usuário

### Cardápio
- Cadastro de itens
- Organização por categorias
- Controle de preço e disponibilidade

### Mesas
- Visualização das mesas
- Controle do estado das mesas
- Associação de mesas às comandas

### Comandas e pedidos
- Abertura de comandas
- Registro de pedidos
- Alteração da quantidade dos itens
- Cancelamento de itens
- Cálculo automático dos subtotais e total da comanda
- Persistência de comandas e pedidos em aberto

### Estoque
- Controle de quantidade disponível
- Registro de movimentações
- Atualização do estoque conforme as operações realizadas

### Fechamento de conta
- Cálculo do subtotal
- Aplicação de desconto autorizado
- Registro da forma de pagamento
- Fechamento da comanda
- Liberação da mesa após o fechamento

### Histórico de vendas
- Consulta das vendas realizadas
- Visualização das informações da venda
- Filtro por período
- Filtro por forma de pagamento

### Painel inicial
- Visão geral do restaurante
- Quantidade de mesas livres
- Quantidade de comandas abertas
- Alertas de itens com estoque baixo
- Acesso rápido às principais operações

---

## Tecnologias utilizadas

| Tecnologia | Utilização |
|---|---|
| Java 17 | Linguagem de programação |
| JavaFX | Interface gráfica |
| FXML | Estrutura das telas |
| CSS | Estilização da interface |
| Maven | Gerenciamento e execução do projeto |
| SQLite | Persistência dos dados |
| JUnit 5 | Testes automatizados |

---

## Pré-requisitos

Para executar o sistema, é necessário ter:

- **Java 17 ou superior**
- **Maven**, ou utilizar o Maven Wrapper incluído no projeto

---

## Como executar

### Windows

Utilizando o Maven Wrapper:

```bat
mvnw.cmd javafx:run
```

Ou, caso o Maven esteja instalado:

```bat
mvn javafx:run
```

### Linux e macOS

Utilizando o Maven Wrapper:

```bash
./mvnw javafx:run
```

---

## Banco de dados

O sistema utiliza **SQLite** para persistência local.

O banco de dados armazena informações relacionadas a:

- Usuários
- Produtos
- Estoque
- Comandas abertas
- Pedidos
- Vendas realizadas

O banco é utilizado localmente pela aplicação e o arquivo de banco não é versionado no Git.

---

## Testes

Para executar a suíte de testes automatizados:

```bash
mvn clean test
```

Os testes abrangem funcionalidades de:

- Validação de dados
- Autenticação
- Persistência
- Controle de estoque
- Histórico de vendas
- Pedidos e comandas

---

## Credencial de demonstração

**Usuário:** `admin`

**Senha:** `admin`

---

## Estrutura do projeto

```text
Grupo-6---Eng.Soft2---2026.2
│
├── .mvn/
│   └── wrapper/
│
├── Dados/
│   ├── config.json
│   ├── produtos.json
│   ├── usuarios.json
│   └── restaurante.db
│
├── database/
│
├── diagramas/
│
├── docs/
│
├── prototipos/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── App/
│   │   │   │   ├── Controles/
│   │   │   │   ├── Persistencia/
│   │   │   │   ├── Utils/
│   │   │   │   └── Validacao/
│   │   │   │
│   │   │   └── Model/
│   │   │       ├── Atendimento/
│   │   │       ├── Estoque/
│   │   │       ├── Pagamento/
│   │   │       ├── Produtos/
│   │   │       └── Usuarios/
│   │   │
│   │   └── resources/
│   │       └── App/
│   │           ├── arquivos FXML
│   │           └── arquivos CSS
│   │
│   └── test/
│       └── java/
│           └── testes automatizados
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## Execução local

O sistema foi desenvolvido para operar localmente em um único computador.

As funcionalidades principais não dependem de:

- Navegador
- Serviços em nuvem
- APIs externas
- Pedidos pela Internet
- Serviços de entrega
- Pagamentos online

---

## Projeto acadêmico

**Disciplina:** Engenharia de Software II

**Projeto:** Projeto 06 — Sistema de Gestão de Restaurante

**Grupo:** 6

**Semestre:** 2026.2

### Integrantes

- Brisa Tielly Almeida da Silva
- Victor Henrick Santos Andrade
- Rennan Oliveira Santos
- Wendell Moura Leite
- José Henrique Cintra de Souza Barros