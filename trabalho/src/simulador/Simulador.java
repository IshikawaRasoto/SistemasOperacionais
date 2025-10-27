package simulador;

import sistemaoperacional.SistemaOperacional;

import configuracao.LeitorDeConfiguracao;

import modelo.Tarefa;

import simulador.Relogio;

import java.util.ArrayList;


public class Simulador {

    private SistemaOperacional so;
    private LeitorDeConfiguracao leitor;

    public Simulador(String caminhoDoArquivoDeConfiguracao) {
        //this.leitor = new LeitorDeConfiguracao(caminhoDoArquivoDeConfiguracao);
        this.so = new SistemaOperacional();


    }
    
    private void carregarTarefasDoArquivoDeConfiguracao(){
        ArrayList<Tarefa> tarefasDoArquivoDeConfiguracao = new ArrayList<>();
        //tarefasDoArquivoDeConfiguracao = leitor.lerTarefas();
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t01", 0,0,5,3));
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t02", 1,0,3,2));
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t03", 2,2,6,4));
        tarefasDoArquivoDeConfiguracao.add(new Tarefa("t04", 3,4,2,9));
        so.setTarefasParaCriar(tarefasDoArquivoDeConfiguracao);
    }




    public void exec(){
        carregarTarefasDoArquivoDeConfiguracao();
        while (!so.terminouTodasTarefas()) {
            // Avança o relógio
            Relogio.getInstancia().tick();
            // Executa uma unidade de tempo do SO
            so.exec();
        }

    }

}