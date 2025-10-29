import simulador.Simulador;

public class Main {
    public static void main(String[] args) {
        String caminhoDoArquivoDeConfiguracao = "config.txt";
        Simulador simulador = new Simulador(caminhoDoArquivoDeConfiguracao);
        simulador.executar();
    }
}