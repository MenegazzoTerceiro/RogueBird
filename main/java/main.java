import javax.swing.*;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Game("Flappy Bird", 400, 600));
    }
}
