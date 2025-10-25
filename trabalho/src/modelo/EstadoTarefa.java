package modelo;

public enum EstadoTarefa{
    NOVA,
    PRONTA,
    EXECUTANDO,
    BLOQUEADA, // ja pensando na Defesa B segundo .txt de exemplo
    FINALIZADA;

    public boolean ehFinalizada(){
        return this == FINALIZADA;
    }

    public boolean podeEscalonar(){
        return this == PRONTA;
    }
}