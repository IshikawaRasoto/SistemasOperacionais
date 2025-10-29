package hardware;

import modelo.TCB;
import simulador.Relogio;

import java.util.ArrayList;

public class Processador {
    public ArrayList<CPU> nucleos;

    public Processador(int numeroDeNucleos) {
        this.nucleos = new ArrayList<>();
        for (int i = 0; i < numeroDeNucleos; i++) {
            nucleos.add(new CPU());
        }
    }

    public boolean haNucleoOcioso() {
        for (CPU cpu : nucleos) {
            if (cpu.getEstado() == EstadoCPU.OCIOSA) {
                return true;
            }
        }
        return false;
    }

    public void executarProcessos(){
        for (CPU cpu : nucleos) {
            cpu.executaTarefa();
        }
    }

    public ArrayList<CPU> getNucleos() {
        return nucleos;
    }
}