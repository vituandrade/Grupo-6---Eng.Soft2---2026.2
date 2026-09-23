package Model.Pagamento;

public class PagamentoPixPresencial extends Pagamento {

    public PagamentoPixPresencial(double valorDaConta) {
        super(valorDaConta);
    }

    @Override
    public boolean processar() {
        if (valorDaConta < 0) {
            statusConfirmado = false;
            return false;
        }

        statusConfirmado = true;
        return true;
    }

    @Override
    public String getTipo() {
        return "Pix presencial";
    }

    @Override
    public String gerarComprovante() {
        return "=== COMPROVANTE PIX PRESENCIAL ===\n" +
                "Valor Total: R$ " +
                String.format("%.2f", valorDaConta) +
                "\n" +
                "Transação: APROVADA";
    }
}