import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class MenuState implements GameState {

    private final Game game;
    private int tick;
    private boolean blink = true;
    private int highScore;
    private Rectangle btnLoja = new Rectangle(148, 430, 100, 50);

    // ── Sprites ───────────────────────────────────────────────────────────
    private BufferedImage imgBackground; // background.png
    private BufferedImage imgFloor;      // chao.png

    public MenuState(Game game)         { this(game, 0); }
    public MenuState(Game game, int hi) { this.game = game; this.highScore = hi; }

    // ── Fixed Sprite loader ───────────────────────────────────────────────
    private BufferedImage load(String filename) {
        // Caminho corrigido para buscar dentro de java/recursos/sprites/
        String path = "/recursos/sprites/" + filename;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null)
                throw new RuntimeException("Sprite not found: " + path);
            return ImageIO.read(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load: " + path, e);
        }
    }

    @Override
    public void onEnter() {
        tick = 0;
        imgBackground = load("background.png");
        imgFloor      = load("chao.png");
    }

    @Override public void onExit() {}

    @Override
    public void update() {
        if (++tick % 35 == 0) blink = !blink;

        Point p = game.mouse.getPosition();
        boolean click = game.mouse.isJustPressed(MouseHandler.LEFT);

        if (click && btnLoja.contains(p)) {
            game.setState(new ShopState(game));
        } else if (game.keys.isJustPressed(KeyEvent.VK_SPACE)
                || game.keys.isJustPressed(KeyEvent.VK_ENTER)
                || (p != null && click && !btnLoja.contains(p))) {
            game.setState(new PlayState(game));
        }
    }

    @Override
    public void render(Graphics2D g, float alpha) {
        int cx     = game.width / 2;
        int floorY = game.height - PlayState.GROUND_H;

        // ── Background ────────────────────────────────────────────────────
        g.drawImage(imgBackground,
            0, 0, game.width, game.height,
            null);

        // ── Ground ────────────────────────────────────────────────────────
        g.drawImage(imgFloor,
            0, floorY, game.width, game.height,
            null);

        // ── Bird (preview square) ─────────────────────────────────────────
        g.setColor(new Color(255, 200, 0));
        g.fillRect(cx - PlayState.BIRD_W / 2, 240, PlayState.BIRD_W, PlayState.BIRD_H);
        // TODO: replace with bird sprite (idle frame)

        // ── Title ─────────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 44));
        FontMetrics fm = g.getFontMetrics();
        String title = "FLAPPY BIRD";
        g.setColor(new Color(80, 40, 0));
        g.drawString(title, cx - fm.stringWidth(title) / 2 + 3, 163);
        g.setColor(new Color(255, 220, 0));
        g.drawString(title, cx - fm.stringWidth(title) / 2, 160);

        // ── Shop button ───────────────────────────────────────────────────
        String botão = "LOJA";
        g.setFont(new Font("Arial", Font.PLAIN, 25));
        fm = g.getFontMetrics();
        g.setColor(new Color(255, 220, 0));
        g.fill(btnLoja);
        g.setColor(new Color(80, 40, 0));
        g.drawString(botão, 165, 460);

        // ── Prompt ────────────────────────────────────────────────────────
        if (blink) {
            g.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g.getFontMetrics();
            String prompt = "SPACE / CLICK to start";
            g.setColor(Color.WHITE);
            g.drawString(prompt, cx - fm.stringWidth(prompt) / 2, 340);
        }

        // ── Best score ────────────────────────────────────────────────────
        if (highScore > 0) {
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g.getFontMetrics();
            String hi = "Best: " + highScore;
            g.setColor(Color.WHITE);
            g.drawString(hi, cx - fm.stringWidth(hi) / 2, 370);
        }
    }
}
