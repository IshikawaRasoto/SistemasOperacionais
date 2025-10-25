import modelo.Tarefa;

public class Main {
    public static void main(String[] args) {
        Tarefa t1 = new Tarefa("T1", "#4F81BD", 0, 7, 2);
        System.out.println(t1.resumo());
    }
}