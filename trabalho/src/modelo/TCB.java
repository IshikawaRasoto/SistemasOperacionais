package modelo;

import simulador.Relogio;
import ui.Terminal;

import java.util.*;

public class TCB {
    public Tarefa tarefa;
    private EstadoTarefa estadoTarefa;
    private int restante = 0;
    private int quantumUsado = 0;
    private int tickEntradaFilaPronta = 0;
    private int tickEntradaProcessador = -1;
    private int tickTermino = -1;
    private int inicioFatiaAtual = -1;
    private int esperaAcumulada = 0;
    private int prioridadeDinamica;
    private boolean bloqueadoPorMutex = false;
    private Map<Evento, Integer> eventosIOEmAndamento;
    private Set<Evento> eventosConcluidos;

    private final Relogio relogio = Relogio.getInstancia();

    public TCB(Tarefa tarefa) {
        this.tarefa = tarefa;
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.restante = tarefa.getDuracaoTotal();
        this.quantumUsado = 0;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
        // Inicializa com a prioridade estática (original)
        this.prioridadeDinamica = tarefa.getPrioridade();
        this.eventosIOEmAndamento = new HashMap<>();
    }

    // Calcula quanto tempo "útil" a tarefa já rodou
    public int getTempoExecutado() {
        return tarefa.getDuracaoTotal() - restante;
    }

    // Verifica se existe algum evento agendado EXATAMENTE para este instante de execução
    public List<Evento> verificarEventosAtuais() {
        int tempoAtualExecucao = getTempoExecutado();
        List<Evento> eventosDaTarefa = tarefa.getEventos();
        List<Evento> eventosDoTick = new ArrayList<>();

        if (eventosDaTarefa == null || eventosDaTarefa.isEmpty()){
            Terminal.println("DEBUG TCB: Tarefa " + tarefa.getId() + " tem " + eventosDoTick.size() + " evento(s) neste tick.");
            return eventosDoTick;
        }

        for (Evento e : eventosDaTarefa) {
            // Verifica se o evento ocorre NESTE tick exato
            if (e.getTempoOcorrencia() == tempoAtualExecucao) {
                // (Opcional) Verifique se ele já não está em andamento no mapa de IO para evitar duplicidade
                if (eventosIOEmAndamento != null && !eventosIOEmAndamento.containsKey(e)) {
                    eventosDoTick.add(e);
                } else if (eventosIOEmAndamento == null) {
                    // Fallback caso não esteja usando a solução anterior do mapa
                    eventosDoTick.add(e);
                }
            }
        }
        Terminal.println("DEBUG TCB: Tarefa " + tarefa.getId() + " tem " + eventosDoTick.size() + " evento(s) neste tick.");
        return eventosDoTick;
    }

    public void bloquearPorIO(Evento evento) {
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        // Salva o evento e sua duração no mapa
        this.eventosIOEmAndamento.put(evento, evento.getDuracao());
    }

    public void bloquearPorMutex() {
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        this.bloqueadoPorMutex = true; // Marca que o Mutex está segurando a tarefa
    }

    public void receberMutex() {
        this.bloqueadoPorMutex = false;
        verificarSePodeDesbloquear();
    }

    public void finalizarIO() {
        // A lógica de remoção do mapa já está no processarTicksIO,
        // mas precisamos checar se podemos mudar o estado global.
        verificarSePodeDesbloquear();
    }

    public void desbloquear() {
        verificarSePodeDesbloquear();
    }

    private void verificarSePodeDesbloquear() {
        // Só volta para PRONTA se NÃO tiver I/O pendente E NÃO estiver esperando Mutex
        if (!temIOPendente() && !bloqueadoPorMutex) {
            this.estadoTarefa = EstadoTarefa.PRONTA;
            this.tickEntradaFilaPronta = Relogio.getInstancia().getTickAtual();
        }
    }

    public boolean processarTicksIO() {
        if (eventosIOEmAndamento.isEmpty()) return false;

        boolean algumIOFinalizou = false;
        Iterator<Map.Entry<Evento, Integer>> iterator = eventosIOEmAndamento.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Evento, Integer> entry = iterator.next();
            int tempoRestante = entry.getValue();

            if (tempoRestante > 0) {
                // Decrementa o tempo deste I/O específico
                entry.setValue(tempoRestante - 1);

                // Verifica novamente após decrementar (caso fosse 1 e virou 0 neste tick)
                if (entry.getValue() == 0) {
                    iterator.remove(); // Remove do mapa de andamento
                    algumIOFinalizou = true;
                    System.out.println("DEBUG TCB: I/O do evento " + entry.getKey() + " finalizado na Tarefa " + tarefa.getId());
                }
            }
        }

        return algumIOFinalizou;
    }

    public boolean temIOPendente() {
        return !eventosIOEmAndamento.isEmpty();
    }

    public void entrarFilaPronta(){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void entrarNoProcessador(){
        this.estadoTarefa = EstadoTarefa.EXECUTANDO;
        this.tickEntradaProcessador = relogio.getTickAtual();
        this.inicioFatiaAtual = relogio.getTickAtual();

        // CORREÇÃO: Rejuvenesce a prioridade ao ganhar a CPU.
        // A tarefa volta para sua prioridade base. Se precisar ganhar a CPU novamente
        // no futuro contra tarefas mais prioritárias, terá que "envelhecer" na fila de novo.
        this.prioridadeDinamica = tarefa.getPrioridade();
    }

    public void sairDoProcessador(){
        if(this.estadoTarefa == EstadoTarefa.FINALIZADA){
            return;
        }
        if (this.estadoTarefa != EstadoTarefa.BLOQUEADA) {
            this.estadoTarefa = EstadoTarefa.PRONTA;
        }
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.inicioFatiaAtual = relogio.getTickAtual();
        this.esperaAcumulada = relogio.getTickAtual();
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void interromperTarefa(){
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        this.inicioFatiaAtual = relogio.getTickAtual();
    }


    public void executarTick(){
        if(this.restante > 0){
            this.restante--;
        }
        this.quantumUsado++;
        if (this.restante == 0){
            this.estadoTarefa = EstadoTarefa.FINALIZADA;
            this.tickTermino = relogio.getTickAtual();
        }
    }

    public void envelhecer(int alpha) {
        this.prioridadeDinamica += alpha;
    }

    // --- Getters e Setters ---
    public void setEstadoTarefa(EstadoTarefa estadoTarefa) { this.estadoTarefa = estadoTarefa; }
    public void setRestante(int restante) { this.restante = restante; }
    public void setTickTermino(int tickTermino) { this.tickTermino = tickTermino; }
    public Tarefa getTarefa() { return tarefa; }
    public EstadoTarefa getEstadoTarefa() { return estadoTarefa; }
    public int getRestante() { return restante; }
    public int getQuantumUsado() { return quantumUsado; }
    public int getTickEntradaFilaPronta() { return tickEntradaFilaPronta; }
    public int getTickEntradaProcessador() { return tickEntradaProcessador; }
    public int getTickTermino() { return tickTermino; }
    public int getInicioFatiaAtual() { return inicioFatiaAtual; }
    public int getEsperaAcumulada() { return esperaAcumulada; }
    public int getPrioridadeDinamica() { return prioridadeDinamica; }
}