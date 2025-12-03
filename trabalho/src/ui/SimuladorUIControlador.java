package ui;

import modelo.Tarefa;
import modelo.TCB;
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
    private int alpha = 0; // NOVO: Armazena o fator de envelhecimento
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
        this.alpha = 0; // Resetar alpha ao carregar novo arquivo

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha = br.readLine();
            if (linha == null || linha.isBlank()) {
                throw new IOException("Arquivo de configuração vazio.");
            }

            // Primeira linha -> algoritmo;quantum [;alpha]
            String[] config = linha.split(";");
            if (config.length < 2)
                throw new IOException("Primeira linha deve conter algoritmo e quantum, separados por ';'.");

            algoritmo = config[0].trim().toUpperCase();
            quantum = Integer.parseInt(config[1].trim());

            // NOVO: Leitura opcional do Alpha (se existir) para Projeto B
            if (config.length >= 3 && !config[2].isBlank()) {
                try {
                    alpha = Integer.parseInt(config[2].trim());
                } catch (NumberFormatException e) {
                    alpha = 0;
                }
            }

            // Linhas seguintes -> tarefas
            String linhaTarefa;
            while ((linhaTarefa = br.readLine()) != null) {
                if (linhaTarefa.isBlank()) continue;
                String[] partes = linhaTarefa.split(";");
                if (partes.length < 5)
                    throw new IOException("Formato inválido em linha de tarefa: " + linhaTarefa);

                String id = partes[0].trim();

                // Adaptação para suportar tanto int quanto HEX na cor (Projeto B pede HEX)
                int corIdx = 0;
                try {
                    corIdx = Integer.parseInt(partes[1].trim());
                } catch (NumberFormatException e) {
                    // Se for HEX ou string, gera um índice simples para não quebrar a UI
                    corIdx = Math.abs(partes[1].hashCode()) % 8;
                }

                int inicio = Integer.parseInt(partes[2].trim());
                int duracao = Integer.parseInt(partes[3].trim());
                int prioridade = Integer.parseInt(partes[4].trim());

                Tarefa tarefa = new Tarefa(id, corIdx, inicio, duracao, prioridade);
                tarefas.add(tarefa);
            }

            String msg = "Configuração carregada com sucesso!\nAlgoritmo: " + algoritmo +
                    "\nQuantum: " + quantum +
                    (alpha > 0 ? "\nAlpha (Envelhecimento): " + alpha : "") +
                    "\nTarefas carregadas: " + tarefas.size();

            JOptionPane.showMessageDialog(null, msg);

            // sincroniza UI de seleção
            ui.setAlgoritmoNaUI(algoritmo);
            ui.setQuantumNaUI(quantum);

            // status visível (algoritmo carregado e quantum)
            ui.setAlgoritmoStatus(algoritmo, quantum);

            atualizarTabelaInicial();
            ui.getPainelGantt().clear();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Erro ao carregar configuração:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void carregarExemploFIFO() {
        if (executando) {
            JOptionPane.showMessageDialog(null, "Finalize a simulação atual antes de carregar um exemplo.");
            return;
        }

        tarefas.clear();

        // define algoritmo e quantum do exemplo
        this.algoritmo = "FIFO";
        this.quantum = 2;
        this.alpha = 0; // Resetar alpha

        tarefas.add(new Tarefa("t01", 0, 0, 5, 3));
        tarefas.add(new Tarefa("t02", 1, 0, 3, 2));
        tarefas.add(new Tarefa("t03", 2, 3, 6, 4));
        tarefas.add(new Tarefa("t04", 3, 3, 2, 9));
        tarefas.add(new Tarefa("t05", 4, 4, 3, 1));


        // sincroniza os campos visuais
        ui.setAlgoritmoNaUI(this.algoritmo);
        ui.setQuantumNaUI(this.quantum);
        ui.setAlgoritmoStatus(this.algoritmo, this.quantum);


        // limpa o Gantt para novo cenário
        ui.getPainelGantt().clear();

        // preenche a tabela com o snapshot "apenasTarefas"
        atualizarTabelaInicial();

        JOptionPane.showMessageDialog(null, "Exemplo FIFO carregado com sucesso!\nQuantum: " + this.quantum + "\nTarefas: " + this.tarefas.size());
    }

    public void iniciarSimulacao(String algoritmoUI, int quantumUI) {
        if(executando) return;

        // Se carregou do arquivo (e não limpou), usa as vars da classe.
        // Se o usuário mudou na combo box, usa da UI.
        String algoritmoUsado = (algoritmo != null && !algoritmo.isEmpty()) ? algoritmo : algoritmoUI;
        int quantumUsado = (quantum != 0) ? quantum : quantumUI;

        // NOVO: Passando alpha para o construtor do SO
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

    // ---------------------------------------------------------
    // Modos de execução
    // ---------------------------------------------------------
    public void executarTick() {
        if (sistema == null) return;
        sistema.criarTarefas();
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
            sistema.verificarTarefasProcessandoEEscalonar();
            atualizarUI();
            sistema.executarProcessos();
            relogio.tick();
        }
        atualizarUI();
    }

    // ---------------------------------------------------------
    // Atualização da interface
    // ---------------------------------------------------------
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

            // NOVO: Exibe prioridade dinâmica se houver envelhecimento
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

    // ---------------------------------------------------------
    // Exportação do gráfico de Gantt
    // ---------------------------------------------------------
    public void exportarGanttComoImagem() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar gráfico de Gantt como PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("Imagem PNG (*.png)", "png"));
        chooser.setAcceptAllFileFilterUsed(false);

        // nome sugerido
        chooser.setSelectedFile(new File("gantt_resultado.png"));

        int res = chooser.showSaveDialog(null);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File destino = chooser.getSelectedFile();

        // garante extensão .png
        String name = destino.getName().toLowerCase();
        if (!name.endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }

        // confirma overwrite
        if (destino.exists()) {
            int op = JOptionPane.showConfirmDialog(
                    null,
                    "O arquivo já existe.\nDeseja sobrescrever?\n\n" + destino.getAbsolutePath(),
                    "Confirmar sobrescrita",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (op != JOptionPane.YES_OPTION) return;
        }

        ui.getPainelGantt().exportarComoPNG(destino);
    }
}