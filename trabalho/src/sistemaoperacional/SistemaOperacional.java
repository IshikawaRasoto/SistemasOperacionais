package sistemaoperacional;

import modelo.TCB;

import java.util.ArrayList;

public class SistemaOperacional {
    ArrayList<TCB> listaDeTarefas;

    public SistemaOperacional() {
        this.listaDeTarefas = new ArrayList<>();
    }

    public void adicionarTarefa(TCB tcb) {
        listaDeTarefas.add(tcb);
    }


}
