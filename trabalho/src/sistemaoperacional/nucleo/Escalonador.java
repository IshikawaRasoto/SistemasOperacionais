/*

        Escalonador.java - responsavel por escolher qual tarefa sera executada com base no algoritmo de escalonamento selecionado.
        Autor: Rafael Eijy Ishikawa Rasoto

        Instrucoes:
            - Para criar um novo algoritmo de escalonamento, basta seguir os passos abaixo:
                1. Criar o metodo que retorna um tipo TCB (tarefa a ser executada) com o nome do seu algoritmo.
                2. Chamar o seu metodo no switch-case do metodo escolherTarefaParaExecucao.
                3. Adicionar o nome do seu algoritmo na enum AlgoritmosEscalonamento.
                4. Inserir seu algoritmo e sua respectiva string de configuracao no metodo defineAlgoritmoEscalonador.

            - Atualmente, os algoritmos implementados sao:
                - FIFO (First In, First Out)
                - TODO SRTF (Shortest Remaining Time First)
                - TODO PRIORIDADE_PREEMPTIVO (Prioridade Preemptivo)

            - Os parametros estao todos contido na tarefa atual, fila de prontos, alem de voce poder visualizar
              o quantum do sistema via variavel interna quantum.

       IMPORTANTE:
            - Para simplificar o funcionamento do SO permitindo com que a implementacao fosse modificando o minimo possivel
              do SO, o metodo de escalonamento eh chamado todos os ticks para todos os nucles do processador. (Defesa A possui
              um unico nucleo). Dessa forma, se o seu algoritmo nao for preemptivo, basta retornar a tarefa atual. O restante
              retorne a tarefa desejada que a troca do contexto da CPU sera feita pelo SO.

 */

package sistemaoperacional.nucleo;

import simulador.Relogio;

import modelo.TCB;
import modelo.EstadoTarefa;

import java.util.Queue;
import java.util.LinkedList;


public class Escalonador {

    private Relogio relogio;
    private AlgoritmosEscalonamento algoritmoEscolhido;
    private int quantum;

    public Escalonador(String nomeEscalonador, int quantum) {
        defineAlgoritmoEscalonador(nomeEscalonador);
        this.quantum = quantum;
        relogio = Relogio.getInstancia();
    }

    private void defineAlgoritmoEscalonador(String nomeEscalonador){
        if (nomeEscalonador.equals("FIFO")){
            algoritmoEscolhido = AlgoritmosEscalonamento.FIFO;
        }
        else if (nomeEscalonador.equals("SRTF")){
            algoritmoEscolhido = AlgoritmosEscalonamento.SRTF;
        }
        else if (nomeEscalonador.equals("PRIORIDADE_PREEMPTIVO")){
            algoritmoEscolhido = AlgoritmosEscalonamento.PRIORIDADE_PREEMPTIVO;
        }
    }

    public TCB escolherTarefaParaExecucao(Queue<TCB> readyQueue, TCB tarefaAtual) {
        switch(algoritmoEscolhido) {
            case FIFO:
                return escalonarFIFO(readyQueue, tarefaAtual);
            case SRTF:
                return escalonarSRTF(readyQueue, tarefaAtual);
            case PRIORIDADE_PREEMPTIVO:
                return escalonarPrioridadePreemptivo(readyQueue, tarefaAtual);
            default:
                return null;
        }

    }

    private TCB escalonarFIFO(Queue<TCB> readyQueue, TCB tarefaAtual) {
        TCB tarefaEscolhida = null;

        if(tarefaAtual == null){
            System.out.println("Tarefa atual nula no escalonador FIFO");
            if (!readyQueue.isEmpty()) {
                tarefaEscolhida = readyQueue.poll();
            }
        }else{
            if(tarefaAtual.getEstadoTarefa() == EstadoTarefa.EXECUTANDO){
                System.out.println("Tarefa atual mantida no escalonador FIFO");
                tarefaEscolhida = tarefaAtual;
            } else {
                if (!readyQueue.isEmpty()) {
                    System.out.println("Proxima tarefa");
                    tarefaEscolhida = readyQueue.poll();
                }
            }
        }

        return tarefaEscolhida;
    }

    private TCB escalonarSRTF(Queue<TCB> readyQueue, TCB tarefaAtual) {
        TCB tarefaEscolhida = null;

        TCB menorTarefaPronta = null;
        if(!readyQueue.isEmpty()){
            for(TCB tcb : readyQueue) {
                if(menorTarefaPronta == null || tcb.getRestante() < menorTarefaPronta.getRestante()) {
                    menorTarefaPronta = tcb;
                }
            }
        }

        if(tarefaAtual == null){
            tarefaEscolhida = menorTarefaPronta;
        }else{
            if(menorTarefaPronta != null){
                if(tarefaAtual.getEstadoTarefa() == EstadoTarefa.FINALIZADA){
                    tarefaEscolhida = menorTarefaPronta;
                }
                else if(menorTarefaPronta.getRestante() < tarefaAtual.getRestante()){
                    tarefaEscolhida = menorTarefaPronta;
                } else {
                    tarefaEscolhida = tarefaAtual;
                }
            } else {
                tarefaEscolhida = tarefaAtual;
            }
        }

        return tarefaEscolhida;
    }

    private TCB escalonarPrioridadePreemptivo(Queue<TCB> readyQueue, TCB tarefaAtual) {
        TCB tarefaEscolhida = null;

        TCB tarefaMaiorPrioridadePronta = null;
        if(!readyQueue.isEmpty()){
            for(TCB tcb : readyQueue) {
                if(tarefaMaiorPrioridadePronta == null || tcb.getTarefa().getPrioridade() > tarefaMaiorPrioridadePronta.getTarefa().getPrioridade()) {
                    tarefaMaiorPrioridadePronta = tcb;
                }
            }
        }

        if(tarefaAtual == null){
            tarefaEscolhida = tarefaMaiorPrioridadePronta;
        }else{
            if(tarefaMaiorPrioridadePronta != null){
                if(tarefaAtual.getEstadoTarefa() == EstadoTarefa.FINALIZADA){
                    tarefaEscolhida = tarefaMaiorPrioridadePronta;
                }
                else if(tarefaMaiorPrioridadePronta.getTarefa().getPrioridade() > tarefaAtual.getTarefa().getPrioridade()){
                    tarefaEscolhida = tarefaMaiorPrioridadePronta;
                } else {
                    tarefaEscolhida = tarefaAtual;
                }
            } else {
                tarefaEscolhida = tarefaAtual;
            }
        }

        return tarefaEscolhida;
    }

}