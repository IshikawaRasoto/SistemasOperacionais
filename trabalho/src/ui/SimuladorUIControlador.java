package ui;

import modelo.Tarefa;
import modelo.TCB;
import modelo.Evento;
import modelo.TipoEvento;
import simulador.Relogio;
import sistemaoperacional.SistemaOperacional;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;

public class SimuladorUIControlador {

    private final MainUI ui;
    private SistemaOperacional sistema;
    private final ArrayList<Tarefa> tarefas;
    private String algoritmo;
    private int quantum;
    private int alpha = 0;
    private final Relogio relogio;
    private boolean executando;

    public SimuladorUIControlador(MainUI ui) {
        this.ui = ui;
        this.tarefas = new ArrayList<>();
        this.relogio = Relogio.getInstancia();
        executando = false;
    }

    public boolean isExecutando(){
        return executando;
    }

    public void carregarConfiguracao(String caminho) {
        tarefas.clear();
        this.alpha = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha = br.readLine();
            if (linha == null || linha.isBlank()) {
                throw new IOException("Arquivo de configuração vazio.");
            }

            // Primeira linha -> algoritmo; quantum [;alpha]
            String[] config = linha.split(";");
            if (config.length < 2)
                throw new IOException("Primeira linha deve conter algoritmo e quantum.");

            algoritmo = config[0].trim().toUpperCase();
            quantum = Integer.parseInt(config[1].trim());

            if (config.length >= 3 && !config[2].isBlank()) {
                try {
                    alpha = Integer.parseInt(config[2].trim());
                } catch (NumberFormatException e) {
                    alpha = 0;
                }
            }

            // Linhas seguintes -> tarefas
            // Formato: id; corHex; ingresso; duracao; prioridade
            String linhaTarefa;
            while ((linhaTarefa = br.readLine()) != null) {
                if (linhaTarefa.isBlank()) continue;
                String[] partes = linhaTarefa.split(";");

                // O tamanho mínimo continua sendo 5
                if (partes.length < 5)
                    throw new IOException("Formato inválido: " + linhaTarefa);

                String id = partes[0].trim();
                String corHex = partes[1].trim();
                int inicio = Integer.parseInt(partes[2].trim());
                int duracao = Integer.parseInt(partes[3].trim());
                int prioridade = Integer.parseInt(partes[4].trim());

                Tarefa tarefa = new Tarefa(id, corHex, inicio, duracao, prioridade);

                // --- NOVA LÓGICA DE PARSE DE EVENTOS ---
                // Se houver mais partes na linha, são os eventos (índice 5 em diante)
                if (partes.length > 5) {
                    // O requisito diz "lista_eventos", assumindo que podem vir separados por espaço ou vírgula
                    // mas o split inicial foi por ';'. Se a lista inteira estiver no último campo:
                    // Exemplo: ...; prioridade; ML01: 02, IO: 05-02

                    // Vamos pegar tudo que sobrou
                    String eventosString = partes[5].trim();

                    // Separar múltiplos eventos (assumindo separação por vírgula dentro do campo de eventos)
                    String[] tokensEventos = eventosString.split(",");

                    for (String token : tokensEventos) {
                        token = token.trim();
                        if (token.isEmpty()) continue;

                        parseAndAddEvento(tarefa, token);
                    }
                }
                Terminal.println(tarefa.resumo());
                tarefas.add(tarefa);
            }

            String msg = "Configuração carregada!\nAlgoritmo: " + algoritmo +
                    "\nQuantum: " + quantum +
                    (alpha > 0 ? "\nAlpha: " + alpha : "") +
                    "\nTarefas: " + tarefas.size();

            JOptionPane.showMessageDialog(null, msg);

            ui.setAlgoritmoNaUI(algoritmo);
            ui.setQuantumNaUI(quantum);
            ui.setAlgoritmoStatus(algoritmo, quantum);

            atualizarTabelaInicial();
            ui.getPainelGantt().clear();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao carregar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // --- DEBUG: Imprimir eventos carregados ---
        System.out.println("--- DEBUG: TAREFAS CARREGADAS ---");
        for(Tarefa t : tarefas) {
            System.out.println("Tarefa: " + t.getId() + " | Eventos: " + t.getEventos().size());
            for(modelo.Evento e : t.getEventos()) {
                System.out.println("   -> " + e.toString());
            }
        }
        System.out.println("---------------------------------");
    }

    private void parseAndAddEvento(Tarefa tarefa, String token) {
        // Formatos esperados:
        // IO: 10: xx-yy (Requisito 3.2 - PDF pag 5) -> "10: 05-02"
        // Mutex Lock: MLxx: 00 (Requisito 2.2 - PDF pag 5) -> "ML01: 02"
        // Mutex Unlock: MUxx: 00 (Requisito 2.3 - PDF pag 5) -> "MU01: 05"

        // Normalizar para facilitar
        token = token.toUpperCase(); // "ML01: 02"

        try {
            if (token.startsWith("IO")) {
                // Formato: IO: xx-yy ou 10: xx-yy (O PDF usa '10' como typo de IO provavelmente, ou vice versa, vamos suportar "IO")
                // Vamos assumir separador ":"
                String[] splitIO = token.split(":");
                // splitIO[0] = "IO"
                // splitIO[1] = " xx-yy"

                if (splitIO.length > 1) {
                    String[] tempos = splitIO[1].trim().split("-");
                    int tempoInicio = Integer.parseInt(tempos[0].trim());
                    int duracao = Integer.parseInt(tempos[1].trim());

                    tarefa.adicionarEvento(new Evento(TipoEvento.IO, tempoInicio, duracao));
                }

            } else if (token.startsWith("ML")) {
                // Formato: MLxx: 00
                String[] splitMutex = token.split(":");
                // splitMutex[0] = "ML01" -> precisamos extrair o 01
                String idRecursoStr = splitMutex[0].replace("ML", "").trim();
                int idRecurso = Integer.parseInt(idRecursoStr);

                int tempo = Integer.parseInt(splitMutex[1].trim());

                tarefa.adicionarEvento(new Evento(TipoEvento.MUTEX_SOLICITACAO, tempo, idRecurso, true));

            } else if (token.startsWith("MU")) {
                // Formato: MUxx: 00
                String[] splitMutex = token.split(":");
                String idRecursoStr = splitMutex[0].replace("MU", "").trim();
                int idRecurso = Integer.parseInt(idRecursoStr);

                int tempo = Integer.parseInt(splitMutex[1].trim());

                tarefa.adicionarEvento(new Evento(TipoEvento.MUTEX_LIBERACAO, tempo, idRecurso, true));
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear evento: " + token + " -> " + e.getMessage());
        }
    }

    public void carregarExemploFIFO() {
        if (executando) {
            JOptionPane.showMessageDialog(null, "Finalize a simulação atual antes.");
            return;
        }

        tarefas.clear();
        this.algoritmo = "FIFO";
        this.quantum = 2;
        this.alpha = 0;

        // MODIFICADO: Exemplo agora usa cores HEX
        tarefas.add(new Tarefa("t01", "E74C3C", 0, 5, 3)); // Vermelho
        tarefas.add(new Tarefa("t02", "3498DB", 1, 3, 2)); // Azul
        tarefas.add(new Tarefa("t03", "27AE60", 3, 6, 4)); // Verde
        tarefas.add(new Tarefa("t04", "F1C40F", 3, 2, 9)); // Amarelo
        tarefas.add(new Tarefa("t05", "9B59B6", 4, 3, 1)); // Roxo

        ui.setAlgoritmoNaUI(this.algoritmo);
        ui.setQuantumNaUI(this.quantum);
        ui.setAlgoritmoStatus(this.algoritmo, this.quantum);

        ui.getPainelGantt().clear();
        atualizarTabelaInicial();

        JOptionPane.showMessageDialog(null, "Exemplo FIFO carregado! (Cores Hex)");
    }

    public void iniciarSimulacao(String algoritmoUI, int quantumUI) {
        if(executando) return;

        String algoritmoUsado = (algoritmo != null && !algoritmo.isEmpty()) ? algoritmo : algoritmoUI;
        int quantumUsado = (quantum != 0) ? quantum : quantumUI;

        sistema = new SistemaOperacional(tarefas, algoritmoUsado, quantumUsado, alpha, 1);
        executando = true;
        ui.setEstadoSO(true);

        atualizarUI();
    }

    public void finalizarSimulacao(){
        if(!executando) return;
        relogio.resetar();
        sistema = null;
        executando = false;
        ui.setEstadoSO(false);
        ui.clearAlgoritmoStatus();
        ui.getPainelGantt().clear();
        atualizarTabela();
    }

    public void executarTick() {
        if (sistema == null) return;
        sistema.criarTarefas();
        sistema.gerenciarEventosEBloqueios();
        sistema.verificarTarefasProcessandoEEscalonar();
        atualizarUI();
        sistema.executarProcessos();
        atualizarTabela();
        relogio.tick();
    }

    public void executarAteFim() {
        if (sistema == null) return;
        while (!sistema.terminouTodasTarefas()) {
            sistema.criarTarefas();
            sistema.gerenciarEventosEBloqueios();
            sistema.verificarTarefasProcessandoEEscalonar();
            atualizarUI();
            sistema.executarProcessos();
            relogio.tick();
        }
        atualizarUI();
    }

    private void atualizarUI() {
        if (sistema == null) return;
        ArrayList<TCB> lista = sistema.getListaTCBs();
        ui.getPainelGantt().atualizarGantt(lista, sistema.getTickAtual());
        atualizarTabela();
    }

    private void atualizarTabela() {
        ArrayList<TCB> lista = sistema != null ? sistema.getListaTCBs() : new ArrayList<>();
        Object[][] dados = new Object[lista.size()][6];
        for (int i = 0; i < lista.size(); i++) {
            TCB t = lista.get(i);
            dados[i][0] = t.getTarefa().getId();
            dados[i][1] = t.getTarefa().getInicio();
            dados[i][2] = t.getTarefa().getDuracaoTotal();

            if (alpha > 0) {
                dados[i][3] = t.getPrioridadeDinamica() + " (" + t.getTarefa().getPrioridade() + ")";
            } else {
                dados[i][3] = t.getTarefa().getPrioridade();
            }

            dados[i][4] = t.getEstadoTarefa();
            dados[i][5] = t.getRestante();
        }
        ui.atualizarTabela(dados);
    }

    private void atualizarTabelaInicial() {
        Object[][] dados = new Object[tarefas.size()][6];
        for (int i = 0; i < tarefas.size(); i++) {
            Tarefa t = tarefas.get(i);
            dados[i][0] = t.getId();
            dados[i][1] = t.getInicio();
            dados[i][2] = t.getDuracaoTotal();
            dados[i][3] = t.getPrioridade();
            dados[i][4] = "AGUARDANDO";
            dados[i][5] = t.getDuracaoTotal();
        }
        ui.atualizarTabela(dados);
    }

    public void exportarGanttComoImagem() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar gráfico de Gantt como PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("Imagem PNG (*.png)", "png"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(new File("gantt_resultado.png"));
        int res = chooser.showSaveDialog(null);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File destino = chooser.getSelectedFile();
        String name = destino.getName().toLowerCase();
        if (!name.endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        ui.getPainelGantt().exportarComoPNG(destino);
    }
}