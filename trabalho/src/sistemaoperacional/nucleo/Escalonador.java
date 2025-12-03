package sistemaoperacional.nucleo;

import modelo.TCB;
import java.util.Queue;

public class Escalonador {

    private AlgoritmosEscalonamento algoritmoEscolhido;
    private int quantum;

    public Escalonador(String nomeEscalonador, int quantum) {
        defineAlgoritmoEscalonador(nomeEscalonador);
        this.quantum = quantum;
    }

    public AlgoritmosEscalonamento getAlgoritmoEscolhido() {
        return algoritmoEscolhido;
    }

    public boolean deveTrocarContexto(TCB tarefaAtual, Queue<TCB> filaProntos, CausaEscalonamento causa) {
        if (causa == CausaEscalonamento.TAREFA_FINALIZADA || causa == CausaEscalonamento.CPU_OCIOSA) {
            return true;
        }

        if (filaProntos.isEmpty()) {
            return false;
        }

        switch (algoritmoEscolhido) {
            case FIFO:
                if (causa == CausaEscalonamento.QUANTUM_EXPIRADO) return true;
                return false;

            case SRTF:
                if (causa == CausaEscalonamento.NOVA_TAREFA) {
                    return verificarSeExisteTarefaMaisCurta(tarefaAtual, filaProntos);
                }
                return false;

            case PRIORIDADE_PREEMPTIVO:
                if (causa == CausaEscalonamento.NOVA_TAREFA) {
                    return verificarSeExisteMaiorPrioridade(tarefaAtual, filaProntos, false);
                }
                return false;

            case PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO:
                // Verifica se deve trocar em 3 casos:
                // 1. Nova Tarefa (Padrão)
                // 2. Quantum Expirado (Padrão/Segurança)
                // 3. Envelhecimento (Tick a Tick): Verifica se alguém na fila ficou mais prioritário que a atual
                if (causa == CausaEscalonamento.NOVA_TAREFA ||
                        causa == CausaEscalonamento.QUANTUM_EXPIRADO ||
                        causa == CausaEscalonamento.ENVELHECIMENTO) {
                    return verificarSeExisteMaiorPrioridade(tarefaAtual, filaProntos, true);
                }
                return false;

            default:
                if (causa == CausaEscalonamento.QUANTUM_EXPIRADO) return true;
                return false;
        }
    }

    private boolean verificarSeExisteTarefaMaisCurta(TCB atual, Queue<TCB> fila) {
        for (TCB t : fila) {
            if (t.getRestante() < atual.getRestante()) return true;
        }
        return false;
    }

    private boolean verificarSeExisteMaiorPrioridade(TCB atual, Queue<TCB> fila, boolean usarDinamica) {
        for (TCB t : fila) {
            int pFila = usarDinamica ? t.getPrioridadeDinamica() : t.getTarefa().getPrioridade();
            int pAtual = usarDinamica ? atual.getPrioridadeDinamica() : atual.getTarefa().getPrioridade();

            // Se a prioridade de alguém na fila for ESTRITAMENTE maior, preempta.
            if (pFila > pAtual) return true;
        }
        return false;
    }

    private void defineAlgoritmoEscalonador(String nomeEscalonador){
        String nome = nomeEscalonador.toUpperCase().trim();

        if (nome.equals("FIFO") || nome.equals("ROUND_ROBIN")){
            algoritmoEscolhido = AlgoritmosEscalonamento.FIFO;
        }
        else if (nome.equals("SRTF")){
            algoritmoEscolhido = AlgoritmosEscalonamento.SRTF;
        }
        else if (nome.equals("PRIORIDADE_PREEMPTIVO") || nome.equals("PRIOP")){
            algoritmoEscolhido = AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO;
        }
        else if (nome.equals("PRIOPENV") || nome.equals("PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO")) {
            algoritmoEscolhido = AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO;
        }
        else{
            System.out.println("Algoritmo desconhecido ("+nome+"). Usando FIFO.");
            algoritmoEscolhido = AlgoritmosEscalonamento.FIFO;
        }
    }

    public TCB escolherProximaTarefa(Queue<TCB> readyQueue) {
        if (readyQueue.isEmpty()) return null;

        switch(algoritmoEscolhido) {
            case FIFO:
                return readyQueue.poll();
            case SRTF:
                return escalonarSRTF(readyQueue);
            case PRIORIDADE_PREEMPTIVO:
                return escalonarPrioridade(readyQueue, false);
            case PRIORIDADE_PREEMPTIVO_ENVELHECIMENTO:
                return escalonarPrioridade(readyQueue, true);
            default:
                return null;
        }
    }

    private TCB escalonarSRTF(Queue<TCB> readyQueue) {
        TCB melhor = null;
        for(TCB t : readyQueue) {
            if(melhor == null || t.getRestante() < melhor.getRestante()) melhor = t;
        }
        if (melhor != null) readyQueue.remove(melhor);
        return melhor;
    }

    private TCB escalonarPrioridade(Queue<TCB> readyQueue, boolean usarDinamica) {
        TCB melhor = null;
        for(TCB t : readyQueue) {
            if(melhor == null) {
                melhor = t;
            } else {
                int pMelhor = usarDinamica ? melhor.getPrioridadeDinamica() : melhor.getTarefa().getPrioridade();
                int pAtual = usarDinamica ? t.getPrioridadeDinamica() : t.getTarefa().getPrioridade();

                if (pAtual > pMelhor) melhor = t;
            }
        }
        if (melhor != null) readyQueue.remove(melhor);
        return melhor;
    }
}