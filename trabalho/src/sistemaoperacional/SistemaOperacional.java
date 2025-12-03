package sistemaoperacional;

import hardware.CPU;
import hardware.Processador;
import modelo.Tarefa;
import modelo.TCB;
import modelo.EstadoTarefa;
import simulador.Relogio;
import sistemaoperacional.nucleo.AlgoritmosEscalonamento;
import sistemaoperacional.nucleo.Escalonador;
import sistemaoperacional.nucleo.CausaEscalonamento;
import ui.Terminal;

import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class SistemaOperacional {

    private String algoritmoEscalonador = "";
    private int quantum = 0;
    private int alpha = 0;
    private ArrayList<Tarefa> tarefasParaCriar;
    private Escalonador escalonador;
    private Processador processador;
    private Relogio relogio;
    private Queue<TCB> listaTCBs;
    private Queue<TCB> listaProntos;
    private boolean houveInsercaoDeTarefas = false;

    public SistemaOperacional(ArrayList<Tarefa> tarefasParaCriar, String algoritmoEscalonador, int quantum, int alpha, int numeroDeNucleos) {
        this.tarefasParaCriar = tarefasParaCriar;
        this.algoritmoEscalonador = algoritmoEscalonador;
        this.quantum = quantum;
        this.alpha = alpha;
        this.escalonador = new Escalonador(algoritmoEscalonador, quantum);
        this.processador = new Processador(numeroDeNucleos);
        this.listaTCBs = new LinkedList<>();
        this.listaProntos = new LinkedList<>();
        this.relogio = Relogio.getInstancia();
        Terminal.println("SO configurado: " + algoritmoEscalonador + " | Q: " + quantum + " | Alpha: " + alpha);
    }

    public SistemaOperacional(ArrayList<Tarefa> tarefasParaCriar, String algoritmoEscalonador, int quantum, int numeroDeNucleos) {
        this(tarefasParaCriar, algoritmoEscalonador, quantum, 0, numeroDeNucleos);
    }

    public void execTick() {
        criarTarefas();
        verificarTarefasProcessandoEEscalonar();
        processador.executarProcessos();
    }

    private void aplicarEnvelhecimento() {
        if (escalonador.getAlgoritmoEscolhido() == AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO && alpha > 0) {
            for (TCB tcb : listaProntos) {
                tcb.envelhecer(alpha);
            }
        }
    }

    public void criarTarefas() {
        this.houveInsercaoDeTarefas = false;
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
        if (!tarefasRemovidas.isEmpty()) {
            this.houveInsercaoDeTarefas = true;
            Terminal.println("Tarefas criadas: " + tarefasRemovidas.size());
        }
    }

    public void verificarTarefasProcessandoEEscalonar(){

        aplicarEnvelhecimento();

        for (CPU cpu : processador.getNucleos()) {
            TCB tarefaAtual = cpu.getTarefaAtual();
            CausaEscalonamento causa = null;

            if (tarefaAtual == null) {
                causa = CausaEscalonamento.CPU_OCIOSA;
            } else if (tarefaAtual.getEstadoTarefa() == EstadoTarefa.FINALIZADA) {
                causa = CausaEscalonamento.TAREFA_FINALIZADA;
            } else if (quantum > 0 && (relogio.getTickAtual() - tarefaAtual.getInicioFatiaAtual()) >= quantum) {
                causa = CausaEscalonamento.QUANTUM_EXPIRADO;
            } else if (houveInsercaoDeTarefas) {
                causa = CausaEscalonamento.NOVA_TAREFA;
            }
            // CORREÇÃO: Se temos envelhecimento (alpha > 0), devemos verificar SEMPRE (a cada tick)
            // se a prioridade de alguém na fila superou a da tarefa atual.
            else if (alpha > 0 && escalonador.getAlgoritmoEscolhido() == AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO) {
                causa = CausaEscalonamento.ENVELHECIMENTO;
            }

            // Se não houve causa relevante (e não é o algoritmo de envelhecimento), segue o jogo
            if (causa == null) continue;

            boolean deveEscalonar = escalonador.deveTrocarContexto(tarefaAtual, listaProntos, causa);

            if (deveEscalonar) {
                // Só loga se não for apenas verificação periódica que resultou em true (para não poluir tanto)
                // ou loga tudo para debug:
                if (causa != CausaEscalonamento.ENVELHECIMENTO) {
                    Terminal.println("Troca de contexto. Causa: " + causa);
                } else {
                    Terminal.debugPrintln("Preempção por Envelhecimento!");
                }
                realizarTrocaDeContexto(cpu, tarefaAtual);
            }
        }
    }

    private void realizarTrocaDeContexto(CPU cpu, TCB tarefaAtual) {
        if (tarefaAtual != null) {
            tarefaAtual.sairDoProcessador();
            cpu.finalizarProcesso();

            if (tarefaAtual.getEstadoTarefa() != EstadoTarefa.FINALIZADA) {
                listaProntos.add(tarefaAtual);
            }
        }

        TCB proximaTarefa = escalonador.escolherProximaTarefa(listaProntos);

        if (proximaTarefa != null) {
            cpu.novoProcesso(proximaTarefa);
            proximaTarefa.entrarNoProcessador();
        }
    }

    public void executarProcessos(){ processador.executarProcessos(); }

    public boolean terminouTodasTarefas() {
        if (!tarefasParaCriar.isEmpty()) return false;
        for (TCB tcb : listaTCBs) {
            if (tcb.getEstadoTarefa() != EstadoTarefa.FINALIZADA) return false;
        }
        return true;
    }

    public ArrayList<TCB> getListaTCBs() { return new ArrayList<>(listaTCBs); }
    public int getTickAtual() { return relogio.getTickAtual(); }
}