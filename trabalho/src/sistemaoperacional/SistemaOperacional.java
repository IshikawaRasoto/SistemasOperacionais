package sistemaoperacional;

import hardware.CPU;
import hardware.Processador;
import modelo.Evento;
import modelo.Tarefa;
import modelo.TCB;
import modelo.EstadoTarefa;
import modelo.TipoEvento;
import simulador.Relogio;
import sistemaoperacional.nucleo.AlgoritmosEscalonamento;
import sistemaoperacional.nucleo.Escalonador;
import sistemaoperacional.nucleo.CausaEscalonamento;
import sistemaoperacional.nucleo.GerenciadorRecursos; // Importe o novo gerenciador
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

    // NOVO: Gerenciador de Recursos
    private GerenciadorRecursos gerenciadorRecursos;

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

        // Inicializa o Gerenciador
        this.gerenciadorRecursos = new GerenciadorRecursos();

        Terminal.println("SO configurado: " + algoritmoEscalonador + " | Q: " + quantum + " | Alpha: " + alpha);
    }

    // Construtor auxiliar mantido
    public SistemaOperacional(ArrayList<Tarefa> tarefasParaCriar, String algoritmoEscalonador, int quantum, int numeroDeNucleos) {
        this(tarefasParaCriar, algoritmoEscalonador, quantum, 0, numeroDeNucleos);
    }

    public void execTick() {
        // 1. Admitir novas tarefas
        criarTarefas();

        // 2. Escalonador: Decide quem deve estar na CPU agora
        // (Se a CPU estava vazia, ele coloca a T01 aqui)
        verificarTarefasProcessandoEEscalonar();

        // 3. Gerenciador de Eventos: Verifica eventos da tarefa que ACABOU de ser escalonada
        // Isso garante que eventos no tempo 0 (logo que entra) sejam processados
        gerenciarEventosEBloqueios();

        // 4. Hardware: Roda o ciclo de clock na CPU (se tiver alguém lá)
        processador.executarProcessos();
    }

    private void aplicarEnvelhecimento() {
        if (escalonador.getAlgoritmoEscolhido() == AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO && alpha > 0) {
            for (TCB tcb : listaProntos) {
                tcb.envelhecer(alpha);
            }
        }
    }

    public void gerenciarEventosEBloqueios() {
        // --- PARTE 1: Desbloqueio de I/O ---
        for (TCB tcb : listaTCBs) {
            if (tcb.getEstadoTarefa() == EstadoTarefa.BLOQUEADA) {
                if (!tcb.acabouTempoBloqueio()) {
                    tcb.decrementarTempoBloqueio();
                    if (tcb.acabouTempoBloqueio()) {
                        tcb.desbloquear();
                        listaProntos.add(tcb);
                        Terminal.println("DEBUG: Tarefa " + tcb.getTarefa().getId() + " ACORDOU do I/O.");
                    }
                }
            }
        }

        // --- PARTE 2: Disparo de Eventos ---
        for (CPU cpu : processador.getNucleos()) {
            TCB tarefaExecutando = cpu.getTarefaAtual();

            // DEBUG 1: Quem está na CPU?
            if (tarefaExecutando == null) {
                Terminal.println("DEBUG: CPU Ociosa neste tick.");
            } else {
                Terminal.println("DEBUG: CPU tem " + tarefaExecutando.getTarefa().getId() +
                        " | Estado: " + tarefaExecutando.getEstadoTarefa() +
                        " | TempoExec: " + tarefaExecutando.getTempoExecutado());

                // Se a tarefa não estiver EXECUTANDO, algo está errado com o estado
                if (tarefaExecutando.getEstadoTarefa() == EstadoTarefa.EXECUTANDO) {
                    Evento evento = tarefaExecutando.verificarEventoAtual();

                    // DEBUG 2: Achou evento?
                    if (evento != null) {
                        Terminal.println("DEBUG: !!! EVENTO ENCONTRADO: " + evento);
                        processarEvento(cpu, tarefaExecutando, evento);
                    } else {
                        Terminal.println("DEBUG: Nenhum evento para agora.");
                    }
                }
            }
        }
    }

    private void processarEvento(CPU cpu, TCB tarefa, Evento evento) {
        Terminal.println("Evento disparado: " + evento + " na tarefa " + tarefa.getTarefa().getId());

        switch (evento.getTipo()) {
            case IO:
                // Bloqueia a tarefa
                tarefa.sairDoProcessador(); // Sai da CPU
                cpu.finalizarProcesso();    // CPU fica ociosa
                tarefa.bloquearPorIO(evento.getDuracao()); // Define tempo e estado BLOQUEADA
                // Não adiciona na listaProntos (está bloqueada)
                Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " bloqueada por I/O (" + evento.getDuracao() + " ticks)");
                break;

            case MUTEX_SOLICITACAO:
                boolean conseguiu = gerenciadorRecursos.solicitarMutex(evento.getIdRecurso(), tarefa);
                if (!conseguiu) {
                    tarefa.sairDoProcessador();
                    cpu.finalizarProcesso();
                    tarefa.bloquearPorMutex(); // Estado BLOQUEADA (sem tempo definido)
                    Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " bloqueada aguardando Mutex " + evento.getIdRecurso());
                } else {
                    Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " adquiriu Mutex " + evento.getIdRecurso());
                }
                break;

            case MUTEX_LIBERACAO:
                TCB desbloqueada = gerenciadorRecursos.liberarMutex(evento.getIdRecurso(), tarefa);
                Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " liberou Mutex " + evento.getIdRecurso());

                if (desbloqueada != null) {
                    desbloqueada.desbloquear(); // Muda estado para PRONTA
                    listaProntos.add(desbloqueada); // Volta para o escalonador
                    Terminal.println("Tarefa " + desbloqueada.getTarefa().getId() + " foi desbloqueada (ganhou o Mutex).");

                    // Opcional: Se a política for preempção imediata ao liberar recurso, poderia chamar o escalonador aqui.
                    // Por padrão, seguimos o fluxo normal (tick termina, escalonador decide na próxima etapa).
                }
                break;
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

            // SE A CPU ESTIVER OCUPADA, MAS A TAREFA ESTIVER BLOQUEADA (SAIU POR EVENTO),
            // Precisamos garantir que o escalonador saiba que a vaga abriu.
            // A lógica `processarEvento` já tirou da CPU (cpu.finalizarProcesso),
            // então `tarefaAtual` será NULL se houve bloqueio neste tick.

            // Re-pega a tarefa atual caso tenha mudado no `processarEvento`
            tarefaAtual = cpu.getTarefaAtual();

            CausaEscalonamento causa = null;

            if (tarefaAtual == null) {
                causa = CausaEscalonamento.CPU_OCIOSA;
            } else if (tarefaAtual.getEstadoTarefa() == EstadoTarefa.FINALIZADA) {
                causa = CausaEscalonamento.TAREFA_FINALIZADA;
            } else if (quantum > 0 && (relogio.getTickAtual() - tarefaAtual.getInicioFatiaAtual()) >= quantum) {
                causa = CausaEscalonamento.QUANTUM_EXPIRADO;
            } else if (houveInsercaoDeTarefas) {
                causa = CausaEscalonamento.NOVA_TAREFA;
            } else if (alpha > 0 && escalonador.getAlgoritmoEscolhido() == AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO) {
                causa = CausaEscalonamento.ENVELHECIMENTO;
            }

            if (causa == null) continue;

            // Se a CPU está ociosa (pq a tarefa bloqueou), o escalonador DEVE rodar para encher a vaga
            boolean deveEscalonar = (causa == CausaEscalonamento.CPU_OCIOSA) ||
                    escalonador.deveTrocarContexto(tarefaAtual, listaProntos, causa);

            if (deveEscalonar) {
                realizarTrocaDeContexto(cpu, tarefaAtual);
            }
        }
    }

    private void realizarTrocaDeContexto(CPU cpu, TCB tarefaAtual) {
        if (tarefaAtual != null) {
            tarefaAtual.sairDoProcessador();
            cpu.finalizarProcesso();

            // SÓ devolve pra fila de prontos se NÃO estiver Finalizada E NÃO estiver Bloqueada
            if (tarefaAtual.getEstadoTarefa() != EstadoTarefa.FINALIZADA &&
                    tarefaAtual.getEstadoTarefa() != EstadoTarefa.BLOQUEADA) {
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