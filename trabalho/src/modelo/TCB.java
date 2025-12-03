package modelo;

import simulador.Relogio;

import java.util.List;

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
    private int tempoRestanteBloqueioIO = 0; // Contador regressivo para I/O

    private final Relogio relogio = Relogio.getInstancia();

    public TCB(Tarefa tarefa) {
        this.tarefa = tarefa;
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.restante = tarefa.getDuracaoTotal();
        this.quantumUsado = 0;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
        // Inicializa com a prioridade estática (original)
        this.prioridadeDinamica = tarefa.getPrioridade();
    }

    // Calcula quanto tempo "útil" a tarefa já rodou
    public int getTempoExecutado() {
        return tarefa.getDuracaoTotal() - restante;
    }

    // Verifica se existe algum evento agendado EXATAMENTE para este instante de execução
    public Evento verificarEventoAtual() {
        int tempoAtualExecucao = getTempoExecutado();
        List<Evento> eventos = tarefa.getEventos();

        if (eventos == null || eventos.isEmpty()) return null;

        // DEBUG: Mostra que o TCB está checando eventos
        // System.out.println("DEBUG CHECK: Tarefa " + tarefa.getId() + " TempoExec: " + tempoAtualExecucao);

        for (Evento e : eventos) {
            if (e.getTempoOcorrencia() == tempoAtualExecucao) {
                System.out.println("!!! MATCH !!! Evento disparado na tarefa " + tarefa.getId() + " no tempo " + tempoAtualExecucao);
                return e;
            }
        }
        return null;
    }

    public void bloquearPorIO(int duracao) {
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        this.tempoRestanteBloqueioIO = duracao;
    }

    public void bloquearPorMutex() {
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
    }

    public void desbloquear() {
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.tickEntradaFilaPronta = relogio.getTickAtual(); // Reseta espera para evitar starvation imediato
        this.tempoRestanteBloqueioIO = 0;
    }

    public void decrementarTempoBloqueio() {
        if (tempoRestanteBloqueioIO > 0) {
            tempoRestanteBloqueioIO--;
        }
    }

    public boolean acabouTempoBloqueio() {
        return tempoRestanteBloqueioIO <= 0;
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