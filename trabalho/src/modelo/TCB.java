package modelo;

import modelo.EstadoTarefa;


import simulador.Relogio;

public class TCB {
    Tarefa tarefa;
    private EstadoTarefa estadoTarefa;
    private int restante = 0;
    private int tickEntradaFilaPronta = 0;
    private int tickPrimeiraResposta = -1;
    private int tickTermino = -1;
    private int inicioFatiaAtual = -1;
    private int esperaAcumulada = 0;

    private final Relogio relogio = Relogio.getInstancia();

    public TCB(Tarefa tarefa) {
        this.tarefa = tarefa;
        this.estadoTarefa = EstadoTarefa.NOVA;
        this.restante = tarefa.getDuracaoTotal();
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void entrarFilaPronta(){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void entrarNoProcessador(){
        this.estadoTarefa = EstadoTarefa.EXECUTANDO;
        this.tickPrimeiraResposta = relogio.getTickAtual();
        this.inicioFatiaAtual = relogio.getTickAtual();
    }

    public void sairDoProcessador(){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.inicioFatiaAtual = relogio.getTickAtual();
        this.esperaAcumulada = relogio.getTickAtual();
        this.tickEntradaFilaPronta = relogio.getTickAtual();
    }

    public void interromperTarefa(){
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        this.inicioFatiaAtual = relogio.getTickAtual();
    }

    public void decrementarRestante(){
        this.restante--;
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

    public int getTickEntradaFilaPronta() {
        return tickEntradaFilaPronta;
    }

    public int getTickPrimeiraResposta() {
        return tickPrimeiraResposta;
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