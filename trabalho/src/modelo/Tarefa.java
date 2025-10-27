package modelo;

import java.util.regex.Pattern;

/*
    Dados fixos de uma tarefa:
    id; cor; ingresso; duracao; prioridade; lista_eventos
 */

public class Tarefa {
    private String id;
    private int cor;
    private int instanteChegada;
    private int duracaoTotal;
    private int prioridade;

    public Tarefa (String id, int cor, int instanteChegada, int duracaoTotal, int prioridade){
        if (id == null || id.isBlank()){
            throw new IllegalArgumentException("id Tarefa não pode ser vazio");
        }
        this.id = id.trim();

        if(instanteChegada < 0){
            throw new IllegalArgumentException("Instante de chegada negativo");
        }
        this.instanteChegada = instanteChegada;

        if(duracaoTotal <= 0 ){
            throw new IllegalArgumentException("Duracao total deve ser maior que zero");
        }
        this.duracaoTotal = duracaoTotal;

        if(prioridade < 0){
            throw new IllegalArgumentException("Prioridade deve ser positiva");
        }
        this.prioridade = prioridade;
    }

    public String resumo(){
        return "%s [chegada=%d, duracao=%d, prioridade=%d]".formatted(id, instanteChegada, duracaoTotal, prioridade);
    }

    public String getId(){
        return id;
    }

    public int getCor(){
        return cor;
    }

    public int getInstanteChegada(){
        return instanteChegada;
    }

    public int getDuracaoTotal(){
        return duracaoTotal;
    }

    public int getPrioridade(){
        return prioridade;
    }
}