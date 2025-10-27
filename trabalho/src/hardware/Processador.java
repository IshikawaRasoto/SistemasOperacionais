package hardware;

import modelo.TCB;
import simulador.Relogio;

import java.util.ArrayList;

public class Processador {
    ArrayList<CPU> nucleos;
    Relogio relogio;

    public Processador(int numeroDeNucleos, int quantum) {
        this.nucleos = new ArrayList<>();
        for (int i = 0; i < numeroDeNucleos; i++) {
            nucleos.add(new CPU(quantum));
        }
        this.relogio = Relogio.getInstancia();
    }

    public boolean haNucleoOcioso() {
        for (CPU cpu : nucleos) {
            if (cpu.getEstado() == EstadoCPU.OCIOSA) {
                return true;
            }
        }
        return false;
    }

    public int executarTarefa(TCB tcb) {
        for (CPU cpu : nucleos) {
            if (cpu.getEstado() == EstadoCPU.OCIOSA) {
                cpu.novoProcesso(tcb, relogio.getTickAtual());
                return nucleos.indexOf(cpu);
            }
        }
        return -1; // Nenhum núcleo disponível
    }

    public ArrayList<CPU> getNucleos() {
        return nucleos;
    }

    public Relogio getRelogio() {
        return relogio;
    }
}