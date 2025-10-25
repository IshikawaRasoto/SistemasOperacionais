package hardware;

import modelo.TCB;

public class CPU {
    EstadoCPU estado;
    TCB tarefaAtual;
    int quantum;
    int inicioProcessoAtual;

    public CPU(int quantum) {
        this.quantum = quantum;
        this.tarefaAtual = null;
        this.inicioProcessoAtual = -1;
        this.estado = EstadoCPU.OCIOSA;
    }

    public TCB getTarefaAtual() {
        return tarefaAtual;
    }

    public void novoProcesso(TCB tcb, int tickAtual) {
        this.tarefaAtual = tcb;
        this.inicioProcessoAtual = tickAtual;
        estado = EstadoCPU.OCIOSA;
    }

    public void finalizarProcesso() {
        this.tarefaAtual = null;
        this.inicioProcessoAtual = -1;
        estado = EstadoCPU.OCIOSA;
    }

    // getters
    public EstadoCPU getEstado() {
        return estado;
    }
}
