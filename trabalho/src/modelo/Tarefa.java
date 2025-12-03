package modelo;

import java.util.ArrayList;
import java.util.List;

public class Tarefa {
    private String id;
    private String corHex; // MODIFICADO: Agora armazena o código Hex (ex: "FF0000")
    private int inicio;
    private int duracaoTotal;
    private int prioridade;
    private List<Evento> eventos;

    // Construtor atualizado
    public Tarefa (String id, String corHex, int inicio, int duracaoTotal, int prioridade){
        if (id == null || id.isBlank()){
            throw new IllegalArgumentException("id Tarefa não pode ser vazio");
        }
        this.id = id.trim();

        // Armazena a cor, garantindo que não seja nula (padrão cinza se vazio)
        this.corHex = (corHex != null && !corHex.isBlank()) ? corHex.trim() : "CCCCCC";

        if(inicio < 0){
            throw new IllegalArgumentException("Instante de chegada negativo");
        }
        this.inicio = inicio;

        if(duracaoTotal <= 0 ){
            throw new IllegalArgumentException("Duracao total deve ser maior que zero");
        }
        this.duracaoTotal = duracaoTotal;

        if(prioridade < 0){
            throw new IllegalArgumentException("Prioridade deve ser positiva");
        }
        this.prioridade = prioridade;
        this.eventos = new ArrayList<>();
    }

    public void adicionarEvento(Evento e) {
        this.eventos.add(e);
    }

    public List<Evento> getEventos() {
        return eventos;
    }

    public String resumo(){
        return "%s [chegada=%d, duracao=%d, prio=%d, cor=#%s, eventos=%d]"
                .formatted(id, inicio, duracaoTotal, prioridade, corHex, eventos.size());
    }

    public String getId(){
        return id;
    }

    // Getter modificado
    public String getCorHex(){
        return corHex;
    }

    public int getInicio(){
        return inicio;
    }

    public int getDuracaoTotal(){
        return duracaoTotal;
    }

    public int getPrioridade(){
        return prioridade;
    }
}