import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Ein vollständiges Schachspiel in einer einzigen Datei.
 * Enthält: GUI, Logik, KI, und Analyse.
 */
public class ChessGame extends JFrame {

    private Board board;
    private BoardPanel boardPanel;
    private Engine engine;
    private boolean isPlayerTurn;
    private List<Board> history;
    private JLabel statusLabel;
    private JTextArea debugArea;
    private int aiDepth = 2; // Standard Schwierigkeit
    private JPanel mainContainer;
    private CardLayout cardLayout;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ChessGame().setVisible(true);
        });
    }

    public ChessGame() {
        setTitle("Java Schach - Hauptmenü");
        setSize(1000, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Engine Logger verbinden
        Engine.LOGGER = this::log;

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Panels erstellen
        JPanel menuPanel = createMainMenuPanel();
        JPanel gamePanel = createGamePanel();

        mainContainer.add(menuPanel, "MENU");
        mainContainer.add(gamePanel, "GAME");

        add(mainContainer);
        
        // Start im Menü
        cardLayout.show(mainContainer, "MENU");
    }

    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(60, 60, 60));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("JAVA SCHACH", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        panel.add(title, gbc);

        JLabel subTitle = new JLabel("Wähle eine Schwierigkeit:", SwingConstants.CENTER);
        subTitle.setForeground(Color.LIGHT_GRAY);
        subTitle.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridy = 1;
        panel.add(subTitle, gbc);

        // Buttons
        gbc.gridy = 2; panel.add(createDifficultyButton("Leicht (Tiefe 1)", 1), gbc);
        gbc.gridy = 3; panel.add(createDifficultyButton("Mittel (Tiefe 2)", 2), gbc);
        gbc.gridy = 4; panel.add(createDifficultyButton("Schwer (Tiefe 3)", 3), gbc);
        // Tiefe 4 ist in Java ohne Transposition Table oft zu langsam für GUI, lassen wir es bei 3 als Max für "Schwer"
        // Oder wir optimieren es später. Für jetzt sicherheitshalber 3.

        return panel;
    }

    private JButton createDifficultyButton(String text, int depth) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> startGame(depth));
        return btn;
    }

    private void startGame(int depth) {
        this.aiDepth = depth;
        restartGame(); // Reset board
        cardLayout.show(mainContainer, "GAME");
        setTitle("Java Schach - Spiel läuft (Tiefe " + depth + ")");
    }
    
    private void showMenu() {
        cardLayout.show(mainContainer, "MENU");
        setTitle("Java Schach - Hauptmenü");
    }

    private JPanel createGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout());

        board = new Board(); // Initialisierung
        board.setupStandardBoard(); // Setup
        history = new ArrayList<>();
        history.add(board.copy());
        
        engine = new Engine();
        isPlayerTurn = true; 

        boardPanel = new BoardPanel();
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        // Status Panel + Debug Area
        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Weiß am Zug (Du)");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        
        debugArea = new JTextArea(5, 40);
        debugArea.setEditable(false);
        JScrollPane debugScroll = new JScrollPane(debugArea);
        bottomPanel.add(debugScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton evalButton = new JButton("Lage bewerten");
        evalButton.addActionListener(e -> evaluateCurrentPosition());
        buttonPanel.add(evalButton);

        JButton menuButton = new JButton("Hauptmenü");
        menuButton.addActionListener(e -> showMenu());
        buttonPanel.add(menuButton);

        JButton restartButton = new JButton("Neues Spiel");
        restartButton.addActionListener(e -> restartGame());
        buttonPanel.add(restartButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        gamePanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return gamePanel;
    }
    
    private void evaluateCurrentPosition() {
        int score = engine.evaluate(board);
        int scoreWhite = board.whiteToMove ? score : -score;
        String msg = "Aktuelle Bewertung (Weiß-Perspektive): " + scoreWhite + "\n" +
                     (scoreWhite > 50 ? "Weiß steht besser." : scoreWhite < -50 ? "Schwarz steht besser." : "Ausgeglichen.");
        JOptionPane.showMessageDialog(this, msg, "Bewertung", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void log(String msg) {
        System.out.println(msg);
        SwingUtilities.invokeLater(() -> {
            debugArea.append(msg + "\n");
            debugArea.setCaretPosition(debugArea.getDocument().getLength());
        });
    }

    private void restartGame() {
        board = new Board();
        board.setupStandardBoard();
        history.clear();
        history.add(board.copy());
        isPlayerTurn = true;
        boardPanel.selectedSquare = -1;
        boardPanel.validMovesSquares.clear();
        statusLabel.setText("Weiß am Zug (Du)");
        log("Spiel neu gestartet.");
        boardPanel.repaint();
    }

    private void checkGameOver() {
        List<Move> legalMoves = board.generateLegalMoves();
        if (legalMoves.isEmpty()) {
            if (board.isCheck(board.whiteToMove)) {
                String winner = board.whiteToMove ? "Schwarz (Bot)" : "Weiß (Spieler)";
                statusLabel.setText("Schachmatt! " + winner + " gewinnt.");
                showGameOverDialog("Schachmatt! " + winner + " gewinnt.");
            } else {
                statusLabel.setText("Patt! Unentschieden.");
                showGameOverDialog("Patt! Unentschieden.");
            }
        }
    }

    private void startBotMove() {
        statusLabel.setText("Bot überlegt...");
        log("Bot startet Nachdenken...");
        Board boardCopy = board.copy();
        
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                Move bestMove = engine.getBestMove(boardCopy, aiDepth);
                long duration = System.currentTimeMillis() - start;
                log("Bot fertig in " + duration + "ms. Move: " + (bestMove != null ? bestMove.from + "->" + bestMove.to : "null"));
                
                SwingUtilities.invokeLater(() -> {
                    if (bestMove != null) {
                        try {
                            board.makeMove(bestMove);
                            history.add(board.copy());
                            isPlayerTurn = true;
                            statusLabel.setText("Weiß am Zug (Du) - Bot: " + duration + "ms");
                            boardPanel.repaint();
                            // Erst prüfen, dann loggen
                            checkGameOver();
                            log("Bot Zug ausgeführt.");
                        } catch (Exception ex) {
                            log("Fehler beim Ausführen des Bot-Zugs: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    } else {
                        log("Bot hat keinen Zug gefunden (Patt/Matt?).");
                        checkGameOver();
                    }
                });
            } catch (Exception e) {
                log("Fehler im Bot Thread: " + e.toString());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> statusLabel.setText("Fehler: " + e.getMessage()));
            }
        }).start();
    }
    
    private void showGameOverDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Spielende", JOptionPane.INFORMATION_MESSAGE);
        analyzeGame();
    }

    private void analyzeGame() {
        StringBuilder analysis = new StringBuilder("--- Partie-Analyse ---\n\n");
        Engine analyzer = new Engine();
        
        for (int i = 0; i < history.size(); i++) {
            Board b = history.get(i);
            // Hole Score aus weißer Perspektive
            int rawScore = analyzer.evaluate(b);
            int scoreWhitePersp = b.whiteToMove ? rawScore : -rawScore;
            
            String manualEval = "";
            if (scoreWhitePersp > 9000) manualEval = "Weiß gewinnt";
            else if (scoreWhitePersp < -9000) manualEval = "Schwarz gewinnt";
            else if (scoreWhitePersp > 200) manualEval = "Weiß hat Vorteil";
            else if (scoreWhitePersp < -200) manualEval = "Schwarz hat Vorteil";
            else manualEval = "Ausgeglichen";

            analysis.append(String.format("Zug %d: %s (Eval: %d)\n", i, manualEval, scoreWhitePersp));
            
            // Fehlererkennung
            if (i > 0) {
                Board prevB = history.get(i-1);
                int prevRaw = analyzer.evaluate(prevB);
                int prevScoreWhite = prevB.whiteToMove ? prevRaw : -prevRaw;
                
                int diff = scoreWhitePersp - prevScoreWhite;
                // Wer hat gerade gezogen?
                // State i ist das Ergebnis des Zuges von State i-1.
                // State i-1: Wer war am Zug?
                boolean whiteJustMoved = prevB.whiteToMove;
                
                if (whiteJustMoved) {
                    if (diff < -200) analysis.append("  -> Ungenauer Zug von Weiß\n");
                    if (diff < -500) analysis.append("  -> FEHLER von Weiß\n");
                } else {
                    if (diff > 200) analysis.append("  -> Ungenauer Zug von Schwarz\n");
                    if (diff > 500) analysis.append("  -> FEHLER von Schwarz\n");
                }
            }
        }
        
        analysis.append("\nZusammenfassung: Das Spiel ist beendet.");
        
        JTextArea textArea = new JTextArea(analysis.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "Analyse", JOptionPane.INFORMATION_MESSAGE);
    }

    private class BoardPanel extends JPanel {
        private int offsetX = 0;
        private int offsetY = 0;
        private int selectedSquare = -1;
        private List<Integer> validMovesSquares = new ArrayList<>();

        public BoardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isPlayerTurn) return; 

                    int size = getTileSize();
                    int col = (e.getX() - offsetX) / size;
                    int row = (e.getY() - offsetY) / size;
                    int index = row * 8 + col;

                    if (col < 0 || col > 7 || row < 0 || row > 7) return;

                    handleClick(index);
                }
            });
        }

        private int getTileSize() {
             // Quadratisches Brett, das in den Bereich passt
             int s = Math.min(getWidth(), getHeight());
             return s / 8;
        }

        // ... handleClick and doPlayerMove remain same ...
        private void handleClick(int index) {
            // Wenn bereits gewählt und Ziel valide ist -> Ziehen
            if (selectedSquare != -1 && validMovesSquares.contains(index)) {
                // Finde den passenden Move
                List<Move> moves = board.generateLegalMoves();
                for (Move m : moves) {
                    if (m.from == selectedSquare && m.to == index) {
                        doPlayerMove(m);
                        return;
                    }
                }
            }

            // Ansonsten Auswahl ändern
            if (board.pieces[index] != Piece.EMPTY && Piece.isWhite(board.pieces[index])) {
                selectedSquare = index;
                validMovesSquares = board.generateLegalMoves().stream()
                        .filter(m -> m.from == index)
                        .map(m -> m.to)
                        .collect(Collectors.toList());
            } else {
                selectedSquare = -1;
                validMovesSquares.clear();
            }
            repaint();
        }

        private void doPlayerMove(Move move) {
            // Promotion check
            if (Piece.getType(board.pieces[move.from]) == PieceType.PAWN) {
                int targetRow = move.to / 8;
                if (targetRow == 0) { // Weiß promoviert auf Reihe 0
                     String[] options = {"Dame", "Turm", "Läufer", "Springer"};
                     int choice = JOptionPane.showOptionDialog(this, "Wähle eine Figur:", "Bauernumwandlung",
                             JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                     switch(choice) {
                         case 1: move.promotion = Piece.W_ROOK; break;
                         case 2: move.promotion = Piece.W_BISHOP; break;
                         case 3: move.promotion = Piece.W_KNIGHT; break;
                         default: move.promotion = Piece.W_QUEEN; break;
                     }
                }
            }

            board.makeMove(move);
            selectedSquare = -1;
            validMovesSquares.clear();
            isPlayerTurn = false;
            repaint();
            
            // History update für Spielerzug
            history.add(board.copy());
            checkGameOver();
            
            // Wenn Spiel nicht zu Ende ist, Bot aktivieren
            if (!board.generateLegalMoves().isEmpty()) {
                 startBotMove();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            int tileSize = getTileSize();
            int boardPixelSize = tileSize * 8;
            
            offsetX = (getWidth() - boardPixelSize) / 2;
            offsetY = (getHeight() - boardPixelSize) / 2;
            
            // Hintergrund (Randbereich)
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int index = row * 8 + col;
                    boolean isLight = (row + col) % 2 == 0;
                    
                    int x = offsetX + col * tileSize;
                    int y = offsetY + row * tileSize;
                    
                    // Brett zeichnen
                    if (isLight) g.setColor(new Color(240, 217, 181));
                    else g.setColor(new Color(181, 136, 99));
                    g.fillRect(x, y, tileSize, tileSize);

                    // Highlights
                    if (index == selectedSquare) {
                        g.setColor(new Color(100, 255, 100, 128));
                        g.fillRect(x, y, tileSize, tileSize);
                    } else if (validMovesSquares.contains(index)) {
                        g.setColor(new Color(100, 200, 255, 128));
                        g.fillOval(x + tileSize/3, y + tileSize/3, tileSize/3, tileSize/3);
                    }
                    else if (board.lastMoveTo == index || board.lastMoveFrom == index) {
                        g.setColor(new Color(255, 255, 0, 80));
                        g.fillRect(x, y, tileSize, tileSize);
                    }

                    // Figur zeichnen
                    byte piece = board.pieces[index];
                    if (piece != Piece.EMPTY) {
                        drawPiece(g, piece, x, y, tileSize);
                    }
                }
            }
        }


        private void drawPiece(Graphics g, byte piece, int x, int y, int size) {
            String symbol = "";
            // Unicode Figuren
            switch(piece) {
                case Piece.W_KING: symbol = "♔"; break;
                case Piece.W_QUEEN: symbol = "♕"; break;
                case Piece.W_ROOK: symbol = "♖"; break;
                case Piece.W_BISHOP: symbol = "♗"; break;
                case Piece.W_KNIGHT: symbol = "♘"; break;
                case Piece.W_PAWN: symbol = "♙"; break;
                case Piece.B_KING: symbol = "♚"; break;
                case Piece.B_QUEEN: symbol = "♛"; break;
                case Piece.B_ROOK: symbol = "♜"; break;
                case Piece.B_BISHOP: symbol = "♝"; break;
                case Piece.B_KNIGHT: symbol = "♞"; break;
                case Piece.B_PAWN: symbol = "♟"; break;
            }
            
            g.setFont(new Font("SansSerif", Font.PLAIN, (int)(size * 0.85)));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(symbol);
            int h = fm.getAscent();
            
            // Outline/Color Logic: Im Standard Unicode sind schwarze Figuren gefüllt, weiße hohl.
            // Um bessere Sichtbarkeit zu haben machen wir Standard Textfarbe:
            g.setColor(Color.BLACK); 
            // Korrektur: In Gui sind Unicode chars manchmal tricky.
            // Wir zeichnen sie einfach als Text.
            
            int textX = x + (size - w) / 2;
            int textY = y + (size + h) / 2 - fm.getDescent();
            g.drawString(symbol, textX, textY);
        }
    }
}

// ---------------------------
// LOGIK KLASSEN
// ---------------------------

class Engine {
    public static java.util.function.Consumer<String> LOGGER = s -> System.out.println(s);

    private static final int INF = 100000000;
    
    // Einfache Positionstabelle für Bauern (zentraler is besser)
    private static final int[] PAWN_TABLE = {
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    };
    
    private static final int[] KNIGHT_TABLE = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    public Move getBestMove(Board board, int depth) {
        Move bestMove = null;
        int bestValue = -INF;
        int alpha = -INF;
        int beta = INF;
        
        List<Move> moves = board.generateLegalMoves();
        LOGGER.accept("Bot Analysis: Found " + moves.size() + " legal moves.");
        
        moves.sort((m1, m2) -> {
            int score1 = (board.pieces[m1.to] != Piece.EMPTY) ? 10 : 0;
            int score2 = (board.pieces[m2.to] != Piece.EMPTY) ? 10 : 0;
            return score2 - score1;
        });

        for (Move move : moves) {
            board.makeMove(move);
            int value = -minimax(board, depth - 1, -beta, -alpha);
            board.undoMove(move);
            
            // LOGGER.accept("Move " + move.from + "->" + move.to + " Score: " + value);

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
                LOGGER.accept("New/Best Move: " + move.from + "->" + move.to + " val=" + value);
            }
            alpha = Math.max(alpha, value);
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta) {
        if (depth == 0) {
            return evaluate(board);
        }

        List<Move> moves = board.generateLegalMoves();
        if (moves.isEmpty()) {
            if (board.isCheck(board.whiteToMove)) return -INF + 100; // Matt
            return 0; // Patt
        }

        for (Move move : moves) {
            board.makeMove(move);
            int value = -minimax(board, depth - 1, -beta, -alpha);
            board.undoMove(move);
            
            if (value >= beta) return beta;
            alpha = Math.max(alpha, value);
        }
        return alpha;
    }

    public int evaluate(Board board) {
        int score = 0;
        for (int i = 0; i < 64; i++) {
            byte p = board.pieces[i];
            if (p == Piece.EMPTY) continue;
            
            int val = getPieceValue(p);
            int pst = getPstValue(p, i);
            
            if (Piece.isWhite(p)) {
                score += (val + pst);
            } else {
                score -= (val + pst);
            }
        }
        // Perspektive des aktiven Spielers: Wenn Weiß am Zug, ist positiver Score gut.
        // Wenn Schwarz am Zug, ist negativer Score gut (aber Minimax dreht das Vorzeichen).
        // Hier geben wir "Weiß-Vorteil" zurück.
        return board.whiteToMove ? score : -score;
    }

    private int getPieceValue(byte p) {
        switch (Piece.getType(p)) {
            case PieceType.PAWN: return 100;
            case PieceType.KNIGHT: return 320;
            case PieceType.BISHOP: return 330;
            case PieceType.ROOK: return 500;
            case PieceType.QUEEN: return 900;
            case PieceType.KING: return 20000;
            default: return 0;
        }
    }
    
    private int getPstValue(byte p, int index) {
        // Einfache Mapping für Bauern und Springer, Rest 0
        int row = index / 8;
        int col = index % 8;
        // PST Tabellen sind aus weißer Sicht definiert (unten ist Start).
        // Für Schwarz müssen wir spiegeln.
        boolean isWhite = Piece.isWhite(p);
        int tableRow = isWhite ? row : 7 - row;
        int tableIdx = tableRow * 8 + col;
        
        switch (Piece.getType(p)) {
            case PieceType.PAWN: return PAWN_TABLE[tableIdx];
            case PieceType.KNIGHT: return KNIGHT_TABLE[tableIdx];
            default: return 0;
        }
    }
}

class Move {
    int from;
    int to;
    byte capturedPiece;
    byte promotion = Piece.EMPTY;
    boolean isCastle;
    boolean isEnPassant;
    
    // Für Undo State
    boolean prevWhiteToMove;
    int prevEnPassantCol;
    boolean prevCastleWK, prevCastleWQ, prevCastleBK, prevCastleBQ;

    public Move(int from, int to) {
        this.from = from;
        this.to = to;
    }
}

class Piece {
    public static final byte EMPTY = 0;
    
    public static final byte W_PAWN = 1;
    public static final byte W_KNIGHT = 2;
    public static final byte W_BISHOP = 3;
    public static final byte W_ROOK = 4;
    public static final byte W_QUEEN = 5;
    public static final byte W_KING = 6;
    
    public static final byte B_PAWN = 9; // Bit 4 set -> Schwarz (8 + type)
    public static final byte B_KNIGHT = 10;
    public static final byte B_BISHOP = 11;
    public static final byte B_ROOK = 12;
    public static final byte B_QUEEN = 13;
    public static final byte B_KING = 14;

    public static boolean isWhite(byte p) {
        return p > 0 && p < 8; // 1-6
    }
    
    public static boolean isBlack(byte p) {
        return p >= 8;
    }

    public static byte getType(byte p) {
        return (byte) (p & 7); // 7 = 00000111 mask
    }
}

class PieceType {
    public static final byte PAWN = 1;
    public static final byte KNIGHT = 2;
    public static final byte BISHOP = 3;
    public static final byte ROOK = 4;
    public static final byte QUEEN = 5;
    public static final byte KING = 6;
}

class Board {
    public byte[] pieces = new byte[64];
    public boolean whiteToMove = true;
    
    // Rochade Rechte
    public boolean castleWK = true; // White King-side
    public boolean castleWQ = true; // White Queen-side
    public boolean castleBK = true;
    public boolean castleBQ = true;
    
    public int enPassantCol = -1; // Spalte, wenn Bauer gerade 2 Felder gezogen
    
    // Letzter Zug zum Highlighten
    public int lastMoveFrom = -1;
    public int lastMoveTo = -1;

    public void setupStandardBoard() {
        // Schwarz
        pieces[0] = Piece.B_ROOK; pieces[1] = Piece.B_KNIGHT; pieces[2] = Piece.B_BISHOP; pieces[3] = Piece.B_QUEEN;
        pieces[4] = Piece.B_KING; pieces[5] = Piece.B_BISHOP; pieces[6] = Piece.B_KNIGHT; pieces[7] = Piece.B_ROOK;
        for (int i = 8; i < 16; i++) pieces[i] = Piece.B_PAWN;
        
        // Leer
        for (int i = 16; i < 48; i++) pieces[i] = Piece.EMPTY;
        
        // Weiß
        for (int i = 48; i < 56; i++) pieces[i] = Piece.W_PAWN;
        pieces[56] = Piece.W_ROOK; pieces[57] = Piece.W_KNIGHT; pieces[58] = Piece.W_BISHOP; pieces[59] = Piece.W_QUEEN;
        pieces[60] = Piece.W_KING; pieces[61] = Piece.W_BISHOP; pieces[62] = Piece.W_KNIGHT; pieces[63] = Piece.W_ROOK;
        
        whiteToMove = true;
        castleWK = true; castleWQ = true; castleBK = true; castleBQ = true;
        enPassantCol = -1;
    }
    
    public Board copy() {
        Board b = new Board();
        System.arraycopy(this.pieces, 0, b.pieces, 0, 64);
        b.whiteToMove = this.whiteToMove;
        b.castleWK = this.castleWK;
        b.castleWQ = this.castleWQ;
        b.castleBK = this.castleBK;
        b.castleBQ = this.castleBQ;
        b.enPassantCol = this.enPassantCol;
        return b;
    }

    public void makeMove(Move m) {
        // Status speichern
        m.capturedPiece = pieces[m.to];
        m.prevWhiteToMove = whiteToMove;
        m.prevEnPassantCol = enPassantCol;
        m.prevCastleWK = castleWK;
        m.prevCastleWQ = castleWQ;
        m.prevCastleBK = castleBK;
        m.prevCastleBQ = castleBQ;

        byte movingPiece = pieces[m.from];
        pieces[m.from] = Piece.EMPTY;
        pieces[m.to] = movingPiece;
        
        // Promotion
        if (m.promotion != Piece.EMPTY) {
            pieces[m.to] = m.promotion;
        }

        // Castle Move Logik (Turm bewegen)
        if (m.isCastle) {
            if (m.to == 62) { // Weiß King Side
                pieces[61] = pieces[63]; pieces[63] = Piece.EMPTY;
            } else if (m.to == 58) { // Weiß Queen Side
                pieces[59] = pieces[56]; pieces[56] = Piece.EMPTY;
            } else if (m.to == 6) { // Schwarz King Side
                pieces[5] = pieces[7]; pieces[7] = Piece.EMPTY;
            } else if (m.to == 2) { // Schwarz Queen Side
                pieces[3] = pieces[0]; pieces[0] = Piece.EMPTY;
            }
        }
        
        // En Passant Capture
        if (m.isEnPassant) {
            // Der geschlagene Bauer steht eine Reihe "hinter" bzw "vor" (je nach Sicht) dem Ziel
            // Wenn Weiß zieht (nach oben), steht der schwarze Bauer bei to + 8
            // Wenn Schwarz zieht (nach unten), steht der weiße Bauer bei to - 8
            int capturePos = whiteToMove ? m.to + 8 : m.to - 8;
            m.capturedPiece = pieces[capturePos]; // Speichern für undo!
            pieces[capturePos] = Piece.EMPTY; 
        }

        // En Passant Status aktualisieren
        enPassantCol = -1;
        if (Piece.getType(movingPiece) == PieceType.PAWN && Math.abs(m.from - m.to) == 16) {
            enPassantCol = m.from % 8;
        }

        // Rochade Rechte entfernen
        updateCastlingRights(movingPiece, m.from); // Wenn König oder Turm zieht
        // Rechte entziehen bei Turm Capture ist komplexer, hier vereinfacht:
        // Ideal: Wenn ein Turm auf seiner Startpos geschlagen wird, Right löschen.
        if (m.to == 0) castleBQ = false;
        if (m.to == 7) castleBK = false;
        if (m.to == 56) castleWQ = false;
        if (m.to == 63) castleWK = false;

        whiteToMove = !whiteToMove;
        lastMoveFrom = m.from;
        lastMoveTo = m.to;
    }

    public void undoMove(Move m) {
        whiteToMove = m.prevWhiteToMove;
        enPassantCol = m.prevEnPassantCol;
        castleWK = m.prevCastleWK;
        castleWQ = m.prevCastleWQ;
        castleBK = m.prevCastleBK;
        castleBQ = m.prevCastleBQ;
        
        byte movedPiece = pieces[m.to];
        if (m.promotion != Piece.EMPTY) {
            // Wenn Promotion war, dann ist movedPiece jetzt die Dame/Turm etc.
            // Wir müssen es zurück zum Bauern machen.
            movedPiece = whiteToMove ? Piece.W_PAWN : Piece.B_PAWN;
        }
        
        pieces[m.from] = movedPiece;
        pieces[m.to] = m.capturedPiece; // Normaler Capture Restore (bei En Passant ist das Empty, bei normal das Stück)
        
        if (m.isEnPassant) {
            pieces[m.to] = Piece.EMPTY; // Das Zielfeld war leer bei EP
            int capturePos = whiteToMove ? m.to + 8 : m.to - 8;
            pieces[capturePos] = m.capturedPiece; // Der Bauer wird hier wiederhergestellt
        }
        
        if (m.isCastle) {
             if (m.to == 62) { pieces[63] = pieces[61]; pieces[61] = Piece.EMPTY; }
             else if (m.to == 58) { pieces[56] = pieces[59]; pieces[59] = Piece.EMPTY; }
             else if (m.to == 6) { pieces[7] = pieces[5]; pieces[5] = Piece.EMPTY; }
             else if (m.to == 2) { pieces[0] = pieces[3]; pieces[3] = Piece.EMPTY; }
        }
    }

    private void updateCastlingRights(byte p, int from) {
        if (p == Piece.W_KING) { castleWK = false; castleWQ = false; }
        else if (p == Piece.B_KING) { castleBK = false; castleBQ = false; }
        else if (p == Piece.W_ROOK) {
            if (from == 63) castleWK = false;
            if (from == 56) castleWQ = false;
        }
        else if (p == Piece.B_ROOK) {
            if (from == 7) castleBK = false;
            if (from == 0) castleBQ = false;
        }
    }

    public List<Move> generateLegalMoves() {
        List<Move> pseudoMoves = generatePseudoLegalMoves();
        List<Move> legalMoves = new ArrayList<>();
        
        for (Move m : pseudoMoves) {
            makeMove(m);
            if (!isCheck(!whiteToMove)) { // Prüfen ob der Spieler, der gerade gezogen hat (!whiteToMove), im Schach ist
                legalMoves.add(m);
            }
            undoMove(m);
        }
        return legalMoves;
    }
    
    // Prüft ob 'colorWhite' im Schach steht.
    public boolean isCheck(boolean colorWhite) {
        int kingPos = -1;
        byte kingType = colorWhite ? Piece.W_KING : Piece.B_KING;
        for (int i = 0; i < 64; i++) {
            if (pieces[i] == kingType) {
                kingPos = i;
                break;
            }
        }
        if (kingPos == -1) return true; // Sollte nicht passieren außer König fehlt

        // Gegnerische Züge simulieren, um zu sehen ob sie König schlagen können?
        // Effizienter: Von Königsposition aus schauen ob er angegriffen wird.
        return isSquareAttacked(kingPos, !colorWhite);
    }

    private boolean isSquareAttacked(int square, boolean byWhite) {
        // Bauerngriffe
        int dir = byWhite ? -1 : 1; // Weiße Bauern greifen nach oben (-), schwarze nach unten (+)
        // Achtung: Hier "byWhite" bedeutet "greift Weiß an?". Weiß greift von unten nach oben an (Index wird kleiner).
        // Wenn ich Checke ob Feld X von Weiß angegriffen wird, muss bei X + 7 oder X + 9 ein Weißer Bauer stehen (der nach oben schlägt).
        // Umgekehrt: Wenn ich Checke ob Feld X von Schwarz angegriffen wird (byWhite=false), muss bei X - 7 oder X - 9 ein Schwarzer Bauer stehen.
        
        if (byWhite) { // Angeriffen von Weiß?
             if (isValidPos(square + 7) && square % 8 != 0 && pieces[square + 7] == Piece.W_PAWN) return true;
             if (isValidPos(square + 9) && square % 8 != 7 && pieces[square + 9] == Piece.W_PAWN) return true;
        } else { // Angegriffen von Schwarz?
             if (isValidPos(square - 7) && square % 8 != 7 && pieces[square - 7] == Piece.B_PAWN) return true;
             if (isValidPos(square - 9) && square % 8 != 0 && pieces[square - 9] == Piece.B_PAWN) return true;
        }

        // Springer
        int[] knightOffsets = {-17, -15, -10, -6, 6, 10, 15, 17};
        for (int off : knightOffsets) {
            int target = square + off;
            if (isValidPos(target) && isKnightJumpValid(square, target)) {
                byte p = pieces[target];
                if (p != Piece.EMPTY && Piece.isWhite(p) == byWhite && Piece.getType(p) == PieceType.KNIGHT) return true;
            }
        }
        
        // Sliding Pieces (Rook, Bishop, Queen, King as distance 1)
        int[] dirs = {-9, -8, -7, -1, 1, 7, 8, 9};
        for (int d : dirs) {
            for (int dist = 1; dist < 8; dist++) {
                int target = square + d * dist;
                if (!isValidPos(target) || !isSlideValid(square, target, d)) break;
                
                byte p = pieces[target];
                if (p != Piece.EMPTY) {
                   if (Piece.isWhite(p) == byWhite) {
                        byte t = Piece.getType(p);
                        boolean dia = (d == -9 || d == -7 || d == 7 || d == 9);
                        boolean str = (d == -8 || d == -1 || d == 1 || d == 8);
                        
                        if (t == PieceType.QUEEN) return true;
                        if (dia && t == PieceType.BISHOP) return true;
                        if (str && t == PieceType.ROOK) return true;
                        if (dist == 1 && t == PieceType.KING) return true;
                   }
                   break; // Blocked by piece
                }
            }
        }
        return false;
    }
    
    private boolean isSlideValid(int from, int to, int step) {
        int fCol = from % 8;
        int tCol = to % 8;
        int fRow = from / 8;
        int tRow = to / 8;

        // Vertikal (step +/- 8): Spalte muss gleich bleiben
        if (Math.abs(step) == 8) {
            return fCol == tCol;
        }

        // Horizontal (step +/- 1): Reihe muss gleich bleiben
        if (Math.abs(step) == 1) {
            return fRow == tRow;
        }
        
        // Diagonal (step +/- 7 oder +/- 9)
        // Zeilendifferenz muss gleich Spaltendifferenz sein
        int rowDiff = Math.abs(fRow - tRow);
        int colDiff = Math.abs(fCol - tCol);
        return rowDiff == colDiff;
    }

    private boolean isKnightJumpValid(int from, int to) {
        int colDiff = Math.abs((from % 8) - (to % 8));
        return colDiff <= 2; // Verhindert wrap around (H -> A Sprünge)
    }

    private boolean isValidPos(int i) { return i >= 0 && i < 64; }

    public List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (pieces[i] != Piece.EMPTY && Piece.isWhite(pieces[i]) == whiteToMove) {
                generateMovesForPiece(i, moves);
            }
        }
        return moves;
    }

    private void generateMovesForPiece(int idx, List<Move> moves) {
        byte p = pieces[idx];
        byte t = Piece.getType(p);
        int row = idx / 8;
        int col = idx % 8;
        
        // Pawn
        if (t == PieceType.PAWN) {
            int dir = whiteToMove ? -8 : 8;
            int startRow = whiteToMove ? 6 : 1;
            int forward = idx + dir;
            
            // Move Forward 1
            if (isValidPos(forward) && pieces[forward] == Piece.EMPTY) {
                // Promotion?
                if ((whiteToMove && forward < 8) || (!whiteToMove && forward >= 56)) {
                    // Promotion Moves hinzufügen (Auto-Queen hier im Generator, eigentlich 4 Moves)
                    // Wir fügen hier einfach einen markierten Move hinzu, GUI/Logic handled Piecewahl
                     Move m = new Move(idx, forward);
                     m.promotion = whiteToMove ? Piece.W_QUEEN : Piece.B_QUEEN; // Bot default
                     moves.add(m);
                } else {
                    moves.add(new Move(idx, forward));
                }
                
                // Move Forward 2
                int forward2 = idx + dir * 2;
                if (row == startRow && isValidPos(forward2) && pieces[forward2] == Piece.EMPTY) {
                    moves.add(new Move(idx, forward2));
                }
            }
            // Captures
            int[] caps = {whiteToMove ? -9 : 7, whiteToMove ? -7 : 9}; // Diagonal left/right relative
            for (int cap : caps) {
                int target = idx + cap;
                // Boundary Check für Diagonale
                int tRow = target / 8;
                int tCol = target % 8;
                if (isValidPos(target) && Math.abs(tCol - col) == 1) { // Nur 1 Spalte Differenz erlaubt
                    if (pieces[target] != Piece.EMPTY && Piece.isWhite(pieces[target]) != whiteToMove) {
                         // Capture + Promotion Check
                         if ((whiteToMove && target < 8) || (!whiteToMove && target >= 56)) {
                             Move m = new Move(idx, target);
                             m.promotion = whiteToMove ? Piece.W_QUEEN : Piece.B_QUEEN;
                             moves.add(m);
                         } else {
                             moves.add(new Move(idx, target));
                         }
                    }
                    // En Passant
                    // Ziel ist leer, aber enPassantCol passt zur Zielspalte und korrekte Reihe
                    if (pieces[target] == Piece.EMPTY && enPassantCol != -1) {
                         // Passt Reihe? Weiß en passant capture ist auf Reihe 2 (Index 16-23), Schwarz auf 5 (40-47).
                         // En Passant Target Square ist das Feld, auf das gezogen wird. 
                         // Der Bauer, der geschlagen wird, steht drüber/drunter.
                         int epCandidateRow = whiteToMove ? 2 : 5; // Die Zeile, WOHIN der Bauer zieht
                         if (tRow == epCandidateRow && tCol == enPassantCol) {
                             Move m = new Move(idx, target);
                             m.isEnPassant = true;
                             moves.add(m);
                         }
                    }
                }
            }
        }
        // Knight
        else if (t == PieceType.KNIGHT) {
            int[] diffs = {-17, -15, -10, -6, 6, 10, 15, 17};
            for (int d : diffs) {
                int target = idx + d;
                if (isValidPos(target) && isKnightJumpValid(idx, target)) {
                    if (pieces[target] == Piece.EMPTY || Piece.isWhite(pieces[target]) != whiteToMove) {
                        moves.add(new Move(idx, target));
                    }
                }
            }
        }
        // King
        else if (t == PieceType.KING) {
            int[] diffs = {-9, -8, -7, -1, 1, 7, 8, 9};
            for (int d : diffs) {
                int target = idx + d;
                // Slide Logic missbrauchen für Nachbarschaftscheck (ist slideValid auch für 1 step ok?)
                if (isValidPos(target) && isSlideValid(idx, target, d)) {
                     if (pieces[target] == Piece.EMPTY || Piece.isWhite(pieces[target]) != whiteToMove) {
                        moves.add(new Move(idx, target));
                    }
                }
            }
            // Castling
            if (!isCheck(whiteToMove)) { // Darf nicht im Schach sein
                if (whiteToMove) {
                    if (castleWK && pieces[61] == Piece.EMPTY && pieces[62] == Piece.EMPTY && 
                        !isSquareAttacked(61, false) && !isSquareAttacked(62, false)) { 
                        Move m = new Move(60, 62); m.isCastle = true; moves.add(m); 
                    }
                    if (castleWQ && pieces[59] == Piece.EMPTY && pieces[58] == Piece.EMPTY && pieces[57] == Piece.EMPTY &&
                         !isSquareAttacked(59, false) && !isSquareAttacked(58, false)) { 
                        Move m = new Move(60, 58); m.isCastle = true; moves.add(m); 
                    }
                } else {
                    if (castleBK && pieces[5] == Piece.EMPTY && pieces[6] == Piece.EMPTY &&
                         !isSquareAttacked(5, true) && !isSquareAttacked(6, true)) { 
                        Move m = new Move(4, 6); m.isCastle = true; moves.add(m); 
                    }
                    if (castleBQ && pieces[3] == Piece.EMPTY && pieces[2] == Piece.EMPTY && pieces[1] == Piece.EMPTY &&
                         !isSquareAttacked(3, true) && !isSquareAttacked(2, true)) { 
                        Move m = new Move(4, 2); m.isCastle = true; moves.add(m); 
                    }
                }
            }
        }
        // Sliding (Rook, Bishop, Queen)
        else {
             int[] dirs = (t == PieceType.ROOK) ? new int[]{-8, -1, 1, 8} : 
                          (t == PieceType.BISHOP) ? new int[]{-9, -7, 7, 9} :
                          new int[]{-9, -8, -7, -1, 1, 7, 8, 9};
             for (int d : dirs) {
                 for (int dist = 1; dist < 8; dist++) {
                     int target = idx + d * dist;
                     if (!isValidPos(target) || !isSlideValid(idx, target, d)) break;
                     
                     if (pieces[target] == Piece.EMPTY) {
                         moves.add(new Move(idx, target));
                     } else {
                         if (Piece.isWhite(pieces[target]) != whiteToMove) {
                             moves.add(new Move(idx, target));
                         }
                         break; // Hit piece
                     }
                 }
             }
        }
    }
}
