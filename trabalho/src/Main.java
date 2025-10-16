import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalTime;

public class Main extends JFrame {
    private final JButton btnPrint = new JButton("Print Status");
    private final JButton btnPrintTs = new JButton("Print Timestamp");
    private final JButton btnSavePng = new JButton("Salvar PNG");

    private final DrawPanel drawPanel = new DrawPanel();
    private int counter = 0;

    public Main() {
        super("UI Mínima — Teste");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 420);
        setLocationRelativeTo(null);
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(btnPrint);
        top.add(btnPrintTs);
        top.add(btnSavePng);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(drawPanel, BorderLayout.CENTER);

        btnPrint.addActionListener(this::onPrint);
        btnPrintTs.addActionListener(this::onPrintTs);
        btnSavePng.addActionListener(this::onSavePng);
    }

    private void onPrint(ActionEvent e) {
        counter++;
        System.out.println("[STATUS] Clique #" + counter + ". Tudo ok.");
        drawPanel.setProgress(counter);
    }

    private void onPrintTs(ActionEvent e) {
        System.out.println("[TS] " + LocalTime.now());
    }

    private void onSavePng(ActionEvent e) {
        try {
            // Renderiza apenas o painel de desenho (pode trocar para a janela inteira se quiser)
            BufferedImage img = new BufferedImage(drawPanel.getWidth(), drawPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            drawPanel.paint(g2);
            g2.dispose();

            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("ui_teste.png"));
            int res = fc.showSaveDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File out = fc.getSelectedFile();
                ImageIO.write(img, "png", out);
                System.out.println("[PNG] Salvo em: " + out.getAbsolutePath());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar PNG: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    // Painel super simples para ter algo visual ao salvar PNG
    static class DrawPanel extends JPanel {
        private int progress = 0; // controla o tamanho do retângulo

        public DrawPanel() {
            setPreferredSize(new Dimension(800, 320));
            setBackground(Color.WHITE);
        }

        public void setProgress(int p) {
            this.progress = Math.max(0, p);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Grade leve só para referência visual
            g2.setColor(new Color(235, 235, 235));
            for (int x = 20; x < w; x += 40) g2.drawLine(x, 0, x, h);
            for (int y = 20; y < h; y += 40) g2.drawLine(0, y, w, y);

            // Retângulo de progresso
            int barW = Math.min(40 + progress * 20, w - 80);
            int barH = 60;
            int x = (w - barW) / 2;
            int y = (h - barH) / 2;

            g2.setColor(new Color(0x4F, 0x81, 0xBD));
            g2.fillRoundRect(x, y, barW, barH, 16, 16);

            g2.setColor(new Color(30, 30, 30));
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            String txt = "Clicks: " + progress;
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(txt)) / 2;
            int ty = y - 12;
            g2.drawString(txt, tx, Math.max(ty, 20));

            g2.dispose();
        }
    }
}