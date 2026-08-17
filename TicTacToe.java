import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicTacToe extends JFrame {
    private static final String CARD_HOME = "HOME";
    private static final String CARD_GAME = "GAME";

    private static final int BOARD_SIZE = 3;
    private final JButton[][] buttons = new JButton[BOARD_SIZE][BOARD_SIZE];

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);

    // Player State
    private String playerXName = "Player 1";
    private String playerOName = "Player 2";
    private boolean isXTurn = true;
    private boolean isGameOver = false;
    private int scoreX = 0;
    private int scoreO = 0;

    // Home Screen Input Fields
    private JTextField nameXField;
    private JTextField nameOField;

    // Game Screen Labels
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel scoreLabel = new JLabel("", SwingConstants.CENTER);

    // Overlay for Celebration Animations
    private CelebrationOverlay celebrationOverlay;
    private Timer autoContinueTimer;

    // Synthesized Move Sound Buffer
    private static byte[] moveSoundBytes;
    private static AudioFormat audioFormat;

    // UI Theme Palette (Blue & Red Theme)
    private static final Color COLOR_BG = new Color(30, 30, 46);       // Dark Slate #1E1E2E
    private static final Color COLOR_CARD = new Color(49, 50, 68);      // Container #313244
    private static final Color COLOR_BUTTON = new Color(69, 71, 90);   // Button base #45475A
    private static final Color COLOR_TEXT = new Color(205, 214, 244);   // Neutral text #CDD6F4
    private static final Color COLOR_X = new Color(59, 130, 246);      // Electric Blue #3B82F6
    private static final Color COLOR_O = new Color(239, 68, 68);       // Crimson Red #EF4444
    private static final Color COLOR_ACCENT = new Color(166, 227, 161); // Mint Green #A6E3A1
    private static final Color COLOR_MUTED = new Color(147, 153, 178);  // Muted Gray
    private static final Color COLOR_WIN = new Color(249, 226, 175);    // Gold #F9E2AF

    private static final String HEX_BLUE = "#3B82F6";
    private static final String HEX_RED = "#EF4444";

    static {
        initSound();
    }

    public TicTacToe() {
        setTitle("Tic Tac Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // Set Custom Window Title Bar & Taskbar Logo Icon
        setIconImage(createWindowIcon());

        containerPanel.add(createHomePanel(), CARD_HOME);
        containerPanel.add(createGamePanelWithOverlay(), CARD_GAME);

        add(containerPanel);
        cardLayout.show(containerPanel, CARD_HOME);
    }

    private static void initSound() {
        try {
            float sampleRate = 44100;
            int numSamples = (int) (sampleRate * 0.04); // 40ms audio pop
            moveSoundBytes = new byte[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double t = i / sampleRate;
                double freq = 800 - (i * 500.0 / numSamples); // Pitch drop sweep
                double angle = 2.0 * Math.PI * freq * t;
                double envelope = Math.pow(1.0 - ((double) i / numSamples), 2);
                moveSoundBytes[i] = (byte) (Math.sin(angle) * 120 * envelope);
            }
            audioFormat = new AudioFormat(sampleRate, 8, 1, true, true);
        } catch (Exception ignored) {
        }
    }

    private void playMoveSound() {
        if (moveSoundBytes == null) return;
        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat);
                line.start();
                line.write(moveSoundBytes, 0, moveSoundBytes.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private Image createWindowIcon() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background rounded icon card
        g2.setColor(COLOR_CARD);
        g2.fillRoundRect(2, 2, size - 4, size - 4, 18, 18);

        // Outer border
        g2.setColor(COLOR_BUTTON);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(2, 2, size - 4, size - 4, 18, 18);

        // Grid lines
        g2.setColor(COLOR_BUTTON);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(22, 10, 22, 54);
        g2.drawLine(42, 10, 42, 54);
        g2.drawLine(10, 22, 54, 22);
        g2.drawLine(10, 42, 54, 42);

        // X in top-left (Blue)
        g2.setColor(COLOR_X);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(12, 12, 18, 18);
        g2.drawLine(18, 12, 12, 18);

        // O in center (Red)
        g2.setColor(COLOR_O);
        g2.setStroke(new BasicStroke(3.5f));
        g2.drawOval(27, 27, 10, 10);

        // X in bottom-right (Blue)
        g2.setColor(COLOR_X);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(46, 46, 52, 52);
        g2.drawLine(52, 46, 46, 52);

        g2.dispose();
        return img;
    }

    private JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new GridBagLayout());
        homePanel.setBackground(COLOR_BG);
        homePanel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Custom Logo Emblem Header
        homePanel.add(createHomeLogo(), gbc);

        // Title Header
        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 2, 8);
        JLabel titleLabel = new JLabel("TIC TAC TOE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(COLOR_TEXT);
        homePanel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 16, 8);
        JLabel subtitleLabel = new JLabel("Enter Player Names", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(COLOR_MUTED);
        homePanel.add(subtitleLabel, gbc);

        // Player 1 Input Field (Blue)
        gbc.gridy++;
        gbc.insets = new Insets(12, 8, 4, 8);
        JLabel labelX = new JLabel("Player 1:");
        labelX.setFont(new Font("SansSerif", Font.BOLD, 14));
        labelX.setForeground(COLOR_X);
        homePanel.add(labelX, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 12, 8);
        nameXField = new JTextField("Player 1");
        nameXField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        nameXField.setBackground(COLOR_CARD);
        nameXField.setForeground(COLOR_TEXT);
        nameXField.setCaretColor(COLOR_TEXT);
        nameXField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BUTTON, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        homePanel.add(nameXField, gbc);

        // Player 2 Input Field (Red)
        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 4, 8);
        JLabel labelO = new JLabel("Player 2:");
        labelO.setFont(new Font("SansSerif", Font.BOLD, 14));
        labelO.setForeground(COLOR_O);
        homePanel.add(labelO, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 20, 8);
        nameOField = new JTextField("Player 2");
        nameOField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        nameOField.setBackground(COLOR_CARD);
        nameOField.setForeground(COLOR_TEXT);
        nameOField.setCaretColor(COLOR_TEXT);
        nameOField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BUTTON, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        homePanel.add(nameOField, gbc);

        // Start Game Action Button
        gbc.gridy++;
        gbc.insets = new Insets(12, 8, 8, 8);
        JButton startBtn = new JButton("Start Game");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 17));
        startBtn.setPreferredSize(new Dimension(200, 46));
        startBtn.setBackground(COLOR_ACCENT);
        startBtn.setForeground(COLOR_BG);
        startBtn.setFocusPainted(false);
        startBtn.setBorder(BorderFactory.createEmptyBorder());
        startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startBtn.addActionListener(e -> startGame());
        homePanel.add(startBtn, gbc);

        return homePanel;
    }

    private JComponent createHomeLogo() {
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cardSize = 72;
                int cardX = (getWidth() - cardSize) / 2;
                int cardY = (getHeight() - cardSize) / 2;

                // Background card
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(cardX, cardY, cardSize, cardSize, 20, 20);

                // Border accent
                g2.setColor(COLOR_BUTTON);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(cardX, cardY, cardSize, cardSize, 20, 20);

                // Grid lines
                g2.setColor(COLOR_BUTTON);
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(cardX + 25, cardY + 10, cardX + 25, cardY + 62);
                g2.drawLine(cardX + 47, cardY + 10, cardX + 47, cardY + 62);
                g2.drawLine(cardX + 10, cardY + 25, cardX + 62, cardY + 25);
                g2.drawLine(cardX + 10, cardY + 47, cardX + 62, cardY + 47);

                // X symbol top-left
                g2.setColor(COLOR_X);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cardX + 13, cardY + 13, cardX + 21, cardY + 21);
                g2.drawLine(cardX + 21, cardY + 13, cardX + 13, cardY + 21);

                // O symbol center
                g2.setColor(COLOR_O);
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawOval(cardX + 30, cardY + 30, 12, 12);

                // X symbol bottom-right
                g2.setColor(COLOR_X);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cardX + 51, cardY + 51, cardX + 59, cardY + 59);
                g2.drawLine(cardX + 59, cardY + 51, cardX + 51, cardY + 59);

                g2.dispose();
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(80, 80));
        return logoPanel;
    }

    private JPanel createGamePanelWithOverlay() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(480, 580));

        JPanel gamePanel = createGamePanel();
        gamePanel.setBounds(0, 0, 480, 580);

        celebrationOverlay = new CelebrationOverlay();
        celebrationOverlay.setBounds(0, 0, 480, 580);

        layeredPane.add(gamePanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(celebrationOverlay, JLayeredPane.PALETTE_LAYER);

        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                gamePanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                celebrationOverlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(layeredPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout(16, 16));
        gamePanel.setBackground(COLOR_BG);
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header Section (Status Banner & Score Display)
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        headerPanel.setOpaque(false);

        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        statusLabel.setForeground(COLOR_TEXT);

        scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        scoreLabel.setForeground(COLOR_MUTED);

        headerPanel.add(statusLabel);
        headerPanel.add(scoreLabel);
        gamePanel.add(headerPanel, BorderLayout.NORTH);

        // Board Section
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE, 10, 10));
        boardPanel.setBackground(COLOR_CARD);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Font buttonFont = new Font("SansSerif", Font.BOLD, 48);

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                JButton btn = new JButton("");
                btn.setFont(buttonFont);
                btn.setFocusPainted(false);
                btn.setBackground(COLOR_BUTTON);
                btn.setForeground(COLOR_TEXT);
                btn.setBorder(BorderFactory.createEmptyBorder());

                final int row = r;
                final int col = c;
                btn.addActionListener(e -> handleCellClick(row, col));

                buttons[r][c] = btn;
                boardPanel.add(btn);
            }
        }
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        // Footer Section (Home Navigation Control)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);

        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        homeBtn.setPreferredSize(new Dimension(160, 44));
        homeBtn.setBackground(COLOR_BUTTON);
        homeBtn.setForeground(COLOR_TEXT);
        homeBtn.setFocusPainted(false);
        homeBtn.setBorder(BorderFactory.createEmptyBorder());
        homeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeBtn.addActionListener(e -> {
            cancelAutoContinueTimer();
            celebrationOverlay.stopAnimation();
            cardLayout.show(containerPanel, CARD_HOME);
        });

        footerPanel.add(homeBtn);
        gamePanel.add(footerPanel, BorderLayout.SOUTH);

        return gamePanel;
    }

    private void startGame() {
        String xName = nameXField.getText().trim();
        String oName = nameOField.getText().trim();

        playerXName = xName.isEmpty() ? "Player 1" : xName;
        playerOName = oName.isEmpty() ? "Player 2" : oName;

        scoreX = 0;
        scoreO = 0;
        resetGame();
        cardLayout.show(containerPanel, CARD_GAME);
    }

    private void handleCellClick(int row, int col) {
        if (isGameOver || !buttons[row][col].getText().isEmpty()) {
            return;
        }

        // Play synthesized move sound
        playMoveSound();

        String symbol = isXTurn ? "X" : "O";
        Color symbolColor = isXTurn ? COLOR_X : COLOR_O;

        buttons[row][col].setText(symbol);
        buttons[row][col].setForeground(symbolColor);

        if (checkWin(symbol)) {
            isGameOver = true;
            String winner = isXTurn ? playerXName : playerOName;
            String hexColor = isXTurn ? HEX_BLUE : HEX_RED;
            statusLabel.setText("<html><font color='" + hexColor + "'>" + escapeHtml(winner) + "</font> Wins!</html>");

            if (isXTurn) {
                scoreX++;
            } else {
                scoreO++;
            }

            // Trigger Celebration Confetti and Point Animation
            celebrationOverlay.triggerWin(winner, isXTurn);

            // Pulse scoreboard label
            Timer pointTimer = new Timer(300, e -> updateScoreLabel());
            pointTimer.setRepeats(false);
            pointTimer.start();

            // Auto-continue playing after 2 seconds
            scheduleAutoContinue();
        } else if (checkDraw()) {
            isGameOver = true;
            statusLabel.setText("It's a Draw!");

            // Trigger Draw Animation Overlay
            celebrationOverlay.triggerDraw();

            // Auto-continue playing after 2 seconds
            scheduleAutoContinue();
        } else {
            isXTurn = !isXTurn;
            updateStatusLabel();
        }
    }

    private void scheduleAutoContinue() {
        cancelAutoContinueTimer();
        autoContinueTimer = new Timer(2000, e -> resetGame());
        autoContinueTimer.setRepeats(false);
        autoContinueTimer.start();
    }

    private void cancelAutoContinueTimer() {
        if (autoContinueTimer != null && autoContinueTimer.isRunning()) {
            autoContinueTimer.stop();
        }
    }

    private boolean checkWin(String symbol) {
        // Check Rows and Columns
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (buttons[i][0].getText().equals(symbol) &&
                buttons[i][1].getText().equals(symbol) &&
                buttons[i][2].getText().equals(symbol)) {
                highlightWinningCells(i, 0, i, 1, i, 2);
                return true;
            }
            if (buttons[0][i].getText().equals(symbol) &&
                buttons[1][i].getText().equals(symbol) &&
                buttons[2][i].getText().equals(symbol)) {
                highlightWinningCells(0, i, 1, i, 2, i);
                return true;
            }
        }

        // Check Diagonals
        if (buttons[0][0].getText().equals(symbol) &&
            buttons[1][1].getText().equals(symbol) &&
            buttons[2][2].getText().equals(symbol)) {
            highlightWinningCells(0, 0, 1, 1, 2, 2);
            return true;
        }
        if (buttons[0][2].getText().equals(symbol) &&
            buttons[1][1].getText().equals(symbol) &&
            buttons[2][0].getText().equals(symbol)) {
            highlightWinningCells(0, 2, 1, 1, 2, 0);
            return true;
        }

        return false;
    }

    private void highlightWinningCells(int r1, int c1, int r2, int c2, int r3, int c3) {
        Color highlightBg = new Color(88, 91, 112);
        buttons[r1][c1].setBackground(highlightBg);
        buttons[r2][c2].setBackground(highlightBg);
        buttons[r3][c3].setBackground(highlightBg);
    }

    private boolean checkDraw() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (buttons[r][c].getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateStatusLabel() {
        String name = isXTurn ? playerXName : playerOName;
        String color = isXTurn ? HEX_BLUE : HEX_RED;
        statusLabel.setText("<html><font color='" + color + "'>" + escapeHtml(name) + "</font>'s Turn</html>");
    }

    private void updateScoreLabel() {
        scoreLabel.setText("<html><font color='" + HEX_BLUE + "'>" + escapeHtml(playerXName) + "</font>: " + scoreX +
                           " &nbsp;|&nbsp; <font color='" + HEX_RED + "'>" + escapeHtml(playerOName) + "</font>: " + scoreO + "</html>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void resetGame() {
        cancelAutoContinueTimer();
        celebrationOverlay.stopAnimation();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                buttons[r][c].setText("");
                buttons[r][c].setBackground(COLOR_BUTTON);
            }
        }
        isXTurn = true;
        isGameOver = false;
        updateStatusLabel();
        updateScoreLabel();
    }

    // Celebration & Draw Overlay Component for Confetti & Winner/Draw Banner Animation
    private class CelebrationOverlay extends JPanel {
        private final List<Particle> particles = new ArrayList<>();
        private Timer timer;
        private double bannerScale = 0.0;
        private double floatY = 0.0;
        private float alpha = 0.0f;
        private String bannerTitle = "";
        private String bannerSubtitle = "";
        private boolean isXWinner = true;
        private boolean isDrawMode = false;
        private int animFrame = 0;

        public CelebrationOverlay() {
            setOpaque(false);
            setLayout(null);
            timer = new Timer(16, e -> updateAnimation());
        }

        public void triggerWin(String winnerName, boolean xWin) {
            this.bannerTitle = "🎉 WINNER! 🎉";
            this.bannerSubtitle = winnerName.toUpperCase();
            this.isXWinner = xWin;
            this.isDrawMode = false;
            this.bannerScale = 0.0;
            this.floatY = 70.0;
            this.alpha = 0.0f;
            this.animFrame = 0;

            particles.clear();
            Random rng = new Random();
            Color[] colors = {COLOR_X, COLOR_O, COLOR_ACCENT, COLOR_WIN, new Color(203, 166, 247)};

            int width = Math.max(getWidth(), 480);
            int height = Math.max(getHeight(), 580);

            // Spawn Festive Confetti Particles
            for (int i = 0; i < 90; i++) {
                double px = width / 2.0 + (rng.nextDouble() * 120 - 60);
                double py = height / 2.0 + (rng.nextDouble() * 80 - 40);
                double vx = (rng.nextDouble() - 0.5) * 14;
                double vy = -rng.nextDouble() * 12 - 4;
                Color col = colors[rng.nextInt(colors.length)];
                int size = rng.nextInt(8) + 6;
                particles.add(new Particle(px, py, vx, vy, col, size));
            }

            if (!timer.isRunning()) {
                timer.start();
            }
        }

        public void triggerDraw() {
            this.bannerTitle = "🤝 MATCH DRAWN 🤝";
            this.bannerSubtitle = "WELL PLAYED BOTH!";
            this.isDrawMode = true;
            this.bannerScale = 0.0;
            this.floatY = 70.0;
            this.alpha = 0.0f;
            this.animFrame = 0;

            particles.clear();
            Random rng = new Random();
            Color[] colors = {COLOR_MUTED, COLOR_TEXT, COLOR_WIN, COLOR_ACCENT};

            int width = Math.max(getWidth(), 480);
            int height = Math.max(getHeight(), 580);

            // Spawn Draw Sparkles Particles
            for (int i = 0; i < 60; i++) {
                double px = width / 2.0 + (rng.nextDouble() * 120 - 60);
                double py = height / 2.0 + (rng.nextDouble() * 80 - 40);
                double vx = (rng.nextDouble() - 0.5) * 8;
                double vy = -rng.nextDouble() * 8 - 2;
                Color col = colors[rng.nextInt(colors.length)];
                int size = rng.nextInt(6) + 5;
                particles.add(new Particle(px, py, vx, vy, col, size));
            }

            if (!timer.isRunning()) {
                timer.start();
            }
        }

        public void stopAnimation() {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            particles.clear();
            bannerScale = 0.0;
            alpha = 0.0f;
            repaint();
        }

        private void updateAnimation() {
            animFrame++;

            // Banner Scale Ease-Out Cubic
            if (bannerScale < 1.0) {
                bannerScale += 0.09;
                if (bannerScale > 1.0) bannerScale = 1.0;
            }

            if (alpha < 1.0f && animFrame <= 90) {
                alpha += 0.1f;
                if (alpha > 1.0f) alpha = 1.0f;
            }

            // Floating Y position moving upward
            if (floatY > 0) {
                floatY -= 2.5;
                if (floatY < 0) floatY = 0;
            }

            // Smooth Fade out near end of 2-second celebration (~1.6s to 2.0s)
            if (animFrame > 95) {
                alpha -= 0.08f;
                if (alpha < 0.0f) alpha = 0.0f;
            }

            // Update Particle Physics
            for (Particle p : particles) {
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.35; // Gravity
                p.rotation += p.rotSpeed;
            }

            // Remove off-screen particles
            particles.removeIf(p -> p.y > getHeight() + 50);

            if (animFrame > 120) {
                timer.stop();
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (alpha <= 0.001f && bannerScale <= 0.001) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Render Confetti / Sparkles
            for (Particle p : particles) {
                g2.setColor(new Color(
                    p.color.getRed(),
                    p.color.getGreen(),
                    p.color.getBlue(),
                    (int) (255 * alpha)
                ));
                AffineTransform transform = g2.getTransform();
                g2.translate(p.x, p.y);
                g2.rotate(p.rotation);
                g2.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
                g2.setTransform(transform);
            }

            // Render Animated Card
            int w = getWidth();
            int h = getHeight();

            int cardW = (int) (340 * bannerScale);
            int cardH = (int) (110 * bannerScale);
            int cardX = (w - cardW) / 2;
            int cardY = (h - cardH) / 2 - 20;

            if (cardW > 10 && cardH > 10 && alpha > 0) {
                // Drop shadow
                g2.setColor(new Color(0, 0, 0, (int) (140 * alpha)));
                g2.fillRoundRect(cardX - 4, cardY + 6, cardW + 8, cardH, 24, 24);

                // Card background
                g2.setColor(new Color(
                    COLOR_CARD.getRed(),
                    COLOR_CARD.getGreen(),
                    COLOR_CARD.getBlue(),
                    (int) (255 * alpha)
                ));
                g2.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);

                // Border accent
                Color borderCol = isDrawMode ? COLOR_WIN : (isXWinner ? COLOR_X : COLOR_O);
                g2.setColor(new Color(
                    borderCol.getRed(),
                    borderCol.getGreen(),
                    borderCol.getBlue(),
                    (int) (255 * alpha)
                ));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(cardX, cardY, cardW, cardH, 24, 24);

                // Text
                if (bannerScale > 0.6) {
                    g2.setFont(new Font("SansSerif", Font.BOLD, (int) (22 * bannerScale)));
                    g2.setColor(new Color(
                        COLOR_WIN.getRed(),
                        COLOR_WIN.getGreen(),
                        COLOR_WIN.getBlue(),
                        (int) (255 * alpha)
                    ));
                    FontMetrics fm1 = g2.getFontMetrics();
                    g2.drawString(bannerTitle, cardX + (cardW - fm1.stringWidth(bannerTitle)) / 2, cardY + (int) (40 * bannerScale));

                    g2.setFont(new Font("SansSerif", Font.BOLD, (int) (18 * bannerScale)));
                    g2.setColor(new Color(
                        borderCol.getRed(),
                        borderCol.getGreen(),
                        borderCol.getBlue(),
                        (int) (255 * alpha)
                    ));
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(bannerSubtitle, cardX + (cardW - fm2.stringWidth(bannerSubtitle)) / 2, cardY + (int) (75 * bannerScale));
                }
            }

            // Animated Floating text flying upward
            if (floatY > 0 && alpha > 0) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 26));
                g2.setColor(new Color(
                    COLOR_ACCENT.getRed(),
                    COLOR_ACCENT.getGreen(),
                    COLOR_ACCENT.getBlue(),
                    (int) (255 * alpha)
                ));
                String floatText = isDrawMode ? "No Winner!" : "+1 Point!";
                FontMetrics fm = g2.getFontMetrics();
                int px = (w - fm.stringWidth(floatText)) / 2;
                int py = cardY - (int) floatY;
                g2.drawString(floatText, px, py);
            }

            g2.dispose();
        }
    }

    private static class Particle {
        double x, y, vx, vy, rotation, rotSpeed;
        Color color;
        int size;

        Particle(double x, double y, double vx, double vy, Color color, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.rotation = Math.random() * Math.PI * 2;
            this.rotSpeed = (Math.random() - 0.5) * 0.2;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TicTacToe frame = new TicTacToe();
            frame.setVisible(true);
        });
    }
}
