package sistemaoperacional.nucleo;

import modelo.TCB;
import modelo.EstadoTarefa;
import modelo.Tarefa;

public class Escalonador {
    public TCB escolherTarefaParaExecucao(AlgoritmosEscalonamento escalonamento, TCB[] listaDeTCBs) {
        switch(escalonamento) {


        }

    }

    private TCB escalonarFIFO(TCB[] listaDeTCBs) {
        TCB tarefaEscolhida = null;
        for (TCB tcb : listaDeTCBs) {
            if (tcb.getEstadoTarefa() == EstadoTarefa.PRONTA) {
                if (tarefaEscolhida == null || tcb.getTarefa().getTickChegada() < tarefaEscolhida.getTarefa().getTickChegada()) {
                    tarefaEscolhida = tcb;
                }
            }
        }
        return tarefaEscolhida;
    }

}