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

public class PainelGantt extends JPanel {

    // Histórico do Gantt
    private final Map<String, List<Byte>> hist = new LinkedHashMap<>();
    private final Map<String, Color> coresTarefas = new HashMap<>();
    private int tickAtual = 0;

    public PainelGantt() {
        setBackground(Color.WHITE);
    }

    public void clear(){
        hist.clear();
        coresTarefas.clear();
        tickAtual = 0;
        repaint();
    }

    public void atualizarGantt(ArrayList<TCB> tarefas, int tickDoSO) {
        this.tickAtual = tickDoSO;

        for (TCB t : tarefas) {
            String id = t.getTarefa().getId();
            hist.putIfAbsent(id, new ArrayList<>());

            // MODIFICADO: Decodifica a cor hexadecimal da tarefa
            if (!coresTarefas.containsKey(id)) {
                String hex = t.getTarefa().getCorHex();
                Color cor;
                try {
                    // Converte "F0E0D0" para int e cria a cor
                    cor = new Color(Integer.parseInt(hex, 16));
                } catch (Exception e) {
                    cor = Color.DARK_GRAY; // Fallback se o hex for inválido
                }
                coresTarefas.put(id, cor);
            }
        }

        for (Map.Entry<String, List<Byte>> e : hist.entrySet()) {
            String id = e.getKey();
            List<Byte> linha = e.getValue();

            while (linha.size() < tickDoSO) linha.add((byte)0);

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
            else linha.set(tickDoSO, estadoTick);
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
        int margemDir = 20;

        int linhas = Math.max(1, hist.size());
        int linhaAltura = Math.max(25, (altura - margemTop - margemInf - 20) / linhas);

        int numCols = tickAtual + 1;
        int areaUtil = Math.max(1, largura - margemEsq - margemDir);
        int tickLargura = Math.max(6, areaUtil / Math.max(1, numCols));

        int linha = 0;
        for (String id : hist.keySet()) {
            int y = margemTop + linha * linhaAltura;
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(margemEsq, y, largura - margemDir, y);
            g2.setColor(Color.BLACK);
            g2.drawString(id, 20, y + linhaAltura / 2);
            linha++;
        }

        linha = 0;
        for (Map.Entry<String, List<Byte>> entry : hist.entrySet()) {
            String id = entry.getKey();
            List<Byte> ticks = entry.getValue();
            int y = margemTop + linha * linhaAltura + 5;

            int maxT = Math.min(numCols, ticks.size());
            for (int t = 0; t < maxT; t++) {
                byte estado = ticks.get(t);
                if (estado == 0) continue;

                int x = margemEsq + t * tickLargura;
                int bw = tickLargura - 2;
                int bh = linhaAltura - 10;

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

        int yEixo = altura - margemInf;
        g2.setColor(Color.BLACK);
        g2.drawLine(margemEsq, yEixo, largura - margemDir, yEixo);

        for (int t = 0; t < numCols; t++) {
            int x = margemEsq + t * tickLargura;
            if (x > largura - margemDir) break;
            g2.drawLine(x, yEixo - 5, x, yEixo + 5);
            if (t % 5 == 0 || t == tickAtual) g2.drawString(String.valueOf(t), x - 5, yEixo + 20);
        }
    }

    public void exportarComoPNG(File destino) {
        try {
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            paint(g2);
            g2.dispose();
            ImageIO.write(image, "png", destino);
            JOptionPane.showMessageDialog(this, "Imagem exportada com sucesso:\n" + destino);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar imagem:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void exportarComoPNG(String caminho) {
        exportarComoPNG(new File(caminho));
    }
}