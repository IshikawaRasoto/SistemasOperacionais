package sistemaoperacional;

import hardware.CPU;
import hardware.Processador;
import hardware.EstadoCPU;
import modelo.Tarefa;
import modelo.TCB;
import modelo.EstadoTarefa;
import simulador.Relogio;
import sistemaoperacional.nucleo.Escalonador;
import sistemaoperacional.nucleo.CausaEscalonamento;
import ui.Terminal;

import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class SistemaOperacional {

    // ... (Atributos permanecem iguais: algoritmoEscalonador, quantum, etc) ...
    private String algoritmoEscalonador = "";
    private int quantum = 0;
    private ArrayList<Tarefa> tarefasParaCriar;
    private Escalonador escalonador;
    private Processador processador;
    private Relogio relogio;
    private Queue<TCB> listaTCBs;
    private Queue<TCB> listaProntos;
    private boolean houveInsercaoDeTarefas = false;

    public SistemaOperacional(ArrayList<Tarefa> tarefasParaCriar, String algoritmoEscalonador, int quantum, int numeroDeNucleos) {
        // ... (Construtor permanece igual) ...
        this.tarefasParaCriar = tarefasParaCriar;
        this.algoritmoEscalonador = algoritmoEscalonador;
        this.quantum = quantum;
        this.escalonador = new Escalonador(algoritmoEscalonador, quantum);
        this.processador = new Processador(numeroDeNucleos);
        this.listaTCBs = new LinkedList<>();
        this.listaProntos = new LinkedList<>();
        this.relogio = Relogio.getInstancia();
        Terminal.println("SO configurado: " + algoritmoEscalonador + " | Quantum: " + quantum);
    }

    public void execTick() {
        // 1. Criar tarefas
        criarTarefas();

        // 2. Escalonar
        verificarTarefasProcessandoEEscalonar();

        // 3. Executar Hardware
        processador.executarProcessos();
    }

    // ... (métodos executarProcessos e criarTarefas permanecem iguais) ...
    public void executarProcessos(){ processador.executarProcessos(); }

    public void criarTarefas() {
        this.houveInsercaoDeTarefas = false;
        ArrayList<Tarefa> tarefasRemovidas = new ArrayList<>();
        for (Tarefa tarefa : tarefasParaCriar) {
            if (tarefa.getInicio() == relogio.getTickAtual()) {
                TCB novoTCB = new TCB(tarefa);
                listaTCBs.add(novoTCB);
                listaProntos.add(novoTCB); // Entra no final da fila
                tarefasRemovidas.add(tarefa);
            }
        }
        tarefasParaCriar.removeAll(tarefasRemovidas);
        if (!tarefasRemovidas.isEmpty()) {
            this.houveInsercaoDeTarefas = true;
            Terminal.println("Tarefas criadas: " + tarefasRemovidas.size());
        }
    }


    /**
     * Lógica de Tempo Compartilhado:
     * 1. Verifica se precisa interromper a tarefa atual (Quantum, Fim, etc).
     * 2. Se interromper: Salva contexto (devolve p/ fila) -> Escolhe nova -> Restaura contexto.
     */
    public void verificarTarefasProcessandoEEscalonar(){

        for (CPU cpu : processador.getNucleos()) {
            TCB tarefaAtual = cpu.getTarefaAtual();

            // 1. Identificar a CAUSA (o Evento)
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

            // Se não houve nenhum evento relevante, pula para o próximo núcleo
            if (causa == null) continue;

            // 2. Perguntar ao Escalonador se deve agir baseada na causa
            boolean deveEscalonar = escalonador.deveTrocarContexto(tarefaAtual, listaProntos, causa);

            if (deveEscalonar) {
                Terminal.println("Escalonador decidiu trocar. Motivo: " + causa);

                // Lógica de troca de contexto (igual a antes: tira a atual, devolve pra fila, pega a próxima)
                realizarTrocaDeContexto(cpu, tarefaAtual);
            } else {
                Terminal.println("Evento " + causa + " ignorado pela política do escalonador.");
            }
        }
    }

    private void realizarTrocaDeContexto(CPU cpu, TCB tarefaAtual) {
        // 1. Salvar contexto (Tirar da CPU)
        if (tarefaAtual != null) {
            tarefaAtual.sairDoProcessador(); // Atualiza contadores do TCB
            cpu.finalizarProcesso();         // Deixa a CPU ociosa temporariamente

            // 2. Se a tarefa não acabou, ela volta para o fim da fila de prontos
            if (tarefaAtual.getEstadoTarefa() != EstadoTarefa.FINALIZADA) {
                Terminal.println("Preempção: Devolvendo " + tarefaAtual.getTarefa().getId() + " para fila de prontos.");
                listaProntos.add(tarefaAtual);
            } else {
                Terminal.println("Tarefa " + tarefaAtual.getTarefa().getId() + " finalizada.");
            }
        }

        // 3. Escolher a próxima (O escalonador já remove da fila ao escolher)
        TCB proximaTarefa = escalonador.escolherProximaTarefa(listaProntos);

        // 4. Carregar contexto (Colocar na CPU)
        if (proximaTarefa != null) {
            Terminal.println("Selecionada para CPU: " + proximaTarefa.getTarefa().getId());
            cpu.novoProcesso(proximaTarefa);
            proximaTarefa.entrarNoProcessador();
        } else {
            Terminal.println("Nenhuma tarefa pronta para assumir a CPU.");
        }
    }

    private boolean verificarNecessidadeEscalonamento(TCB tarefaAtual) {
        // 1. CPU Ociosa: precisa buscar tarefa
        if (tarefaAtual == null) return !listaProntos.isEmpty();

        // 2. Tarefa acabou: libera CPU
        if (tarefaAtual.getEstadoTarefa() == EstadoTarefa.FINALIZADA) return true;

        // 3. Quantum Expirado: Todos os algoritmos (RR, SRTF, Prioridade) respeitam o Quantum neste modelo
        if (quantum > 0) {
            int tempoExecutado = relogio.getTickAtual() - tarefaAtual.getInicioFatiaAtual();
            if (tempoExecutado >= quantum) {
                Terminal.debugPrintln("Interrupt: Quantum (" + quantum + ")");
                return true;
            }
        }

        // 4. Chegada de nova tarefa (Preempção por Prioridade/SRTF imediata?)
        // Se o professor pediu estritamente Time-Sharing controlado por Quantum,
        // talvez ele não queira preempção imediata na chegada, apenas no fim do Quantum.
        // MAS, geralmente SRTF preempta na chegada. Vou manter, mas você pode remover se for "Quantum-Only".
        if (houveInsercaoDeTarefas) {
            return true;
        }

        return false;
    }

    // ... (Restante da classe igual: terminouTodasTarefas, getters, etc) ...
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