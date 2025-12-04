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

        processarEventosDeTarefasFinalizadas();

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

    private void processarEventosDeTarefasFinalizadas() {
        for (CPU cpu : processador.getNucleos()) {
            TCB tarefa = cpu.getTarefaAtual();
            // Se a tarefa terminou neste último tick, processa seus eventos finais agora
            if (tarefa != null && tarefa.getEstadoTarefa() == EstadoTarefa.FINALIZADA) {
                Terminal.println("DEBUG: Verificando eventos finais para tarefa " + tarefa.getTarefa().getId());
                verificarEProcessarEventosNaCPU(cpu, tarefa);
            }
        }
    }

    public void gerenciarEventosEBloqueios() {
        // --- PARTE 1: Desbloqueio de I/O (Mantido igual à sua versão corrigida) ---
        for (TCB tcb : listaTCBs) {
            boolean algumIOFinalizou = tcb.processarTicksIO();
            if (algumIOFinalizou) {
                tcb.finalizarIO();
                if (tcb.getEstadoTarefa() == EstadoTarefa.PRONTA && !listaProntos.contains(tcb)) {
                    listaProntos.add(tcb);
                    Terminal.println("DEBUG: Tarefa " + tcb.getTarefa().getId() + " desbloqueada (Fim de I/O e sem Mutex pendente).");
                }
            }
        }

        // --- PARTE 2: Disparo de Eventos (Refatorado) ---
        for (CPU cpu : processador.getNucleos()) {
            TCB tarefaExecutando = cpu.getTarefaAtual();

            if (tarefaExecutando != null && tarefaExecutando.getEstadoTarefa() == EstadoTarefa.EXECUTANDO) {
                verificarEProcessarEventosNaCPU(cpu, tarefaExecutando);
            }
        }
    }

    private void verificarEProcessarEventosNaCPU(CPU cpu, TCB tarefa) {
        java.util.List<Evento> eventos = tarefa.verificarEventosAtuais();

        if (!eventos.isEmpty()) {
            for (Evento evento : eventos) {
                // Se a tarefa está FINALIZADA, ela só pode processar liberação de recursos (Mutex Unlock)
                // Bloqueios (IO/Lock) em tarefa finalizada não fazem sentido e seriam ignorados naturalmente,
                // mas a liberação é crítica.
                processarEvento(cpu, tarefa, evento);
            }
        }
    }

    private void processarEvento(CPU cpu, TCB tarefa, Evento evento) {
        Terminal.println("Evento disparado: " + evento + " na tarefa " + tarefa.getTarefa().getId());

        switch (evento.getTipo()) {
            case IO:
                // Bloqueia a tarefa
                tarefa.sairDoProcessador();
                cpu.finalizarProcesso();

                // ALTERADO: Passa o objeto evento inteiro para o TCB registrar no mapa
                tarefa.bloquearPorIO(evento);

                Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " bloqueada por I/O (" + evento.getDuracao() + " ticks)");
                break;

            case MUTEX_SOLICITACAO:
                boolean conseguiu = gerenciadorRecursos.solicitarMutex(evento.getIdRecurso(), tarefa);
                if (!conseguiu) {
                    tarefa.sairDoProcessador();
                    cpu.finalizarProcesso();
                    tarefa.bloquearPorMutex();
                    Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " bloqueada aguardando Mutex " + evento.getIdRecurso());
                } else {
                    Terminal.println("Tarefa " + tarefa.getTarefa().getId() + " adquiriu Mutex " + evento.getIdRecurso());
                }
                break;

            case MUTEX_LIBERACAO:
                TCB desbloqueada = gerenciadorRecursos.liberarMutex(evento.getIdRecurso(), tarefa);
                if (desbloqueada != null) {
                    // Avisa o TCB que ele ganhou o Mutex
                    desbloqueada.receberMutex();

                    // Se isso foi suficiente para desbloqueá-la (sem IO pendente), põe na fila
                    if (desbloqueada.getEstadoTarefa() == EstadoTarefa.PRONTA) {
                        listaProntos.add(desbloqueada);
                        Terminal.println("Tarefa " + desbloqueada.getTarefa().getId() + " pronta para executar.");
                    } else {
                        Terminal.println("Tarefa " + desbloqueada.getTarefa().getId() + " ganhou Mutex, mas continua bloqueada por I/O.");
                    }
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
                // A tarefa acabou. Antes de o escalonador removê-la, verificamos se
                // há algum evento de "despedida" (ex: liberar Mutex no último tick).

                Terminal.println("DEBUG: Tarefa " + tarefaAtual.getTarefa().getId() + " finalizou. Verificando eventos finais...");
                java.util.List<Evento> eventosFinais = tarefaAtual.verificarEventosAtuais();

                for (Evento e : eventosFinais) {
                    // Só processamos liberações de recurso ou logs.
                    // Bloqueios (IO/ML) não fazem sentido pois ela já acabou.
                    if (e.getTipo() == TipoEvento.MUTEX_LIBERACAO) {
                        processarEvento(cpu, tarefaAtual, e);
                    }
                }
                // ---------------------

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