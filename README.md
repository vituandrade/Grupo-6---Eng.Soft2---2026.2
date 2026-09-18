# SistemaGestaoRestaurante

Sistema desktop JavaFX para gestao de restaurante (login, mesas, comandas, produtos, reservas e pagamentos). Os dados ficam em JSON na pasta `Dados/`.

## Requisitos

- JDK 17
- Maven 3.8+

No macOS com Homebrew:

```bash
brew install openjdk@17 maven
```

## Como executar

Na pasta do projeto:

```bash
chmod +x run.sh
./run.sh
```

Ou, com Java 17 no `JAVA_HOME`:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
mvn javafx:run
```

Login padrao: usuario `admin`, senha `admin`.
