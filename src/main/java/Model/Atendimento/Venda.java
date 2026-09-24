package Model.Atendimento;

import java.time.LocalDateTime;

/**
 * Registro histórico imutável de uma venda concluída.
 * É criado quando uma comanda é fechada e persistido no banco local.
 */
public class Venda {

    private final int id;
    private final int comandaId;
    private final LocalDateTime dataHora;
    private final String clienteNome;
    private final String atendimento;
    private final String formaPagamento;
    private final double subtotal;
    private final double desconto;
    private final double total;
    private final String funcionario;

    public Venda(
            int id,
            int comandaId,
            LocalDateTime dataHora,
            String clienteNome,
            String atendimento,
            String formaPagamento,
            double subtotal,
            double desconto,
            double total,
            String funcionario
    ) {
        this.id = id;
        this.comandaId = comandaId;
        this.dataHora = dataHora;
        this.clienteNome = clienteNome;
        this.atendimento = atendimento;
        this.formaPagamento = formaPagamento;
        this.subtotal = subtotal;
        this.desconto = desconto;
        this.total = total;
        this.funcionario = funcionario;
    }

    public int getId() {
        return id;
    }

    public int getComandaId() {
        return comandaId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getAtendimento() {
        return atendimento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDesconto() {
        return desconto;
    }

    public double getTotal() {
        return total;
    }

    public String getFuncionario() {
        return funcionario;
    }
}
