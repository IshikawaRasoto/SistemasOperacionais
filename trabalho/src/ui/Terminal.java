package ui;

import modelo.Tarefa;
import modelo.TCB;

import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

// Essa classe eh utilizada para o desenvolvimento do sistema e debug


public class Terminal {

    private static final boolean verbose = false;
    private static final boolean debug = false;
    private static final boolean resume = true;

    static public void printaTarefa(Tarefa tarefa){
        if (!verbose) return;
        System.out.print(tarefa.resumo());
    }

    static public void printaListaTarefa(ArrayList<Tarefa> listaTarefas){
        if (!verbose) return;
        for (Tarefa tarefa : listaTarefas) {
            printaTarefa(tarefa);
            System.out.println();
        }
    }

    static public void printaTCB(TCB tcb){
        if (!verbose) return;
        if (tcb == null){
            System.out.println("TCB nulo");
            return;
        }
        System.out.println("Tarefa ID: " + tcb.tarefa.getId());
        System.out.println("Estado: " + tcb.getEstadoTarefa());
        System.out.println("Restante: " + tcb.getRestante());
        System.out.println("Quantum Usado: " + tcb.getQuantumUsado());
        System.out.println("Tick Entrada Fila Pronta: " + tcb.getTickEntradaFilaPronta());
        System.out.println("Tick Entrada Processador: " + tcb.getTickEntradaProcessador());
        System.out.println("Tick Termino: " + tcb.getTickTermino());
        System.out.println("Inicio Fatia Atual: " + tcb.getInicioFatiaAtual());
        System.out.println("Espera Acumulada: " + tcb.getEsperaAcumulada());
    }

    static public void printaListaTCB(Queue<TCB> listaTCBs){
        if (!verbose) return;
        for (TCB tcb : listaTCBs) {
            printaTCB(tcb);
            System.out.println();
        }
    }

    static public void print(String linha){
        if (!verbose) return;
        System.out.print(linha);
    }

    static public void println(String linha){
        if (!verbose) return;
        System.out.println(linha);
    }

    static public void debugPrint(String linha){
        if (!debug) return;
        System.out.print(linha);
    }

    static public void debugPrintln(String linha){
        if (!debug) return;
        System.out.println(linha);
    }

    static public void esperar(){
        if (!debug) return;
        try {
            System.out.println("Pressione Enter para continuar...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void resumePrintln(String linha){
        if (!resume) return;
        System.out.println(linha);
    }

    static public void resumePrintTarefa(Tarefa tarefa){
        if (!resume) return;
        System.out.print(tarefa.resumo());
    }

    static public void resumePrintListaPronta(Queue<TCB> listaProntos){
        if (!resume) return;
        System.out.println("Lista de Tarefas na Fila Pronta:");
        for (TCB tcb : listaProntos) {
            System.out.print("- ");
            System.out.print(tcb.tarefa.resumo());
            System.out.println(" Quantum: " + tcb.getQuantumUsado() + "| rest:" + tcb.getRestante());
        }
    }

    static public void resumePrintTarefaExecutando(TCB tcb){
        if (!resume) return;
        if (tcb == null){
            System.out.println("Nenhuma tarefa em execucao.");
            return;
        }
        System.out.print("Tarefa em execucao: ");
        System.out.print(tcb.tarefa.resumo());
        System.out.println("Quantum: " + tcb.getQuantumUsado() + "| rest:" + tcb.getRestante());

    }

    static public void resumeEsperar() {
        if (!resume) return;
        try {
            System.out.println("Pressione Enter para continuar...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}