package modelo;

public class Evento {
    private TipoEvento tipo;
    private int tempoOcorrencia; // Tempo relativo ao início da tarefa
    private int duracao;         // Usado apenas para IO (yy)
    private int idRecurso;       // Usado apenas para Mutex (xx)

    // Construtor para IO
    public Evento(TipoEvento tipo, int tempoOcorrencia, int duracao) {
        this.tipo = tipo;
        this.tempoOcorrencia = tempoOcorrencia;
        this.duracao = duracao;
        this.idRecurso = -1;
    }

    // Construtor para Mutex
    public Evento(TipoEvento tipo, int tempoOcorrencia, int idRecurso, boolean isMutex) {
        this.tipo = tipo;
        this.tempoOcorrencia = tempoOcorrencia;
        this.idRecurso = idRecurso;
        this.duracao = 0;
    }

    @Override
    public String toString() {
        if (tipo == TipoEvento.IO) return "IO@" + tempoOcorrencia + "(dur:" + duracao + ")";
        return tipo + " id:" + idRecurso + "@" + tempoOcorrencia;
    }

    // Getters
    public TipoEvento getTipo() { return tipo; }
    public int getTempoOcorrencia() { return tempoOcorrencia; }
    public int getDuracao() { return duracao; }
    public int getIdRecurso() { return idRecurso; }
}