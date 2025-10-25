package hardware;

public class Relogio {
    private int tickAtual;

    public Relogio() {
        this.tickAtual = 0;
    }

    public int getTickAtual() {
        return tickAtual;
    }

    public void avancarTick() {
        tickAtual++;
    }
}