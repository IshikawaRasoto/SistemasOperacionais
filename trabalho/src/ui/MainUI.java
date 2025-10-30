package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

public class MainUI extends JFrame {

    private final SimuladorUIControlador controlador;
    private final JComboBox<String> algoritmoCombo;
    private final JSpinner quantumSpinner;
    private final JButton carregarButton, carregarExemploButton, startStopButton, tickButton, runButton, exportarButton;
    private final JTable tabelaTarefas;
    private final PainelGantt painelGantt;
    private final JLabel statusSoLabel;
    private final JLabel statusAlgLabel;
    private final JLabel statusQuantumLabel;

    public MainUI() {
        super("Simulador de Escalonamento de Tarefas - Projeto A");

        controlador = new SimuladorUIControlador(this);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Painel superior (configuração)
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuração do Sistema"));

        algoritmoCombo = new JComboBox<>(new String[]{"FIFO", "SRTF", "PRIORIDADE_PREEMPTIVO"});
        quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        carregarButton = new JButton("Carregar Arquivo");
        carregarExemploButton = new JButton("Carregar Exemplo"); // << novo
        startStopButton = new JButton("Iniciar Simulação"); // Depois vira o botao de finalizar simulacao

        // Inidicador de execucao do SO
        statusSoLabel = new JLabel("● Parado");
        statusSoLabel.setForeground(new Color(200, 0, 0)); // vermelho quando parado
        statusSoLabel.setFont(statusSoLabel.getFont().deriveFont(Font.BOLD, 13f));

        statusAlgLabel = new JLabel("-");
        statusQuantumLabel = new JLabel("-");

        //configPanel.add(new JLabel("Algoritmo:"));
        //configPanel.add(algoritmoCombo);
        //configPanel.add(new JLabel("Quantum:"));
        //configPanel.add(quantumSpinner);
        configPanel.add(carregarButton);
        configPanel.add(carregarExemploButton);
        configPanel.add(startStopButton);
        configPanel.add(new JLabel("SO:"));
        configPanel.add(statusSoLabel);

        configPanel.add(Box.createHorizontalStrut(20));
        configPanel.add(new JLabel("Algoritmo carregado:"));
        configPanel.add(statusAlgLabel);
        configPanel.add(Box.createHorizontalStrut(8));
        configPanel.add(new JLabel("Quantum:"));
        configPanel.add(statusQuantumLabel);

        add(configPanel, BorderLayout.NORTH);

        // Painel central (tabela + Gantt)
        JPanel centerPanel = new JPanel(new GridLayout(2, 1));

        tabelaTarefas = new JTable(new DefaultTableModel(
                new Object[]{"ID", "Chegada", "Duração", "Prioridade", "Estado", "Restante"}, 0
        ));
        JScrollPane scrollTabela = new JScrollPane(tabelaTarefas);
        scrollTabela.setBorder(BorderFactory.createTitledBorder("Tarefas"));

        painelGantt = new PainelGantt();
        painelGantt.setBorder(BorderFactory.createTitledBorder("Gráfico de Gantt"));

        centerPanel.add(scrollTabela);
        centerPanel.add(painelGantt);

        add(centerPanel, BorderLayout.CENTER);

        // Painel inferior (controles)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controle da Simulação"));

        tickButton = new JButton("Executar Tick");
        runButton = new JButton("Executar até o fim");
        exportarButton = new JButton("Exportar PNG");

        controlPanel.add(tickButton);
        controlPanel.add(runButton);
        controlPanel.add(exportarButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Ações dos botões
        carregarButton.addActionListener(e -> escolherArquivoConfiguracao());
        carregarExemploButton.addActionListener(e -> controlador.carregarExemploFIFO());
        startStopButton.addActionListener(e -> {
            if (!controlador.isExecutando()) {
                controlador.iniciarSimulacao(
                        (String) algoritmoCombo.getSelectedItem(),
                        (Integer) quantumSpinner.getValue()
                );
            } else {
                controlador.finalizarSimulacao();
            }
        });
        tickButton.addActionListener(e -> controlador.executarTick());
        runButton.addActionListener(e -> controlador.executarAteFim());
        exportarButton.addActionListener(e -> controlador.exportarGanttComoImagem());

        setEstadoSO(false);
        setVisible(true);
    }

    private void escolherArquivoConfiguracao() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            controlador.carregarConfiguracao(arquivo.getAbsolutePath());
        }
    }

    public DefaultTableModel getModeloTabela() {
        return (DefaultTableModel) tabelaTarefas.getModel();
    }

    public PainelGantt getPainelGantt() {
        return painelGantt;
    }

    public void atualizarTabela(Object[][] dados) {
        DefaultTableModel model = getModeloTabela();
        model.setRowCount(0);
        for (Object[] linha : dados) {
            model.addRow(linha);
        }
    }

    public void setEstadoSO(boolean executando) {
        if (executando) {
            statusSoLabel.setText("● Executando");
            statusSoLabel.setForeground(new Color(0, 140, 0)); // verde
            startStopButton.setText("Finalizar Simulação");
            // Durante execução, evita trocar algoritmo/quantum
            algoritmoCombo.setEnabled(false);
            quantumSpinner.setEnabled(false);
            carregarButton.setEnabled(false);
            // A UI permite tick a tick e run-to-end durante execução
            tickButton.setEnabled(true);
            runButton.setEnabled(true);
        } else {
            statusSoLabel.setText("● Parado");
            statusSoLabel.setForeground(new Color(200, 0, 0)); // vermelho
            startStopButton.setText("Iniciar Simulação");
            algoritmoCombo.setEnabled(true);
            quantumSpinner.setEnabled(true);
            carregarButton.setEnabled(true);
            tickButton.setEnabled(false);
            runButton.setEnabled(false);
        }
    }

    // setters auxiliares para sincronizar os campos de seleção
    public void setAlgoritmoNaUI(String algoritmo) {
        algoritmoCombo.setSelectedItem(algoritmo);
    }
    public void setQuantumNaUI(int q) {
        quantumSpinner.setValue(q);
    }

    // NOVOS: controlar a exibição do algoritmo/quantum carregados
    public void setAlgoritmoStatus(String algoritmo, Integer quantum) {
        statusAlgLabel.setText(algoritmo != null ? algoritmo : "-");
        statusQuantumLabel.setText(quantum != null ? String.valueOf(quantum) : "-");
    }
    public void clearAlgoritmoStatus() {
        statusAlgLabel.setText("-");
        statusQuantumLabel.setText("-");
    }
}
