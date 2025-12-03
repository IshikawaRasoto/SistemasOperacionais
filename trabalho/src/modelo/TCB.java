package modelo;

import simulador.Relogio;

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

    // Prioridade dinâmica para o algoritmo de envelhecimento
    private int prioridadeDinamica;

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

    // Método usado pelo SO para aplicar envelhecimento a quem está na fila
    public void envelhecer(int alpha) {
        this.prioridadeDinamica += alpha;
    }

    // --- Getters e Setters ---

    public void setEstadoTarefa(EstadoTarefa estadoTarefa) {
        this.estadoTarefa = estadoTarefa;
    }

    public void setRestante(int restante) {
        this.restante = restante;
    }

    public void setTickTermino(int tickTermino) {
        this.tickTermino = tickTermino;
    }

    public Tarefa getTarefa() {
        return tarefa;
    }

    public EstadoTarefa getEstadoTarefa() {
        return estadoTarefa;
    }

    public int getRestante() {
        return restante;
    }

    public int getQuantumUsado() {
        return quantumUsado;
    }

    public int getTickEntradaFilaPronta() {
        return tickEntradaFilaPronta;
    }

    public int getTickEntradaProcessador() {
        return tickEntradaProcessador;
    }

    public int getTickTermino() {
        return tickTermino;
    }

    public int getInicioFatiaAtual() {
        return inicioFatiaAtual;
    }

    public int getEsperaAcumulada() {
        return esperaAcumulada;
    }

    public int getPrioridadeDinamica() {
        return prioridadeDinamica;
    }
}