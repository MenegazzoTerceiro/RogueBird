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
    private static final float FLAP = -8.5f;
    public static final int BIRD_X = 80;
    public static final int BIRD_W = 34;
    public static final int BIRD_H = 24;
    private Bird bird;
    private ArrayList<BirdBullet> birdBullets;
    private long lastShotTime;
    private static final long SHOT_COOLDOWN = 250;
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
    private int coinsThisRun;

    // ── Ground scroll ─────────────────────────────────────────────────────
    public static final int GROUND_H = 60;
    private float groundScroll;

    // ── Boss ──────────────────────────────────────────────────────────────
    private Boss boss;
    private boolean bossFight;
    private boolean bossSpawned;
    private int nextBossScore;
    private int bossLevel;

    // ── Estado do jogo ────────────────────────────────────────────────────────
    private int score, coinScore, tickCount, deadTimer;
    private boolean dead;

    // ── Míssil ────────────────────────────────────────────────────────
    private Missile missile;
    private int lastMissileScore = 0;

    public PlayState(Game game) {
        this.game = game;
    // ── Missile ───────────────────────────────────────────────────────────
    private Missile missile;
    private int lastMissileScore = 0;

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
        boss = null;
        bossFight = false;
        bossSpawned = false;
        nextBossScore = 19;
        bossLevel = 1;
        bird = new Bird();
        birdBullets = new ArrayList<>();
        lastShotTime = 0;
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

        if (flapPressed()) {
            birdVel = FLAP;
            birdAngle = -25;
        }
        // ativar escudo com shift
        if (game.keys.isJustPressed(KeyEvent.VK_SHIFT)|| game.mouse.isJustPressed(MouseHandler.MIDDLE)) {
            bird.activateShield();
        }

        long now = System.currentTimeMillis();
        
        // atirar com o botão direito do mouse
        if (game.mouse.isJustPressed(MouseHandler.RIGHT)
                || game.keys.isJustPressed(KeyEvent.VK_ENTER)
                        && now - lastShotTime >= SHOT_COOLDOWN) {
            birdBullets.add(new BirdBullet(BIRD_X + BIRD_W / 2, birdY));
            lastShotTime = now; // atualiza o tempo do último tiro
        }

        for (int i = birdBullets.size() - 1; i >= 0; i--) {
            BirdBullet bullet = birdBullets.get(i);
            bullet.update();
            if (bullet.isOffScreen(game.width)) {
                birdBullets.remove(i);
            }
        }

        prevBirdY  = birdY;
        birdVel   += GRAVITY;
        birdY     += birdVel;
        birdAngle  = Math.min(birdAngle + 3, 60);

        // ── Check boss trigger ────────────────────────────────────────────
        if (!bossFight && score >= nextBossScore) {
            bossFight = true;
        }

        // ── Spawn pipes + coins ───────────────────────────────────────────
        if (!bossFight && tickCount % PIPE_FREQ == 0) {
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

        // ── Scroll pipes ──────────────────────────────────────────────────
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

        // ── Spawn boss ────────────────────────────────────────────────────
        if (bossFight && !bossSpawned && pipes.isEmpty()) {
            int bossSize = BIRD_W * 2;
            boss = new Boss(
                    game.width + 50,
                    200,
                    bossSize - 10,
                    bossSize,
                    game.width - 100,
                    game.height / 2,
                    bossLevel);
            bossSpawned = true;
            System.out.println("Boss nasceu com "
                    + boss.getCurrentHealth() + "/" + boss.getMaxHealth());
        }

        // ── Update boss ───────────────────────────────────────────────────
        if (boss != null) {
            boss.updat(BIRD_X, (int) birdY);
            if (bossSpawned && boss.hasFinishedDeathAnimation()) {
                bossLevel++;
                boss = null;
                bossFight = false;
                bossSpawned = false;
                defineNextBossScore();
            }
        }
        // chama míssil ────────────────────────────────────────────────────────
        if (score != 0 && score % 5 == 0 && score != lastMissileScore) {


        // ── Spawn missile ─────────────────────────────────────────────────
        if (score != 0 && score % 10 == 0 && score != lastMissileScore) {
            missile = new Missile(birdY, game.width, score);
            lastMissileScore = score;
        }

        // atualiza míssil ────────────────────────────────────────────────────────
        // ── Update missile ────────────────────────────────────────────────
        if (missile != null) {
            missile.update();
        }

        // ── Collision rects ───────────────────────────────────────────────
        int bx = BIRD_X - BIRD_W / 2, by = (int) birdY - BIRD_H / 2;
        Rectangle birdRect = new Rectangle(bx + 3, by + 3, BIRD_W - 6, BIRD_H - 6);

        // ── Boss laser collision ──────────────────────────────────────────
        if (boss != null) {
            Shape laserHitbox = boss.getLaserHitbox();
            if (laserHitbox != null && !boss.hasLaserHit() && laserHitbox.intersects(
                    birdRect.getX(), birdRect.getY(),
                    birdRect.getWidth(), birdRect.getHeight())) {
                boss.markLaserHit();
                bird.takeDamage(boss.getLaserDamage());
                System.out.println(
                        "LASER ACERTOU - DANO: "
                                + boss.getLaserDamage()); // substituir por função de dano
                System.out.println("LASER ACERTOU - DANO: " + boss.getLaserDamage());
            }
        }

        // ── Boss fireball collision ───────────────────────────────────────
        if (boss != null) {
            for (int i = boss.getFireball().size() - 1; i >= 0; i--) {
                Fireball fireball = boss.getFireball().get(i);

                if (birdRect.intersects(fireball.getBounds())) {
                    bird.takeDamage(boss.getFireballDamage());

                    System.out.println(
                            "FIREBALL ACERTOU - DANO: "
                                    + boss.getFireballDamage());

                if (birdRect.intersects(fireball.getBounds())) {
                    System.out.println("FIREBALL ACERTOU - DANO: " + boss.getFireballDamage());
                    boss.getFireball().remove(i);
                }
            }
        }

        // Bird balas atingindo o boss
        if (boss != null) {
            for (int i = birdBullets.size() - 1; i >= 0; i--) {

                BirdBullet bullet = birdBullets.get(i);

                if (bullet.getBounds().intersects(boss.getBounds())) {
                    System.err.println(
                            "BIRD ACERTOU - DANO: "
                                    + bird.getBulletDamage());
                    boss.takeDamage(bird.getBulletDamage());
                    birdBullets.remove(i);
                }
            }
        }

        // colisão com moedas
        // ── Coin collection ───────────────────────────────────────────────
        for (int i = coins.size() - 1; i >= 0; i--) {
            int[] coin = coins.get(i);
            Rectangle coinRect = new Rectangle(
                    coin[0] - COIN_SIZE / 2, coin[1] - COIN_SIZE / 2,
                    COIN_SIZE, COIN_SIZE);
            if (birdRect.intersects(coinRect)) {
                coinsThisRun++;
                game.coins++;
                coins.remove(i);
            }
        }

        // colisão com o míssil
        if (missile != null) {

            if (birdRect.intersects(
                    missile.getBounds())) {
                bird.takeDamage(1);
                missile = null;
            }
        }

        if (birdY - BIRD_H / 2f < 0) {
            birdY = BIRD_H / 2f;
            birdVel = 0;
        }
        if (birdY + BIRD_H / 2f >= game.height - GROUND_H) {
            die();
            return;
        }

        // ── Boundary checks ───────────────────────────────────────────────
        if (birdY - BIRD_H / 2f < 0) { birdY = BIRD_H / 2f; birdVel = 0; }
        if (birdY + BIRD_H / 2f >= game.height - GROUND_H) { die(); return; }

        // ── Temp boss damage (1/sec) ──────────────────────────────────────
        if (bossSpawned && boss != null && tickCount % 60 == 0) {
            boss.takeDamage(1);
        }

        // ── Pipe collision ────────────────────────────────────────────────
        for (int[] p : pipes) {
            int px = p[0], gapY = p[1];
            if (birdRect.intersects(new Rectangle(px, 0, PIPE_W, gapY)) ||
                birdRect.intersects(new Rectangle(px, gapY + GAP, PIPE_W, game.height))) {
                die(); return;
            }
        }

        // morte do pássaro
        if (bird.isDead()) {
            die();
            return;
        }
    }

    private void die() { dead = true; deadTimer = 0; birdVel = -5; }

    private void defineNextBossScore() {
        nextBossScore = score + 16 + rng.nextInt(7); // próximo boss spawnará entre 17 e 23 pontos após o último (abaixe
                                                     // o 17 para testes)
        nextBossScore = score + 17 + rng.nextInt(7);
    }

    // ── Pipe drawing ──────────────────────────────────────────────────────
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
    private void drawSkin(Graphics2D g2, int bx, int by) {
        switch (game.equippedSkin) {
            case 1 -> drawHat(g2, bx, by);
            case 2 -> drawSunglasses(g2, bx, by);
            case 3 -> drawCape(g2, bx, by);
            case 4 -> drawCrown(g2, bx, by);
        }
    }

    private void drawHat(Graphics2D g2, int bx, int by) {
        int cx = bx + BIRD_W / 2;
        g2.setColor(new Color(30, 20, 10));
        g2.fillRect(cx - 10, by - 4, 20, 4);
        g2.fillRect(cx - 6, by - 14, 12, 11);
        g2.setColor(new Color(180, 30, 30));
        g2.fillRect(cx - 6, by - 6, 12, 3);
    }

    private void drawSunglasses(Graphics2D g2, int bx, int by) {
        int faceX = bx + BIRD_W - 10;
        int midY  = by + BIRD_H / 2 - 2;
        g2.setColor(new Color(20, 20, 20, 200));
        g2.fillOval(faceX - 10, midY - 4, 9, 7);
        g2.fillOval(faceX,      midY - 4, 9, 7);
        g2.setColor(new Color(60, 60, 60));
        g2.drawLine(faceX - 1, midY, faceX, midY);
    }

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

    private void drawCrown(Graphics2D g2, int bx, int by) {
        int cx = bx + BIRD_W / 2;
        int[] xs = { cx - 9, cx - 9, cx - 3, cx, cx + 3, cx + 9, cx + 9 };
        int[] ys = { by - 2, by - 10, by - 6, by - 12, by - 6, by - 10, by - 2 };
        g2.setColor(new Color(255, 200, 0));
        g2.fillPolygon(xs, ys, 7);
        g2.setColor(new Color(200, 140, 0));
        g2.drawPolygon(xs, ys, 7);
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
        g.drawImage(imgFloor, -offset,            groundTop, game.width, game.height, null);
        g.drawImage(imgFloor, game.width - offset, groundTop, game.width, game.height, null);

        // ── Boss ──────────────────────────────────────────────────────────
        if (bossSpawned && boss != null) {
            boss.render(g);
        }

        // ── chão ────────────────────────────────────────────────────────
        g.setColor(new Color(210, 170, 80));
        g.fillRect(0, groundTop, game.width, GROUND_H);
        // TODO: sprite

        // ── míssil ───────────────────────────────────────────────────────
        // ── Missile ───────────────────────────────────────────────────────
        if (missile != null) {
            missile.render(g);
        }

        // ── Bird (interpolated) ───────────────────────────────────────────
        float ry = prevBirdY + (birdY - prevBirdY) * alpha;
        int bx = BIRD_X - BIRD_W / 2, by = (int) ry - BIRD_H / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.rotate(Math.toRadians(birdAngle), BIRD_X, ry);
        if (bird.isShieldActive()) {
            g2.setColor(new Color(173, 216, 230)); // azul bebê
        } else {
            g2.setColor(new Color(255, 200, 0)); // amarelo normal
        }

        g2.fillRect(bx, by, BIRD_W, BIRD_H);
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

        if (!bossSpawned) {
            g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2 + 2, 62);
            g.setColor(Color.WHITE);
            g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2, 60);
        }

        // ── Boss health bar ───────────────────────────────────────────────
        if (bossSpawned && boss != null) {
            int barWidth = 260, barHeight = 20;
            int x = game.width / 2 - barWidth / 2, y = 35;
            g.setColor(Color.GRAY);
            g.fillRect(x, y, barWidth, barHeight);
            int currentWidth = boss.getCurrentHealth() * barWidth / boss.getMaxHealth();
            g.setColor(Color.RED);
            g.fillRect(x, y, currentWidth, barHeight);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, barWidth, barHeight);
        }

        // ── Coin HUD ──────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 220, 0));
        g.drawString("$ " + game.coins, 10, 25);
        if (coinsThisRun > 0) {
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(new Color(255, 240, 150));
            g.drawString("+" + coinsThisRun + " esta run", 10, 43);
        }

        // ── bird tools ───────────────────────────────────────────────
        int lifeBarWidth = 120;
        int lifeBarHeight = 15;

        int lifeBarX = game.width - lifeBarWidth - 10;
        int lifeBarY = 10;

        g.setColor(Color.GRAY);
        g.fillRect(
                lifeBarX,
                lifeBarY,
                lifeBarWidth,
                lifeBarHeight);

        int currentLifeWidth = bird.getCurrentHealth()
                * lifeBarWidth
                / bird.getMaxHealth();

        g.setColor(new Color(144, 238, 144)); // verde clarinho

        g.fillRect(
                lifeBarX,
                lifeBarY,
                currentLifeWidth,
                lifeBarHeight);

        g.setColor(Color.WHITE);

        g.drawRect(
                lifeBarX,
                lifeBarY,
                lifeBarWidth,
                lifeBarHeight);

        if (bird.isShieldActive()) {
            int shieldWidth = bird.getShieldHealth()
                    * lifeBarWidth
                    / bird.getMaxShieldhealth();
        }

        for (BirdBullet bullet : birdBullets) {
            bullet.render(g);
        }

        // ── overlay ─────────────────────────────────────────────
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