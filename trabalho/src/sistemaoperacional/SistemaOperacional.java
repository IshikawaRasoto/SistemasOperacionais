package sistemaoperacional;

import hardware.CPU;
import hardware.Processador;
import hardware.EstadoCPU;

import modelo.Tarefa;
import modelo.TCB;
import modelo.EstadoTarefa;

import simulador.Relogio;

import sistemaoperacional.nucleo.Escalonador;

import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

import ui.Terminal;

public class SistemaOperacional {

    // Configuracoes do SO
    private String algoritmoEscalonador = "";
    private int quantum = 0;
    private ArrayList<Tarefa> tarefasParaCriar; // Lista das tarefas que devem ser criadas ao longo da execucao da simulacao.

    // Instancias
    private Escalonador escalonador;
    private Processador processador;
    private Relogio relogio = null;

    // Listas de TCBs
    private Queue<TCB> listaTCBs; // Lista de todas as TCBs criadas no sistema.
    private Queue<TCB> listaProntos; // Lista de TCBs que estao no estado PRONTO.



    public SistemaOperacional(ArrayList<Tarefa> tarefasParaCriar, String algoritmoEscalonador, int quantum, int numeroDeNucleos) {
        this.tarefasParaCriar = tarefasParaCriar;
        this.algoritmoEscalonador = algoritmoEscalonador;
        this.quantum = quantum;
        this.escalonador = new Escalonador(algoritmoEscalonador, quantum);
        this.processador = new Processador(numeroDeNucleos);
        this.listaTCBs = new LinkedList<>();
        this.listaProntos = new LinkedList<>();

        this.relogio = Relogio.getInstancia();

        Terminal.println("SO configurado: ");
        Terminal.println("- Algoritmo de escalonamento: " + algoritmoEscalonador);
        Terminal.println("- Quantum: " + quantum);
        Terminal.println("- Numero de nucleos: " + numeroDeNucleos);
        Terminal.println("- Tarefas a serem criadas: ");
        Terminal.printaListaTarefa(tarefasParaCriar);
        Terminal.esperar();
    }

    public void execTick() {

        Terminal.resumePrintln("==============================");
        Terminal.resumePrintln("Tick: " + relogio.getTickAtual());

        Terminal.println("--- Executando tick do SO ---");
        Terminal.println("Tick atual: " + relogio.getTickAtual());

        // 1. Criar novas tarefas conforme o tempo do relogio
        Terminal.debugPrintln("Ir para criacao de tarefas?");
        Terminal.esperar();
        criarTarefas();

        // 2. Verificar e escalonar tarefas
        Terminal.debugPrintln("Ir para verificacao e escalonamento?");
        Terminal.esperar();
        verificarTarefasProcessandoEEscalonar();

        Terminal.resumeEsperar();

        // 3. Atualizar o estado das tarefas em execucao
        Terminal.println("--- Atualizando estado das tarefas em execucao ---");
        processador.executarProcessos();

    }

    public void executarProcessos(){
        processador.executarProcessos();
    }

    public void criarTarefas() {
        // Implementar a criacao de tarefas conforme o tempo do relogio
        // Se a tarefa for criada, retira da lista de tarefas a serem criadas.

        Terminal.println("--- Criando tarefas ---");

        ArrayList<Tarefa> tarefasRemovidas = new ArrayList<>();
        for (Tarefa tarefa : tarefasParaCriar) {
            if (tarefa.getInicio() == relogio.getTickAtual()) {
                TCB novoTCB = new TCB(tarefa);
                listaTCBs.add(novoTCB);
                listaProntos.add(novoTCB);
                tarefasRemovidas.add(tarefa);
            }
        }
        tarefasParaCriar.removeAll(tarefasRemovidas);

        Terminal.println("Tarefas criadas neste tick: ");
        Terminal.printaListaTarefa(tarefasRemovidas);
    }

    public void verificarTarefasProcessandoEEscalonar(){

        Terminal.println("--- Verificando tarefas em execucao e escalonando ---");
        Terminal.println("Lista de prontos antes do escalonamento: ");
        Terminal.printaListaTCB(listaProntos);

        for (CPU cpu : processador.getNucleos()) {

            Terminal.println("Processo no nucleo atual:");
            Terminal.printaTCB(cpu.getTarefaAtual());

            TCB proximaTarefa = escalonador.escolherTarefaParaExecucao(listaProntos, cpu.getTarefaAtual());

            if (proximaTarefa != null) {
                Terminal.println("Tarefa escolhida para execucao neste nucleo:");
                Terminal.printaTCB(proximaTarefa);
                if (cpu.getEstado() == EstadoCPU.OCIOSA) {
                    cpu.novoProcesso(proximaTarefa);
                    proximaTarefa.entrarNoProcessador();
                    listaProntos.remove(proximaTarefa);
                } else if (proximaTarefa != cpu.getTarefaAtual()) {
                    if(cpu.getTarefaAtual().getEstadoTarefa() != EstadoTarefa.FINALIZADA) {
                        listaProntos.add(cpu.getTarefaAtual());
                    }
                    cpu.getTarefaAtual().sairDoProcessador();
                    cpu.finalizarProcesso();
                    cpu.novoProcesso(proximaTarefa);
                    proximaTarefa.entrarNoProcessador();
                    listaProntos.remove(proximaTarefa);
                }
            }else{
                Terminal.println("Nenhuma tarefa escolhida para execucao neste nucleo.");
                if(cpu.getEstado() == EstadoCPU.OCUPADA){
                    if(cpu.getTarefaAtual().getEstadoTarefa() == EstadoTarefa.FINALIZADA){
                        cpu.getTarefaAtual().sairDoProcessador();
                        cpu.finalizarProcesso();
                    }
                }
            }
            Terminal.resumePrintListaPronta(listaProntos);
            Terminal.resumePrintTarefaExecutando(cpu.getTarefaAtual());
        }
    }

    // TODO - verificar Tarefas suspensas para Defesa B

    public boolean terminouTodasTarefas() {
        if (!tarefasParaCriar.isEmpty()) {
            return false;
        }
        for (TCB tcb : listaTCBs) {
            if (tcb.getEstadoTarefa() != EstadoTarefa.FINALIZADA) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<TCB> converteFilaParaLista(Queue<TCB> fila) {
        return new ArrayList<>(fila);
    }

    public ArrayList<TCB> getListaTCBs() {
        return converteFilaParaLista(listaTCBs);
    }

    public int getTickAtual() {
        return relogio.getTickAtual();
    }
}

