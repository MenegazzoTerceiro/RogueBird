import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayState implements GameState {

    private final Game game;

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

    // ── Pipes ─────────────────────────────────────────────────────────────
    public static final int PIPE_W = 52;
    public static final int GAP = 155;
    private static final int PIPE_FREQ = 90;
    private static final float PIPE_SPD = 2.8f;
    private final List<int[]> pipes = new ArrayList<>(); // [x, gapY]
    private final Random rng = new Random();

    // ── Coins ─────────────────────────────────────────────────────────────
    private int pipesUntilNextCoin;
    private final List<int[]> coins = new ArrayList<>(); // [x, y]
    private static final int COIN_SIZE = 30;
    private static final int COIN_MARGIN = 30;

    // ── Boss ─────────────────────────────────────────────────────────────
    private Boss boss;
    private boolean bossFight;
    private boolean bossSpawned;
    private static final int BOSS_SCORE_TRIGGER = 5;
    private int nextBossScore;
    private int bossLevel;
    // ── chão ────────────────────────────────────────────────────────────
    public static final int GROUND_H = 60;
    private float groundScroll;

    // ── Estado do jogo ────────────────────────────────────────────────────────
    private int score, coinScore, tickCount, deadTimer;
    private boolean dead;

    // ── Míssil ────────────────────────────────────────────────────────
    private Missile missile;
    private int lastMissileScore = 0;

    public PlayState(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        birdY = prevBirdY = game.height / 2f - 40;
        birdVel = 0;
        birdAngle = 0;
        pipes.clear();
        coins.clear();
        score = coinScore = tickCount = deadTimer = 0;
        pipesUntilNextCoin = 4 + rng.nextInt(3);
        dead = false;
        groundScroll = 0;
        boss = null;
        bossFight = false;
        bossSpawned = false;
        nextBossScore = 19; // spawn do primeiro boss após 19 pontos (diminuir para testes)
        bossLevel = 1;
        bird = new Bird();
        birdBullets = new ArrayList<>();
        lastShotTime = 0;
    }

    @Override
    public void onExit() {
    }

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

        if (!bossFight && score >= nextBossScore) {
            bossFight = true;
        }

        if (flapPressed()) {
            birdVel = FLAP;
            birdAngle = -25;
        }
        // ativar escudo com shift
        if (game.keys.isJustPressed(KeyEvent.VK_SHIFT)) {
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

        prevBirdY = birdY;
        birdVel += GRAVITY;
        birdY += birdVel;
        birdAngle = Math.min(birdAngle + 3, 60);

        if (!bossFight && tickCount % PIPE_FREQ == 0) {
            int gapY = 120 + rng.nextInt(
                    game.height - GROUND_H - 120 - GAP);

            pipes.add(new int[] {
                    game.width + 10,
                    gapY
            });

            pipesUntilNextCoin--;

            if (pipesUntilNextCoin <= 0) {
                int coinY = gapY + COIN_MARGIN + rng.nextInt(GAP - 2 * COIN_SIZE - COIN_MARGIN);
                coins.add(new int[] {
                        game.width + 10 + PIPE_W / 2,
                        coinY
                });

                pipesUntilNextCoin = 5 + rng.nextInt(6);
            }
        }

        for (int i = pipes.size() - 1; i >= 0; i--) {
            pipes.get(i)[0] -= (int) PIPE_SPD;
            if (pipes.get(i)[0] + PIPE_W / 2 == BIRD_X)
                score++;
            if (pipes.get(i)[0] + PIPE_W < 0)
                pipes.remove(i);
        }

        // andar das moedas e checar coleta
        for (int i = coins.size() - 1; i >= 0; i--) {
            coins.get(i)[0] -= (int) PIPE_SPD;
            if (coins.get(i)[0] < -20)
                coins.remove(i);
        }

        // spawn do boss
        if (bossFight && !bossSpawned && pipes.isEmpty()) {

            int bossSize = BIRD_W * 2;

            boss = new Boss(
                    game.width + 50, // nasce fora da tela
                    200,
                    bossSize - 10,
                    bossSize,
                    game.width - 100,
                    game.height / 2,
                    bossLevel);

            bossSpawned = true;
            System.out.println(
                    "Boss nasceu com "
                            + boss.getCurrentHealth()
                            + "/"
                            + boss.getMaxHealth());

        }

        // entrada do boss
        if (boss != null) {
            boss.updat(BIRD_X, (int) birdY);
            if (bossSpawned && boss != null && boss.hasFinishedDeathAnimation()) {
                bossLevel++;
                boss = null;
                bossFight = false;
                bossSpawned = false;

                defineNextBossScore();
            }
        }
        // chama míssil ────────────────────────────────────────────────────────
        if (score != 0 && score % 5 == 0 && score != lastMissileScore) {

            missile = new Missile(birdY, game.width, score);
            lastMissileScore = score;

        }

        // atualiza míssil ────────────────────────────────────────────────────────
        if (missile != null) {
            missile.update();
        }

        groundScroll = (groundScroll + PIPE_SPD) % 30;

        // Colisões (use hitbox retângulos — same bounds as the squares below)
        int bx = BIRD_X - BIRD_W / 2, by = (int) birdY - BIRD_H / 2;
        Rectangle birdRect = new Rectangle(bx + 3, by + 3, BIRD_W - 6, BIRD_H - 6); // slight inset

        // colisão com o laser do boss
        if (boss != null) {
            Shape laserHitbox = boss.getLaserHitbox();

            if (laserHitbox != null && !boss.hasLaserHit() && laserHitbox.intersects(
                    birdRect.getX(),
                    birdRect.getY(),
                    birdRect.getWidth(),
                    birdRect.getHeight())) {

                boss.markLaserHit();
                bird.takeDamage(boss.getLaserDamage());
                System.out.println(
                        "LASER ACERTOU - DANO: "
                                + boss.getLaserDamage()); // substituir por função de dano
            }
        }

        // colisão com fireballs do boss
        if (boss != null) {
            for (int i = boss.getFireball().size() - 1; i >= 0; i--) {

                Fireball fireball = boss.getFireball().get(i);

                if (birdRect.intersects(fireball.getBounds())) {
                    bird.takeDamage(boss.getFireballDamage());

                    System.out.println(
                            "FIREBALL ACERTOU - DANO: "
                                    + boss.getFireballDamage());

                    boss.getFireball().remove(i);
                }
            }
        }

        if (boss != null) {
            for (int i = birdBullets.size() - 1; i >= 0; i--) {

                BirdBullet bullet = birdBullets.get(i);

                if (bullet.getBounds().intersects(boss.getBounds())) {
                    System.err.println(
                            "BOSS ACERTOU - DANO: "
                                    + bird.getBulletDamage());
                    boss.takeDamage(bird.getBulletDamage());
                    birdBullets.remove(i);
                }
            }
        }

        // colisão com moedas
        for (int i = coins.size() - 1; i >= 0; i--) {
            int[] coin = coins.get(i);
            Rectangle coinRect = new Rectangle(
                    coin[0] - COIN_SIZE / 2,
                    coin[1] - COIN_SIZE / 2,
                    COIN_SIZE,
                    COIN_SIZE);

            if (birdRect.intersects(coinRect)) {
                coinScore++;
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

        for (int[] p : pipes) {
            int px = p[0], gapY = p[1];
            Rectangle top = new Rectangle(px, 0, PIPE_W, gapY);
            Rectangle bot = new Rectangle(px, gapY + GAP, PIPE_W, game.height);
            if (birdRect.intersects(top) || birdRect.intersects(bot)) {
                die();
                return;
            }
        }

        // morte do pássaro
        if (bird.isDead()) {
            die();
            return;
        }
    }

    private void defineNextBossScore() {
        nextBossScore = score + 17 + rng.nextInt(7); // próximo boss spawnará entre 17 e 23 pontos após o último (abaixe
                                                     // o 17 para testes)
    }

    private void die() {
        dead = true;
        deadTimer = 0;
        birdVel = -5;
    }

    @Override
    public void render(Graphics2D g, float alpha) {
        int groundTop = game.height - GROUND_H;

        // ── Background ────────────────────────────────────────────────────
        g.setColor(new Color(80, 180, 240));
        g.fillRect(0, 0, game.width, groundTop);
        // TODO: replace with background sprite

        // ── Canos ─────────────────────────────────────────────────────────
        for (int[] p : pipes) {
            int px = p[0], gapY = p[1];

            // Top pipe
            g.setColor(new Color(80, 180, 60));
            g.fillRect(px, 0, PIPE_W, gapY);
            // TODO: replace with pipe-top sprite (flipped)

            // Bottom pipe
            g.fillRect(px, gapY + GAP, PIPE_W, game.height - (gapY + GAP));
            // TODO: replace with pipe-bottom sprite
        }

        // ── Boss ─────────────────────────────────────────────────────────────
        if (bossSpawned && boss != null) {
            boss.render(g);
        }

        // ── chão ────────────────────────────────────────────────────────
        g.setColor(new Color(210, 170, 80));
        g.fillRect(0, groundTop, game.width, GROUND_H);
        // TODO: sprite

        // ── míssil ───────────────────────────────────────────────────────
        if (missile != null) {
            missile.render(g);
        }

        // ── Bird (interpolated) ───────────────────────────────────────────
        float ry = prevBirdY + (birdY - prevBirdY) * alpha;
        int bx = BIRD_X - BIRD_W / 2;
        int by = (int) ry - BIRD_H / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(Math.toRadians(birdAngle), BIRD_X, ry);
        if (bird.isShieldActive()) {
            g2.setColor(new Color(173, 216, 230)); // azul bebê
        } else {
            g2.setColor(new Color(255, 200, 0)); // amarelo normal
        }

        g2.fillRect(bx, by, BIRD_W, BIRD_H);
        // TODO: sprites do pássaro
        g2.dispose();

        // ── Coins ─────────────────────────────────────────────────────────
        g.setColor(Color.YELLOW);
        for (int[] coin : coins) {
            g.fillOval(
                    coin[0] - COIN_SIZE / 2,
                    coin[1] - COIN_SIZE / 2,
                    COIN_SIZE,
                    COIN_SIZE);
        }

        // ── Score ─────────────────────────────────────────────────────────

        g.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        String sc = String.valueOf(score);
        g.setColor(new Color(0, 0, 0, 80));

        if (!bossSpawned) { // não mostrar score durante a luta com o boss
            g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2 + 2, 62);
            g.setColor(Color.WHITE);
            g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2, 60);
        }

        // ── Boss Health Bar ───────────────────────────────────────────────
        if (bossSpawned && boss != null) {

            int barWidth = 260;
            int barHeight = 20;

            int x = game.width / 2 - barWidth / 2;
            int y = 35;

            g.setColor(Color.GRAY);
            g.fillRect(x, y, barWidth, barHeight);

            int currentWidth = boss.getCurrentHealth() * barWidth
                    / boss.getMaxHealth();

            g.setColor(Color.RED);
            g.fillRect(x, y, currentWidth, barHeight);

            g.setColor(Color.WHITE);
            g.drawRect(x, y, barWidth, barHeight);
        }

        // ── Coin Score ───────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.YELLOW);
        g.drawString(
                "Moedas: " + coinScore,
                10,
                25);

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

            String coinText = "Moedas: " + coinScore;
            g.drawString(
                    coinText,
                    game.width / 2 - fm.stringWidth(coinText) / 2,
                    game.height / 2 + 40);

            if (deadTimer > 60) {
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                fm = g.getFontMetrics();
                String hint = "SPACE / CLICK to continue";
                g.setColor(new Color(220, 220, 220));
                g.drawString(hint, game.width / 2 - fm.stringWidth(hint) / 2, game.height / 2 + 75);
            }
        }
    }
}