package modelo;

import java.util.Objects;
import java.util.regex.Pattern;

/*
    Dados fixos de uma tarefa:
    id; cor; ingresso; duracao; prioridade; lista_eventos
 */

public class Tarefa {
    private String id;
    private String corHex;
    private int instanteChegada;
    private int duracaoTotal;
    private int prioridade;

    private static final Pattern HEX_RGB = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public Tarefa (String id, String corHex, int instanteChegada, int duracaoTotal, int prioridade){
        if (id == null || id.isBlank()){
            throw new IllegalArgumentException("id Tarefa não pode ser vazio");
        }
        this.id = id.trim();

        if (corHex == null || !HEX_RGB.matcher(corHex).matches()){
            throw new IllegalArgumentException("Cor inválida");
        }
        this.corHex = corHex.toUpperCase();

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

    public String getCorHex(){
        return corHex;
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