import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class PlayState implements GameState {

    private final Game game;

    // ── Sizes (public so MenuState can reference them) ────────────────────
    public static final int GROUND_H = 60;
    public static final int BIRD_W   = 34;
    public static final int BIRD_H   = 24;
    public static final int BIRD_X   = 80;
    public static final int PIPE_W   = 52;
    public static final int GAP      = 155;

    // ── Pipe cap dimensions ───────────────────────────────────────────────
    private static final int CAP_H   = 26;
    private static final int CAP_EXT = 6;

    // ── Bird ──────────────────────────────────────────────────────────────
    private float birdY, birdVel, prevBirdY;
    private double birdAngle;
    private static final float GRAVITY = 0.45f;
    private static final float FLAP    = -8.5f;

    // ── Pipes ─────────────────────────────────────────────────────────────
    private static final int   PIPE_FREQ = 90;
    private static final float PIPE_SPD  = 2.8f;
    private final List<int[]> pipes = new ArrayList<>();
    private final Random rng = new Random();

    // ── Coins ─────────────────────────────────────────────────────────────
    private int pipesUntilNextCoin;
    private final List<int[]> coins = new ArrayList<>(); // [x, y]
    private static final int COIN_SIZE   = 30;
    private static final int COIN_MARGIN = 30;
    private int coinsThisRun; // coins collected in this run only

    // ── Ground scroll ─────────────────────────────────────────────────────
    private float groundScroll;

    // ── Game state ────────────────────────────────────────────────────────
    private int score, tickCount, deadTimer;
    private boolean dead;

    // ── Sprites ───────────────────────────────────────────────────────────
    private BufferedImage imgBackground;
    private BufferedImage imgFloor;

    public PlayState(Game game) { this.game = game; }

    private BufferedImage load(String filename) {
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
        birdY = prevBirdY = game.height / 2f - 40;
        birdVel = 0;
        birdAngle = 0;
        pipes.clear();
        coins.clear();
        score = tickCount = deadTimer = 0;
        coinsThisRun = 0;
        pipesUntilNextCoin = 4 + rng.nextInt(3);
        dead = false;
        groundScroll = 0;

        imgBackground = load("background.png");
        imgFloor      = load("chao.png");
    }

    @Override public void onExit() {}

    private boolean flapPressed() {
        return game.keys.isJustPressed(KeyEvent.VK_SPACE)
            || game.keys.isJustPressed(KeyEvent.VK_UP)
            || game.keys.isJustPressed(KeyEvent.VK_W)
            || game.mouse.isJustPressed(MouseHandler.LEFT);
    }

    @Override
    public void update() {
        if (dead) {
            if (++deadTimer > 60 && flapPressed())
                game.setState(new MenuState(game, score));
            return;
        }

        tickCount++;
        if (flapPressed()) { birdVel = FLAP; birdAngle = -25; }

        prevBirdY  = birdY;
        birdVel   += GRAVITY;
        birdY     += birdVel;
        birdAngle  = Math.min(birdAngle + 3, 70);

        // ── Spawn pipes + coins ───────────────────────────────────────────
        if (tickCount % PIPE_FREQ == 0) {
            int gapY = 120 + rng.nextInt(game.height - GROUND_H - 120 - GAP);
            pipes.add(new int[]{ game.width + 10, gapY });

            pipesUntilNextCoin--;
            if (pipesUntilNextCoin <= 0) {
                int coinY = gapY + COIN_MARGIN
                        + rng.nextInt(GAP - 2 * COIN_SIZE - COIN_MARGIN);
                coins.add(new int[]{ game.width + 10 + PIPE_W / 2, coinY });
                pipesUntilNextCoin = 5 + rng.nextInt(6);
            }
        }

        // ── Scroll pipes ─────────────────────────────────────────────────
        for (int i = pipes.size() - 1; i >= 0; i--) {
            pipes.get(i)[0] -= (int) PIPE_SPD;
            if (pipes.get(i)[0] + PIPE_W / 2 == BIRD_X) score++;
            if (pipes.get(i)[0] + PIPE_W < 0) pipes.remove(i);
        }

        // ── Scroll coins ──────────────────────────────────────────────────
        for (int i = coins.size() - 1; i >= 0; i--) {
            coins.get(i)[0] -= (int) PIPE_SPD;
            if (coins.get(i)[0] < -20) coins.remove(i);
        }

        groundScroll = (groundScroll + PIPE_SPD) % game.width;

        int bx = BIRD_X - BIRD_W / 2, by = (int) birdY - BIRD_H / 2;
        Rectangle birdRect = new Rectangle(bx + 3, by + 3, BIRD_W - 6, BIRD_H - 6);

        // ── Coin collection ───────────────────────────────────────────────
        for (int i = coins.size() - 1; i >= 0; i--) {
            int[] coin = coins.get(i);
            Rectangle coinRect = new Rectangle(
                    coin[0] - COIN_SIZE / 2, coin[1] - COIN_SIZE / 2,
                    COIN_SIZE, COIN_SIZE);
            if (birdRect.intersects(coinRect)) {
                coinsThisRun++;
                game.coins++;          // persist to wallet immediately
                coins.remove(i);
            }
        }

        if (birdY - BIRD_H / 2f < 0) { birdY = BIRD_H / 2f; birdVel = 0; }
        if (birdY + BIRD_H / 2f >= game.height - GROUND_H) { die(); return; }

        for (int[] p : pipes) {
            int px = p[0], gapY = p[1];
            if (birdRect.intersects(new Rectangle(px, 0, PIPE_W, gapY)) ||
                birdRect.intersects(new Rectangle(px, gapY + GAP, PIPE_W, game.height)))
                { die(); return; }
        }
    }

    private void die() { dead = true; deadTimer = 0; birdVel = -5; }

    // ── Pipe drawing ─────────────────────────────────────────────────────
    private void drawPipe(Graphics2D g, int px, int y1, int y2, int capDir) {
        int bodyH = y2 - y1;
        if (bodyH <= 0) return;

        GradientPaint bodyGrad = new GradientPaint(
            px, y1, new Color(50, 140, 40), px + PIPE_W, y1, new Color(30, 100, 25));
        g.setPaint(bodyGrad);
        g.fillRect(px, y1, PIPE_W, bodyH);

        GradientPaint highlight = new GradientPaint(
            px, y1, new Color(120, 220, 80, 180), px + 10, y1, new Color(80, 170, 50, 0));
        g.setPaint(highlight);
        g.fillRect(px, y1, PIPE_W / 2, bodyH);

        g.setColor(new Color(20, 80, 15));
        g.fillRect(px + PIPE_W - 3, y1, 3, bodyH);

        int capX = px - CAP_EXT, capW = PIPE_W + CAP_EXT * 2;
        int capY = (capDir > 0) ? y2 - CAP_H : y1;

        GradientPaint capGrad = new GradientPaint(
            capX, capY, new Color(60, 160, 50), capX + capW, capY, new Color(35, 110, 28));
        g.setPaint(capGrad);
        g.fillRect(capX, capY, capW, CAP_H);

        GradientPaint capHL = new GradientPaint(
            capX, capY, new Color(140, 230, 90, 200), capX + 14, capY, new Color(80, 180, 55, 0));
        g.setPaint(capHL);
        g.fillRect(capX, capY, capW / 2, CAP_H);

        g.setColor(new Color(20, 80, 15));
        g.fillRect(capX + capW - 3, capY, 3, CAP_H);

        g.setColor(new Color(20, 70, 12));
        if (capDir > 0)
            g.drawLine(capX, capY + CAP_H - 1, capX + capW - 1, capY + CAP_H - 1);
        else
            g.drawLine(capX, capY, capX + capW - 1, capY);

        g.setColor(new Color(20, 70, 12));
        g.drawRect(px, y1, PIPE_W - 1, bodyH - 1);
        g.drawRect(capX, capY, capW - 1, CAP_H - 1);
    }

    // ── Bird skin drawing ─────────────────────────────────────────────────
    // Call AFTER drawing the bird body, inside the rotated g2 context.
    // bx/by are the top-left of the bird rectangle.
    private void drawSkin(Graphics2D g2, int bx, int by) {
        switch (game.equippedSkin) {
            case 1 -> drawHat(g2, bx, by);
            case 2 -> drawSunglasses(g2, bx, by);
            case 3 -> drawCape(g2, bx, by);
            case 4 -> drawCrown(g2, bx, by);
        }
    }

    /** A flat-topped hat sitting on the bird's head. */
    private void drawHat(Graphics2D g2, int bx, int by) {
        int cx = bx + BIRD_W / 2;
        // brim
        g2.setColor(new Color(30, 20, 10));
        g2.fillRect(cx - 10, by - 4, 20, 4);
        // crown
        g2.fillRect(cx - 6, by - 14, 12, 11);
        // hat band
        g2.setColor(new Color(180, 30, 30));
        g2.fillRect(cx - 6, by - 6, 12, 3);
    }

    /** Two oval lenses on the front face of the bird. */
    private void drawSunglasses(Graphics2D g2, int bx, int by) {
        int faceX = bx + BIRD_W - 10; // right side = front of the bird
        int midY  = by + BIRD_H / 2 - 2;
        g2.setColor(new Color(20, 20, 20, 200));
        g2.fillOval(faceX - 10, midY - 4, 9, 7);  // left lens
        g2.fillOval(faceX,      midY - 4, 9, 7);  // right lens
        g2.setColor(new Color(60, 60, 60));
        g2.drawLine(faceX - 1, midY, faceX, midY); // bridge
    }

    /** A small triangular cape trailing behind the bird. */
    private void drawCape(Graphics2D g2, int bx, int by) {
        int tipX = bx - 6;
        int topY = by + 4;
        int botY = by + BIRD_H - 4;
        int[] xs = { bx, bx, tipX };
        int[] ys = { topY, botY, (topY + botY) / 2 };
        g2.setColor(new Color(180, 30, 200));
        g2.fillPolygon(xs, ys, 3);
        g2.setColor(new Color(220, 100, 255));
        g2.drawPolygon(xs, ys, 3);
    }

    /** A three-point crown on top of the bird. */
    private void drawCrown(Graphics2D g2, int bx, int by) {
        int cx = bx + BIRD_W / 2;
        // three points: left, center, right
        int[] xs = { cx - 9, cx - 9, cx - 3, cx, cx + 3, cx + 9, cx + 9 };
        int[] ys = { by - 2, by - 10, by - 6, by - 12, by - 6, by - 10, by - 2 };
        g2.setColor(new Color(255, 200, 0));
        g2.fillPolygon(xs, ys, 7);
        g2.setColor(new Color(200, 140, 0));
        g2.drawPolygon(xs, ys, 7);
        // jewels
        g2.setColor(new Color(255, 60, 60));
        g2.fillOval(cx - 2, by - 12, 4, 4);
    }

    @Override
    public void render(Graphics2D g, float alpha) {
        int groundTop = game.height - GROUND_H;

        // ── Background ────────────────────────────────────────────────────
        g.drawImage(imgBackground, 0, 0, game.width, groundTop, null);

        // ── Pipes ─────────────────────────────────────────────────────────
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int[] p : pipes) {
            int px = p[0], gapY = p[1];
            drawPipe(g, px, 0,          gapY,       1);
            drawPipe(g, px, gapY + GAP, groundTop, -1);
        }

        // ── Floor ─────────────────────────────────────────────────────────
        int offset = (int) groundScroll;
        g.drawImage(imgFloor, -offset,             groundTop, game.width, game.height, null);
        g.drawImage(imgFloor, game.width - offset,  groundTop, game.width, game.height, null);

        // ── Bird + skin ───────────────────────────────────────────────────
        float ry = prevBirdY + (birdY - prevBirdY) * alpha;
        int bx = BIRD_X - BIRD_W / 2, by = (int) ry - BIRD_H / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.rotate(Math.toRadians(birdAngle), BIRD_X, ry);
        g2.setColor(new Color(255, 200, 0));
        g2.fillRect(bx, by, BIRD_W, BIRD_H);
        // replace with: g2.drawImage(imgBird, bx, by, BIRD_W, BIRD_H, null);
        drawSkin(g2, bx, by);
        g2.dispose();

        // ── Coins ─────────────────────────────────────────────────────────
        g.setColor(new Color(255, 210, 0));
        for (int[] coin : coins) {
            g.fillOval(coin[0] - COIN_SIZE / 2, coin[1] - COIN_SIZE / 2, COIN_SIZE, COIN_SIZE);
            g.setColor(new Color(200, 150, 0));
            g.drawOval(coin[0] - COIN_SIZE / 2, coin[1] - COIN_SIZE / 2, COIN_SIZE, COIN_SIZE);
            g.setColor(new Color(255, 230, 80));
            g.drawString("$", coin[0] - 5, coin[1] + 6);
            g.setColor(new Color(255, 210, 0));
        }

        // ── Score ─────────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        String sc = String.valueOf(score);
        g.setColor(new Color(0, 0, 0, 80));
        g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2 + 2, 62);
        g.setColor(Color.WHITE);
        g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2, 60);

        // ── Coin HUD ──────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 220, 0));
        g.drawString("$ " + game.coins, 10, 25); // total wallet
        if (coinsThisRun > 0) {
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(new Color(255, 240, 150));
            g.drawString("+" + coinsThisRun + " esta run", 10, 43);
        }

        // ── Game-over overlay ─────────────────────────────────────────────
        if (dead) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, game.width, game.height);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            fm = g.getFontMetrics();
            String msg = "GAME OVER";
            g.drawString(msg, game.width / 2 - fm.stringWidth(msg) / 2, game.height / 2 - 30);

            g.setFont(new Font("Arial", Font.BOLD, 22));
            fm = g.getFontMetrics();
            String sc2 = "Score: " + score;
            g.drawString(sc2, game.width / 2 - fm.stringWidth(sc2) / 2, game.height / 2 + 10);

            g.setColor(new Color(255, 220, 0));
            String coinText = "Moedas: " + coinsThisRun + "  (total: " + game.coins + ")";
            g.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g.getFontMetrics();
            g.drawString(coinText, game.width / 2 - fm.stringWidth(coinText) / 2, game.height / 2 + 38);

            if (deadTimer > 60) {
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                fm = g.getFontMetrics();
                String hint = "SPACE / CLICK to continue";
                g.setColor(new Color(220, 220, 220));
                g.drawString(hint, game.width / 2 - fm.stringWidth(hint) / 2, game.height / 2 + 68);
            }
        }
    }
}