package sistemaoperacional.nucleo;

import simulador.Relogio;
import modelo.TCB;
import java.util.Queue;
import java.util.ArrayList;

public class Escalonador {

    private AlgoritmosEscalonamento algoritmoEscolhido;
    private int quantum;

    public Escalonador(String nomeEscalonador, int quantum) {
        defineAlgoritmoEscalonador(nomeEscalonador);
        this.quantum = quantum;
    }

    public boolean deveTrocarContexto(TCB tarefaAtual, Queue<TCB> filaProntos, CausaEscalonamento causa) {

        // Regra Universal: Se a tarefa acabou ou a CPU está vazia, PRECISA trocar (ou tentar pegar algo)
        if (causa == CausaEscalonamento.TAREFA_FINALIZADA || causa == CausaEscalonamento.CPU_OCIOSA) {
            return true;
        }

        // Se a CPU está ocupada e a fila está vazia, geralmente não há por que trocar
        // (Exceto se o algoritmo quiser punir a tarefa atual, mas vamos simplificar)
        if (filaProntos.isEmpty()) {
            return false;
        }

        switch (algoritmoEscolhido) {
            case FIFO:
                // FIFO é não-preemptivo por natureza (exceto se a tarefa acabar, tratado acima)
                // Ele IGNORA Quantum e Novas Tarefas.
                if (causa == CausaEscalonamento.QUANTUM_EXPIRADO) {
                    return true;
                }
                return false;

            case SRTF:
                // SRTF é preemptivo por NOVA TAREFA.
                // Se chegou tarefa nova, verifica se ela é melhor que a atual.
                if (causa == CausaEscalonamento.NOVA_TAREFA) {
                    return verificarSeExisteTarefaMaisCurta(tarefaAtual, filaProntos);
                }
                // Geralmente SRTF puro ignora Quantum, mas em sistemas híbridos poderia usar.
                // Vamos assumir que SRTF puro ignora Quantum aqui.
                return false;

            case PRIORIDADE_PREEMPTIVO:
                // Troca se chegou alguém mais importante
                if (causa == CausaEscalonamento.NOVA_TAREFA) {
                    return verificarSeExisteMaiorPrioridade(tarefaAtual, filaProntos);
                }
                // Round Robin de Prioridade? Se quiser que ele respeite quantum:
                if (causa == CausaEscalonamento.QUANTUM_EXPIRADO) {
                    return true;
                }
                return false;

            default: // ROUND_ROBIN (O 'FIFO' deste projeto rodando com tempo compartilhado)
                // O critério principal do RR é o Quantum.
                if (causa == CausaEscalonamento.QUANTUM_EXPIRADO) {
                    return true;
                }
                // RR geralmente ignora chegada de nova tarefa (só põe na fila)
                return false;
        }
    }

    private boolean verificarSeExisteTarefaMaisCurta(TCB atual, Queue<TCB> fila) {
        for (TCB t : fila) {
            if (t.getRestante() < atual.getRestante()) return true;
        }
        return false;
    }

    private boolean verificarSeExisteMaiorPrioridade(TCB atual, Queue<TCB> fila) {
        for (TCB t : fila) {
            if (t.getTarefa().getPrioridade() > atual.getTarefa().getPrioridade()) return true;
        }
        return false;
    }

    private void defineAlgoritmoEscalonador(String nomeEscalonador){
        // Mapeia FIFO como Round Robin neste contexto de tempo compartilhado
        if (nomeEscalonador.equals("FIFO") || nomeEscalonador.equals("ROUND_ROBIN")){
            algoritmoEscolhido = AlgoritmosEscalonamento.FIFO;
        }
        else if (nomeEscalonador.equals("SRTF")){
            algoritmoEscolhido = AlgoritmosEscalonamento.SRTF;
        }
        else if (nomeEscalonador.equals("PRIORIDADE_PREEMPTIVO") || nomeEscalonador.equals("PRIOP")){
            algoritmoEscolhido = AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO;
        }
        else{
            System.out.println("Algoritmo de escalonamento desconhecido. Usando FIFO/RR por padrao.");
            algoritmoEscolhido = AlgoritmosEscalonamento.FIFO;
        }
    }

    // Assinatura simplificada: Recebe a fila e retorna quem deve sair dela para a CPU
    public TCB escolherProximaTarefa(Queue<TCB> readyQueue) {
        if (readyQueue.isEmpty()) {
            return null;
        }

        switch(algoritmoEscolhido) {
            case FIFO:
                return escalonarRoundRobin(readyQueue);
            case SRTF:
                return escalonarSRTF(readyQueue);
            case PRIORIDADE_PREEMPTIVO:
                return escalonarPrioridade(readyQueue);
            default:
                return null;
        }
    }

    // Lógica do Round Robin: Pega o primeiro da fila.
    // A rotação acontece porque o SO coloca a tarefa anterior no final da fila antes de chamar este metodo.
    private TCB escalonarRoundRobin(Queue<TCB> readyQueue) {
        return readyQueue.poll();
    }

    private TCB escalonarSRTF(Queue<TCB> readyQueue) {
        TCB melhorTarefa = null;

        // Encontra a tarefa com menor tempo restante
        for(TCB tcb : readyQueue) {
            if(melhorTarefa == null || tcb.getRestante() < melhorTarefa.getRestante()) {
                melhorTarefa = tcb;
            }
        }

        // Remove da fila para entregar ao processador
        if (melhorTarefa != null) {
            readyQueue.remove(melhorTarefa);
        }
        return melhorTarefa;
    }

    private TCB escalonarPrioridade(Queue<TCB> readyQueue) {
        TCB melhorTarefa = null;

        // Encontra a tarefa com maior prioridade numérica
        for(TCB tcb : readyQueue) {
            if(melhorTarefa == null || tcb.getTarefa().getPrioridade() > melhorTarefa.getTarefa().getPrioridade()) {
                melhorTarefa = tcb;
            }
        }

        // Remove da fila para entregar ao processador
        if (melhorTarefa != null) {
            readyQueue.remove(melhorTarefa);
        }
        return melhorTarefa;
    }
}