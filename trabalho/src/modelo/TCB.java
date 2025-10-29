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

    private final Relogio relogio = Relogio.getInstancia();

    public TCB(Tarefa tarefa) {
        this.tarefa = tarefa;
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.restante = tarefa.getDuracaoTotal();
        this.quantumUsado = 0;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void entrarFilaPronta(){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void entrarNoProcessador(){
        this.estadoTarefa = EstadoTarefa.EXECUTANDO;
        this.tickEntradaProcessador = relogio.getTickAtual();
        this.inicioFatiaAtual = relogio.getTickAtual();
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
        this.restante--;
        this.quantumUsado++;
        if (this.restante == 0){
            this.estadoTarefa = EstadoTarefa.FINALIZADA;
            this.tickTermino = relogio.getTickAtual();
        }
    }

    // Setters
    public void setEstadoTarefa(EstadoTarefa estadoTarefa) {
        this.estadoTarefa = estadoTarefa;
    }

    public void setRestante(int restante) {
        this.restante = restante;
    }

    public void setTickTermino(int tickTermino) {
        this.tickTermino = tickTermino;
    }

    // Getters
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
}