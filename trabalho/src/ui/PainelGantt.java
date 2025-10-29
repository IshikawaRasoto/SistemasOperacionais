package ui;

import modelo.TCB;
import modelo.EstadoTarefa;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Painel que desenha o gráfico de Gantt da simulação.
 * Cada linha representa uma tarefa, e o eixo X representa o tempo (ticks).
 * Atualizado dinamicamente a cada execTick() do Sistema Operacional.
 *
 * Regras visuais:
 * - EXECUTANDO  -> cor da tarefa (paleta da UI)
 * - PRONTA      -> cinza
 * - OUTROS (bloqueada, finalizada, inexistente) -> não pinta (fundo branco)
 */
public class PainelGantt extends JPanel {

    // Paleta fixa com boa distinção
    private static final Color[] PALETA = new Color[] {
            new Color(0xE74C3C), // vermelho
            new Color(0x3498DB), // azul
            new Color(0x27AE60), // verde
            new Color(0xF1C40F), // amarelo
            new Color(0x9B59B6), // roxo
            new Color(0xE67E22), // laranja
            new Color(0x16A085), // turquesa
            new Color(0x2C3E50)  // azul escuro
    };

    // Histórico do Gantt: ID -> lista de estados por tick
    // 0 = OUTRO (não pinta), 1 = PRONTA (cinza), 2 = EXECUTANDO (cor)
    private final Map<String, List<Byte>> hist;
    private final Map<String, Color> coresTarefas;
    private int tickAtual = 0;

    public PainelGantt() {
        this.hist = new LinkedHashMap<>();
        this.coresTarefas = new HashMap<>();
        setBackground(Color.WHITE);
    }

    private static Color corPorIndice(int idx){
        if (idx < 0) idx = -idx;
        if (idx < PALETA.length) return PALETA[idx];
        float h = (idx % 360) / 360f;
        Color c = Color.getHSBColor(h, 0.75f, 0.95f);
        // evitar muito claro
        int sum = c.getRed() + c.getGreen() + c.getBlue();
        return (sum > 700) ? c.darker().darker() : c;
    }

    /**
     * Atualiza o histórico do Gantt para o próximo tick,
     * registrando o estado de cada tarefa no snapshot atual.
     */
    public void atualizarGantt(ArrayList<TCB> tarefas, int tickSimulacao) {
        tickAtual = tickSimulacao;

        // Garante entrada e cor para todas as tarefas presentes no snapshot
        for (TCB t : tarefas) {
            String id = t.getTarefa().getId();
            hist.putIfAbsent(id, new ArrayList<>());
            int idxCor = t.getTarefa().getCor(); // índice/categoria vindo do config
            coresTarefas.putIfAbsent(id, corPorIndice(idxCor));
        }

        // Para cada tarefa já conhecida, acrescenta o estado do tick corrente
        for (Map.Entry<String, List<Byte>> e : hist.entrySet()) {
            String id = e.getKey();
            List<Byte> linha = e.getValue();

            // Preenche buracos até tickAtual-1 com OUTRO (0) -> não pinta
            while (linha.size() < tickAtual - 1) {
                linha.add((byte)0);
            }

            // Busca a TCB correspondente no snapshot atual
            TCB tcb = null;
            for (TCB t : tarefas) {
                if (t.getTarefa().getId().equals(id)) {
                    tcb = t;
                    break;
                }
            }

            byte estadoTick = 0; // default OUTRO
            if (tcb != null) {
                EstadoTarefa est = tcb.getEstadoTarefa();
                if (est == EstadoTarefa.EXECUTANDO) {
                    estadoTick = 2;
                } else if (est == EstadoTarefa.PRONTA) {
                    estadoTick = 1;
                } else {
                    estadoTick = 0; // bloqueada, finalizada, etc.
                }
            }
            linha.add(estadoTick);
        }

        // Normaliza comprimentos (se necessário)
        for (List<Byte> linha : hist.values()) {
            while (linha.size() < tickAtual) {
                linha.add((byte)0);
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (hist.isEmpty()) {
            g.setColor(Color.GRAY);
            g.drawString("O gráfico de Gantt será exibido aqui durante a execução.", 50, 50);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int altura = getHeight();
        int largura = getWidth();
        int margemEsq = 80;
        int margemTop = 30;
        int margemInf = 40;
        int linhas = Math.max(1, hist.size());
        int linhaAltura = Math.max(25, (altura - margemTop - 40) / linhas);
        int tickLargura = Math.max(10, (largura - margemEsq - 50) / Math.max(1, tickAtual));

        // Grade e labels
        int linha = 0;
        for (String id : hist.keySet()) {
            int y = margemTop + linha * linhaAltura;
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(margemEsq, y, largura - 20, y);
            g2.setColor(Color.BLACK);
            g2.drawString(id, 20, y + linhaAltura / 2);
            linha++;
        }

        // Blocos por estado (2=EXEC, 1=PRONTA, 0=OUTRO não pinta)
        linha = 0;
        for (Map.Entry<String, List<Byte>> entry : hist.entrySet()) {
            String id = entry.getKey();
            List<Byte> ticks = entry.getValue();
            int y = margemTop + linha * linhaAltura + 5;

            for (int t = 0; t < ticks.size(); t++) {
                byte estado = ticks.get(t);
                if (estado == 0) continue; // OUTRO -> não desenha

                int x = margemEsq + t * tickLargura;
                int bw = tickLargura - 2;
                int bh = linhaAltura - 10;

                if (estado == 2) {
                    // EXECUTANDO -> cor da tarefa
                    Color cor = coresTarefas.getOrDefault(id, Color.GRAY);
                    g2.setColor(cor);
                    g2.fillRect(x, y, bw, bh);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x, y, bw, bh);
                } else if (estado == 1) {
                    // PRONTA -> cinza
                    g2.setColor(new Color(180, 180, 180));
                    g2.fillRect(x, y, bw, bh);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x, y, bw, bh);
                }
            }
            linha++;
        }

        // Eixo do tempo
        int yEixo = altura - margemInf; // posição da linha do eixo
        g2.setColor(Color.BLACK);
        g2.drawLine(margemEsq, yEixo, largura - 20, yEixo);

        for (int t = 0; t <= tickAtual; t++) {
            int x = margemEsq + t * tickLargura;
            g2.drawLine(x, yEixo - 5, x, yEixo + 5); // marca de tick
            if (t % 5 == 0 || t == tickAtual)
                g2.drawString(String.valueOf(t), x - 5, yEixo + 20);
        }

        // Tick atual
        g2.setColor(Color.RED);
        g2.drawLine(margemEsq + tickAtual * tickLargura, margemTop - 10, margemEsq + tickAtual * tickLargura, altura - 20);
        g2.drawString("Tick: " + tickAtual, largura - 100, altura - 15);
    }

    /**
     * Exporta o gráfico de Gantt atual como uma imagem PNG.
     */
    public void exportarComoPNG(String caminho) {
        try {
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            paint(g2);
            g2.dispose();
            ImageIO.write(image, "png", new File(caminho));
            JOptionPane.showMessageDialog(this, "Imagem exportada com sucesso:\n" + caminho);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar imagem:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
