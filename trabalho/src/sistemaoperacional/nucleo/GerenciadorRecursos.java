package sistemaoperacional.nucleo;

import modelo.TCB;
import java.util.*;

public class GerenciadorRecursos {

    // Mapa: ID do Mutex -> TCB que detém o recurso (Dono)
    private final Map<Integer, TCB> mutexDonos;
    // Mapa: ID do Mutex -> Fila de TCBs esperando pelo recurso
    private final Map<Integer, Queue<TCB>> mutexFilasEspera;

    public GerenciadorRecursos() {
        this.mutexDonos = new HashMap<>();
        this.mutexFilasEspera = new HashMap<>();
    }

    // Tenta adquirir o Mutex. Retorna TRUE se conseguiu, FALSE se foi bloqueado.
    public boolean solicitarMutex(int idMutex, TCB solicitante) {
        // Se ninguém tem o mutex, o solicitante ganha
        if (!mutexDonos.containsKey(idMutex)) {
            mutexDonos.put(idMutex, solicitante);
            return true; // Conseguiu livremente
        } else {
            // Se já tem dono, entra na fila
            mutexFilasEspera.putIfAbsent(idMutex, new LinkedList<>());
            mutexFilasEspera.get(idMutex).add(solicitante);
            return false; // Foi bloqueado
        }
    }

    // Libera o Mutex e retorna o próximo TCB da fila (se houver), para ser acordado
    public TCB liberarMutex(int idMutex, TCB solicitante) {
        TCB donoAtual = mutexDonos.get(idMutex);

        // Segurança: só o dono pode liberar
        if (donoAtual != solicitante) {
            System.err.println("ERRO: Tarefa " + solicitante.getTarefa().getId() +
                    " tentou liberar Mutex " + idMutex + " sem ser o dono.");
            return null;
        }

        mutexDonos.remove(idMutex);

        // Verifica se tem alguém esperando
        if (mutexFilasEspera.containsKey(idMutex)) {
            Queue<TCB> fila = mutexFilasEspera.get(idMutex);
            if (!fila.isEmpty()) {
                TCB proximo = fila.poll();
                // O próximo da fila assume o dono imediatamente
                mutexDonos.put(idMutex, proximo);
                return proximo; // Retorna quem deve ser acordado
            }
        }
        return null; // Ninguém para acordar
    }
}