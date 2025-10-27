package hardware;

import modelo.TCB;

import sistemaoperacional.Interrupcoes;

public class CPU {
    EstadoCPU estado;
    TCB tarefaAtual;
    int quantum;
    int inicioProcessoAtual;
    int ticksExecutadosNoProcessoAtual;

    public CPU(int quantum) {
        this.quantum = quantum;
        this.tarefaAtual = null;
        this.inicioProcessoAtual = -1;
        this.estado = EstadoCPU.OCIOSA;
        this.ticksExecutadosNoProcessoAtual = 0;
    }

    public TCB getTarefaAtual() {
        return tarefaAtual;
    }

    public void novoProcesso(TCB tcb, int tickAtual) {
        this.tarefaAtual = tcb;
        this.inicioProcessoAtual = tickAtual;
        estado = EstadoCPU.OCUPADA;
    }

    public void finalizarProcesso() {
        this.tarefaAtual = null;
        this.inicioProcessoAtual = -1;
        this.ticksExecutadosNoProcessoAtual = 0;
        estado = EstadoCPU.OCIOSA;
    }

    public Interrupcoes executarProcesso() {
        if (tarefaAtual == null) return Interrupcoes.NO_INTERRUPT;

        tarefaAtual.decrementarRestante();
        if (tarefaAtual.getEstadoTarefa() == modelo.EstadoTarefa.FINALIZADA) {
            finalizarProcesso();
            return Interrupcoes.FINALIZACAO_DE_TAREFA;
        }

        ticksExecutadosNoProcessoAtual++;
        if(ticksExecutadosNoProcessoAtual >= quantum) {
            ticksExecutadosNoProcessoAtual = 0;
            return Interrupcoes.QUANTUM;
        }

        return Interrupcoes.NO_INTERRUPT;
    }

    // getters
    public EstadoCPU getEstado() {
        return estado;
    }
}