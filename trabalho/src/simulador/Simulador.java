package simulador;

import sistemaoperacional.SistemaOperacional;
import modelo.Tarefa;

import java.util.ArrayList;
import java.util.Scanner;

import ui.Terminal;

public class Simulador {

    private SistemaOperacional so;

    Relogio relogio;

    ArrayList<Tarefa> tarefasDoArquivoDeConfiguracao = new ArrayList<>();

    public Simulador(String caminhoDoArquivoDeConfiguracao) {
        //this.leitor = new LeitorDeConfiguracao(caminhoDoArquivoDeConfiguracao);
        relogio = Relogio.getInstancia();
    }
    
    private void carregarTarefasDoArquivoDeConfiguracao(){

        //tarefasDoArquivoDeConfiguracao = leitor.lerTarefas();
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t01", "E74C3C", 0, 5, 3)); // Vermelho
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t02", "3498DB", 0, 3, 2)); // Azul
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t03", "27AE60", 2, 6, 4)); // Verde
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t04", "F1C40F", 4, 2, 9)); // Amarelo

    }

    public void executar() {
        try(Scanner scanner = new Scanner(System.in)){
            String comando;

            while (true) {
                System.out.println("Digite um comando:");
                System.out.println("1 - executa tudo | 2 - executa passo a passo | 0 - sair");
                comando = scanner.nextLine();

                switch (comando) {
                    case "1":
                        execTodoSO();
                        break;
                    case "2":
                        execSOPassoAPasso();
                        break;
                    case "0":
                        System.out.println("Encerrando o simulador.");
                        return;
                    default:
                        System.out.println("Comando inválido. Tente novamente.");
                }
            }
        }
    }

    public void execTodoSO(){
        carregarTarefasDoArquivoDeConfiguracao();

        Terminal.println("Executando Tudo");
        Terminal.println("Lista de Tarefas carregadas do arquivo de configuração:");

        Terminal.printaListaTarefa(tarefasDoArquivoDeConfiguracao);
        Terminal.debugPrintln("Pronto para iniciar a simulacao?");
        Terminal.esperar();
        so = new SistemaOperacional(tarefasDoArquivoDeConfiguracao, "SRTF", 3, 1);
        while (!so.terminouTodasTarefas()) {
            // Executa uma unidade de tempo do SO
            so.execTick();

            // Avança o relógio
            relogio.tick();
        }


    }

    public void execSOPassoAPasso(){
        carregarTarefasDoArquivoDeConfiguracao();

        Terminal.println("Executando Tudo");
        Terminal.println("Lista de Tarefas carregadas do arquivo de configuração:");

        Terminal.printaListaTarefa(tarefasDoArquivoDeConfiguracao);
        Terminal.debugPrintln("Pronto para iniciar a simulacao?");
        Terminal.esperar();
        so = new SistemaOperacional(tarefasDoArquivoDeConfiguracao, "PRIORIDADE_PREEMPTIVO", 3, 1);
        while (!so.terminouTodasTarefas()) {
            // Executa uma unidade de tempo do SO
            so.execTick();

            Terminal.debugPrintln("Executar tick?");
            Terminal.esperar();

            // Avança o relógio
            relogio.tick();
        }

    }

}