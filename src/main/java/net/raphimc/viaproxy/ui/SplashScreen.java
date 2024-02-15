/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SplashScreen extends JFrame {

    private final ProgressPanel progressPanel = new ProgressPanel();

    public SplashScreen() throws IOException {
        this.setAlwaysOnTop(true);
        this.setUndecorated(true);
        this.setBackground(new Color(0, 0, 0, 0));
        this.setType(Window.Type.UTILITY);
        this.setSize(300, 235);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.init();
        this.setVisible(true);
    }

    private void init() throws IOException {
        JPanel contentPane = new JPanel();
        contentPane.setOpaque(false);
        contentPane.setBackground(new Color(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new SplashPanel(ImageIO.read(SplashScreen.class.getResourceAsStream("/assets/icons/icon.png"))), BorderLayout.CENTER);
        contentPane.add(this.progressPanel, BorderLayout.SOUTH);
        this.setContentPane(contentPane);
    }

    public float getProgress() {
        return this.progressPanel.progress;
    }

    public void setProgress(final float progress) {
        this.progressPanel.progress = Math.max(0, Math.min(1, progress));
        this.progressPanel.repaint();
    }

    public void setText(final String text) {
        this.progressPanel.text = text;
        this.progressPanel.repaint();
    }


    private static class SplashPanel extends JPanel {
        private final BufferedImage image;

        public SplashPanel(final BufferedImage image) {
            this.image = image;
            this.setOpaque(false);
            this.setBackground(new Color(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.drawImage(this.image, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }

    private static class ProgressPanel extends JPanel {
        private float progress = 0;
        private String text = "";

        public ProgressPanel() {
            this.setOpaque(false);
            this.setBackground(new Color(0, 0, 0, 0));
            this.setPreferredSize(new Dimension(this.getWidth(), 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }

            g.setColor(Color.WHITE);
            g.drawRect(0, 5, this.getWidth(), this.getHeight() - 5);
            g.drawRect(1, 6, this.getWidth() - 2, this.getHeight() - 7);
            g.setColor(new Color(0, 69, 104));
            g.fillRect(2, 7, this.getWidth() - 4, this.getHeight() - 9);

            g.setColor(new Color(2, 188, 216));
            int progressWidth = this.getWidth() - 4;
            progressWidth = (int) (progressWidth * this.progress);
            g.fillRect(2, 7, progressWidth, this.getHeight() - 9);

            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(15F));
            FontMetrics metrics = g.getFontMetrics();
            int textWidth = metrics.stringWidth(this.text);
            g.drawString(this.text, (this.getWidth() - textWidth) / 2, (this.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() + metrics.getDescent() / 2);
        }
    }

}
