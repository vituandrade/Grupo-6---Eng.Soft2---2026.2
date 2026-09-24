# \# Sistema de Gestão de Restaurante

# 

# Aplicação desktop acadêmica para gestão interna de um restaurante, desenvolvida como parte da disciplina de Engenharia de Software II — Projeto 06.

# 

# O sistema permite o gerenciamento de cardápio, mesas, comandas, pedidos, estoque, fechamento de contas e histórico de vendas, com persistência local das informações. O sistema foi desenvolvido para funcionar localmente em um único computador, sem dependência de navegador, serviços em nuvem, pedidos pela Internet ou pagamentos online.

# 

# \## Funcionalidades

# \- Autenticação de funcionários

# \- Cadastro e gerenciamento de itens do cardápio

# \- Controle de mesas

# \- Abertura e gerenciamento de comandas

# \- Registro de pedidos

# \- Alteração de quantidade dos pedidos

# \- Cancelamento de itens da comanda

# \- Controle de estoque

# \- Fechamento de contas

# \- Aplicação de descontos autorizados

# \- Registro de pagamentos presenciais

# \- Histórico de vendas

# \- Filtros no histórico por período e forma de pagamento

# \- Persistência de comandas e pedidos em aberto

# \- Persistência de vendas no banco de dados

# \- Painel inicial com visão geral do restaurante

# 

# \## Tecnologias

# \- Java 17

# \- JavaFX

# \- FXML

# \- Maven

# \- SQLite

# \- JUnit 5

# 

# \## Pré-requisitos

# Para executar o sistema, é necessário ter instalado:

# \- Java 17 ou superior

# \- Maven ou utilizar o Maven Wrapper incluído no projeto

# 

# \## Execução

# 

# \### Windows

# Utilizando o Maven Wrapper:

# `mvnw.cmd javafx:run`

# 

# Ou, caso o Maven esteja instalado:

# `mvn javafx:run`

# 

# \### Linux e macOS

# Utilizando o Maven Wrapper:

# `./mvnw javafx:run`

# 

# \## Banco de dados

# O sistema utiliza SQLite para persistência local.

# 

# O banco de dados é armazenado no diretório `Dados` e é utilizado para manter informações como usuários, produtos, estoque, comandas abertas, pedidos e vendas registradas.

# 

# O arquivo do banco de dados não é versionado no Git, pois cada ambiente de execução possui sua própria base local.

# 

# \## Testes

# Os testes automatizados podem ser executados com:

# `mvn clean test`

# 

# A suíte de testes contempla validações, persistência, movimentações de estoque, histórico de vendas e operações relacionadas a pedidos e comandas.

# 

# \## Credencial de demonstração

# \- \*\*Usuário:\*\* `admin`

# \- \*\*Senha:\*\* `admin`

# 

# \## Estrutura do projeto

# ```text

# ├── src/

# │   ├── main/

# │   │   ├── java/

# │   │   │   ├── Controles/

# │   │   │   ├── Persistencia/

# │   │   │   ├── Utils/

# │   │   │   └── Validacao/

# │   │   │       └── Model/

# │   │   │           ├── Atendimento/

# │   │   │           ├── Estoque/

# │   │   │           ├── Pagamento/

# │   │   │           ├── Produtos/

# │   │   │           └── Usuarios/

# │   │   └── resources/

# │   │       └── App/

# │   │           ├── arquivos FXML

# │   │           └── arquivos CSS

# │   └── test/

# │       └── java/

# │           └── testes automatizados

# ├── Dados/

# │   └── arquivos e banco de dados local

# └── docs/

# &#x20;   └── documentação do projeto









# \## Projeto acadêmico

# \*\*Disciplina:\*\* Engenharia de Software II

# \*\*Projeto:\*\* Projeto 06 — Sistema de Gestão de Restaurante

# \*\*Grupo:\*\* 6

# \*\*Semestre:\*\* 2026.2

# 

# \### Integrantes

# \- Brisa Tielly Almeida da Silva

# \- Victor Henrick Santos Andrade

# \- Rennan Oliveira Santos

# \- Wendell Moura Leite

# \- José Henrique Cintra de Souza Barros



