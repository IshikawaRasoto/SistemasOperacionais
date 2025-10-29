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
 * Atualizado dinamicamente pelo SO: chame atualizarGantt(snapshot, tickDoSO).
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
    private final Map<String, List<Byte>> hist = new LinkedHashMap<>();
    private final Map<String, Color> coresTarefas = new HashMap<>();
    private int tickAtual = 0;

    public PainelGantt() {
        setBackground(Color.WHITE);
    }

    private static Color corPorIndice(int idx){
        if (idx < 0) idx = -idx;
        if (idx < PALETA.length) return PALETA[idx];
        float h = (idx % 360) / 360f;
        Color c = Color.getHSBColor(h, 0.75f, 0.95f);
        int sum = c.getRed() + c.getGreen() + c.getBlue();
        return (sum > 700) ? c.darker().darker() : c;
    }

    /**
     * Atualiza o histórico do Gantt no índice exato do tick informado pelo SO.
     * @param tarefas  snapshot das TCBs no tick atual (após decisão do escalonador)
     * @param tickDoSO índice do tick (0,1,2,...) que estamos registrando
     */
    public void atualizarGantt(ArrayList<TCB> tarefas, int tickDoSO) {
        this.tickAtual = tickDoSO;

        // Garante entrada e cor para todas as tarefas presentes no snapshot
        for (TCB t : tarefas) {
            String id = t.getTarefa().getId();
            hist.putIfAbsent(id, new ArrayList<>());
            int idxCor = t.getTarefa().getCor(); // índice/categoria vindo da config
            coresTarefas.putIfAbsent(id, corPorIndice(idxCor));
        }

        // Para cada tarefa já conhecida, grava o estado no índice == tickDoSO
        for (Map.Entry<String, List<Byte>> e : hist.entrySet()) {
            String id = e.getKey();
            List<Byte> linha = e.getValue();

            // Preenche buracos até (tickDoSO - 1) com OUTRO (0)
            while (linha.size() < tickDoSO) linha.add((byte)0);

            // Estado default OUTRO
            byte estadoTick = 0;
            TCB achada = null;
            for (TCB t : tarefas) {
                if (t.getTarefa().getId().equals(id)) { achada = t; break; }
            }
            if (achada != null) {
                if (achada.getEstadoTarefa() == EstadoTarefa.EXECUTANDO) estadoTick = 2;
                else if (achada.getEstadoTarefa() == EstadoTarefa.PRONTA) estadoTick = 1;
            }

            if (linha.size() == tickDoSO) linha.add(estadoTick);
            else linha.set(tickDoSO, estadoTick); // sobrescreve se já havia
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

        // Layout
        int altura = getHeight();
        int largura = getWidth();
        int margemEsq = 80;
        int margemTop = 30;
        int margemInf = 40;
        int margemDir = 20;

        int linhas = Math.max(1, hist.size());
        int linhaAltura = Math.max(25, (altura - margemTop - margemInf - 20) / linhas);

        // Número de colunas visíveis = ticks registrados até o atual (0..tickAtual)
        int numCols = tickAtual + 1;
        int areaUtil = Math.max(1, largura - margemEsq - margemDir);
        int tickLargura = Math.max(6, areaUtil / Math.max(1, numCols));

        // Grade + labels
        int linha = 0;
        for (String id : hist.keySet()) {
            int y = margemTop + linha * linhaAltura;
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(margemEsq, y, largura - margemDir, y);
            g2.setColor(Color.BLACK);
            g2.drawString(id, 20, y + linhaAltura / 2);
            linha++;
        }

        // Blocos por estado: só desenhar até numCols (evita "pintar à frente")
        linha = 0;
        for (Map.Entry<String, List<Byte>> entry : hist.entrySet()) {
            String id = entry.getKey();
            List<Byte> ticks = entry.getValue();
            int y = margemTop + linha * linhaAltura + 5;

            int maxT = Math.min(numCols, ticks.size());
            for (int t = 0; t < maxT; t++) {
                byte estado = ticks.get(t);
                if (estado == 0) continue; // OUTRO -> não pinta

                int x = margemEsq + t * tickLargura;
                int bw = tickLargura - 2;
                int bh = linhaAltura - 10;

                // Clamp para não estourar a borda direita
                if (x >= largura - margemDir) break;
                if (x + bw > largura - margemDir) bw = (largura - margemDir) - x;

                if (estado == 2) {
                    Color cor = coresTarefas.getOrDefault(id, Color.GRAY);
                    g2.setColor(cor);
                } else { // 1 = PRONTA
                    g2.setColor(new Color(180, 180, 180));
                }
                g2.fillRect(x, y, bw, bh);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, bw, bh);
            }
            linha++;
        }

        // === EIXO DO TEMPO (inferior) ===
        int yEixo = altura - margemInf;
        g2.setColor(Color.BLACK);
        g2.drawLine(margemEsq, yEixo, largura - margemDir, yEixo);

        for (int t = 0; t < numCols; t++) {
            int x = margemEsq + t * tickLargura;
            if (x > largura - margemDir) break;
            g2.drawLine(x, yEixo - 5, x, yEixo + 5);
            if (t % 5 == 0 || t == tickAtual) g2.drawString(String.valueOf(t), x - 5, yEixo + 20);
        }

        // Linha vermelha do tick atual (clampeada)
        /*
        g2.setColor(Color.RED);
        int xTick = margemEsq + tickAtual * tickLargura;
        xTick = Math.min(xTick, largura - margemDir);
        g2.drawLine(xTick, margemTop - 10, xTick, yEixo);
        g2.drawString("Tick: " + tickAtual, Math.min(largura - 120, xTick + 10), yEixo + 25);
         */
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
            JOptionPane.showMessageDialog(this, "Erro ao exportar imagem:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
