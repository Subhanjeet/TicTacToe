package TicTacToe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TicTacToe extends JFrame implements ActionListener {

    JButton[] buttons = new JButton[9];
    boolean xTurn = true;

    TicTacToe() {
        setTitle("Tic Tac Toe");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 3));

        for (int i = 0; i < 9; i++) {
            buttons[i] = new JButton("");
            buttons[i].setFont(new Font("Arial", Font.BOLD, 50));
            buttons[i].addActionListener(this);
            add(buttons[i]);
        }

        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JButton clickedButton = (JButton) e.getSource();

        // Dont allow clicking an already filled box
        if (!clickedButton.getText().equals("")) {
            return;
        }

        // Put X or O
        if (xTurn) {
            clickedButton.setText("X");
        } else {
            clickedButton.setText("O");
        }

        // Check winner
        if (checkWinner()) {
            String winner = xTurn ? "X" : "O";

            JOptionPane.showMessageDialog(
                    this,
                    winner + " wins!"
            );

            resetGame();
            return;
        }

        // Check draw
        if (checkDraw()) {
            JOptionPane.showMessageDialog(
                    this,
                    "It's a draw!"
            );

            resetGame();
            return;
        }

        // Change turn
        xTurn = !xTurn;
    }

    boolean checkWinner() {

        int[][] winningPatterns = {
                {0, 1, 2},
                {3, 4, 5},
                {6, 7, 8},
                {0, 3, 6},
                {1, 4, 7},
                {2, 5, 8},
                {0, 4, 8},
                {2, 4, 6}
        };

        for (int[] pattern : winningPatterns) {

            String a = buttons[pattern[0]].getText();
            String b = buttons[pattern[1]].getText();
            String c = buttons[pattern[2]].getText();

            if (!a.equals("") && a.equals(b) && b.equals(c)) {
                return true;
            }
        }

        return false;
    }

    boolean checkDraw() {

        for (JButton button : buttons) {
            if (button.getText().equals("")) {
                return false;
            }
        }

        return true;
    }

    void resetGame() {

        for (JButton button : buttons) {
            button.setText("");
        }

        xTurn = true;
    }

    public static void main(String[] args) {
        new TicTacToe();
    }
}