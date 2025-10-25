package modelo;
import modelo.Tarefa;
import modelo.EstadoTarefa;

public class TCB {

    private Tarefa tarefa;
    private EstadoTarefa estadoTarefa;
    private int restante = 0;
    private int tickEntradaFilaPronta = 0;
    private int tickPrimeiraResposta = -1;
    private int tickTermino = -1;
    private int inicioFatiaAtual = -1;
    private int esperaAcumulada = 0;

    public TCB(Tarefa tarefa) {
        this.tarefa = tarefa;
        this.estadoTarefa = EstadoTarefa.NOVA;
        this.restante = tarefa.getDuracaoTotal();
    }

    public void entrarFilaPronta(int tickAtual){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.tickEntradaFilaPronta = tickAtual;
    }

    public void entrarNoProcessador(int tickAtual){
        this.estadoTarefa = EstadoTarefa.EXECUTANDO;
        this.tickPrimeiraResposta = tickAtual;
        this.inicioFatiaAtual = tickAtual;
    }

    public void sairDoProcessador(int tickAtual){
        this.estadoTarefa = EstadoTarefa.PRONTA;
        this.inicioFatiaAtual = tickAtual;
        this.esperaAcumulada = tickAtual;
        this.tickEntradaFilaPronta = tickAtual;
    }

    public void interromperTarefa(int tickAtual){
        this.estadoTarefa = EstadoTarefa.BLOQUEADA;
        this.inicioFatiaAtual = tickAtual;
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