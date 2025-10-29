package sistemaoperacional.nucleo;

public class Interrupcoes {
    public boolean chegada_de_tarefa;
    public boolean quantum_esgotado;

    public Interrupcoes(boolean chegada_de_tarefa, boolean quantum_esgotado) {
        this.chegada_de_tarefa = chegada_de_tarefa;
        this.quantum_esgotado = quantum_esgotado;
    }

    public Interrupcoes(){
        this(false, false);
    }

    public void resetarFlags() {
        this.chegada_de_tarefa = false;
        this.quantum_esgotado = false;
    }
}