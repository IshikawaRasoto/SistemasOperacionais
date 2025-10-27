package sistemaoperacional;

import modelo.Tarefa;
import modelo.TCB;
import modelo.EstadoTarefa;

import java.util.ArrayList;

public class SistemaOperacional {
    private ArrayList<Tarefa> tarefasParaCriar;
    private ArrayList<TCB> listaTCBs;
    private ArrayList<TCB> listaProntos;


    public SistemaOperacional() {
        this.tarefasParaCriar = new ArrayList<>();
        this.listaProntos = new ArrayList<>();
    }

    public void setTarefasParaCriar(ArrayList<Tarefa> tarefasParaCriar) {
        this.tarefasParaCriar = tarefasParaCriar;
    }

    public boolean terminouTodasTarefas() {
        if (tarefasParaCriar.size() == 0) {
            for(TCB tcb : listaTCBs) {
                if (tcb.getEstadoTarefa() != EstadoTarefa.FINALIZADA) {
                    return false;
                }
            }
        }
        return true;
    }

    public void exec() {


    }
}

