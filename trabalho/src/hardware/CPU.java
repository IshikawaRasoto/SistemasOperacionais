package hardware;

import modelo.TCB;

import sistemaoperacional.nucleo.Interrupcoes;

public class CPU {
    EstadoCPU estado;
    TCB tarefaAtual;

    public CPU() {
        this.tarefaAtual = null;
        this.estado = EstadoCPU.OCIOSA;
    }

    void executaTarefa(){
        if(tarefaAtual != null){
            tarefaAtual.executarTick();
        }
    }

    public TCB getTarefaAtual() {
        return tarefaAtual;
    }

    public void novoProcesso(TCB tcb) {
        estado = EstadoCPU.OCUPADA;
        this.tarefaAtual = tcb;
    }

    public void finalizarProcesso() {
        this.tarefaAtual = null;
        estado = EstadoCPU.OCIOSA;
    }

    // getters
    public EstadoCPU getEstado() {
        return estado;
    }
}