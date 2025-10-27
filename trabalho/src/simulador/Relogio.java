package simulador;

public class Relogio {

    private static final Relogio instancia = new Relogio();

    private int tickAtual;

    private Relogio(){
        this.tickAtual = 0;
    }

    public static Relogio getInstancia(){
        return instancia;
    }

    public int getTickAtual() {
        return tickAtual;
    }

    public void tick() {
        tickAtual++;
    }

    public void resetar(){
        tickAtual = 0;
    }
}