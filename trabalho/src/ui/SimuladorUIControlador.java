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
    private ArrayList<Tarefa> tarefas;
    private String algoritmo;
    private int quantum;
    private Relogio relogio;
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
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha = br.readLine();
            if (linha == null || linha.isBlank()) {
                throw new IOException("Arquivo de configuração vazio.");
            }

            // Primeira linha -> algoritmo;quantum
            String[] config = linha.split(";");
            if (config.length < 2)
                throw new IOException("Primeira linha deve conter algoritmo e quantum, separados por ';'.");

            algoritmo = config[0].trim().toUpperCase();
            quantum = Integer.parseInt(config[1].trim());

            // Linhas seguintes -> tarefas
            String linhaTarefa;
            while ((linhaTarefa = br.readLine()) != null) {
                if (linhaTarefa.isBlank()) continue;
                String[] partes = linhaTarefa.split(";");
                if (partes.length < 5)
                    throw new IOException("Formato inválido em linha de tarefa: " + linhaTarefa);

                String id = partes[0].trim();
                int corIdx = Integer.parseInt(partes[1].trim());
                int inicio = Integer.parseInt(partes[2].trim());
                int duracao = Integer.parseInt(partes[3].trim());
                int prioridade = Integer.parseInt(partes[4].trim());

                Tarefa tarefa = new Tarefa(id, corIdx, inicio, duracao, prioridade);
                tarefas.add(tarefa);
            }

            JOptionPane.showMessageDialog(null,
                    "Configuração carregada com sucesso!\nAlgoritmo: " + algoritmo +
                            "\nQuantum: " + quantum +
                            "\nTarefas carregadas: " + tarefas.size());

            atualizarTabelaInicial();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Erro ao carregar configuração:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    public void iniciarSimulacao(String algoritmoUI, int quantumUI) {
        if(executando) return;

        String algoritmoUsado = (algoritmo != null) ? algoritmo : algoritmoUI;
        int quantumUsado = (quantum != 0) ? quantum : quantumUI;

        sistema = new SistemaOperacional(tarefas, algoritmoUsado, quantumUsado, 1);
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
        ArrayList<TCB> lista = sistema != null ? sistema.getListaTCBs() : new ArrayList<>();

        Object[][] dados = new Object[lista.size()][6];
        for (int i = 0; i < lista.size(); i++) {
            TCB t = lista.get(i);
            dados[i][0] = t.getTarefa().getId();
            dados[i][1] = t.getTarefa().getInicio();
            dados[i][2] = t.getTarefa().getDuracaoTotal();
            dados[i][3] = t.getTarefa().getPrioridade();
            dados[i][4] = t.getEstadoTarefa();
            dados[i][5] = t.getRestante();
        }

        ui.atualizarTabela(dados);
        ui.getPainelGantt().atualizarGantt(lista, sistema.getTickAtual());
    }

    private void atualizarTabela() {
        ArrayList<TCB> lista = sistema != null ? sistema.getListaTCBs() : new ArrayList<>();
        Object[][] dados = new Object[lista.size()][6];
        for (int i = 0; i < lista.size(); i++) {
            TCB t = lista.get(i);
            dados[i][0] = t.getTarefa().getId();
            dados[i][1] = t.getTarefa().getInicio();
            dados[i][2] = t.getTarefa().getDuracaoTotal();
            dados[i][3] = t.getTarefa().getPrioridade();
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
