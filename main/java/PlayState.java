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

    // ── chão ────────────────────────────────────────────────────────────
    public static final int GROUND_H = 60;
    private float groundScroll;

    // ── Estado do jogo ────────────────────────────────────────────────────────
    private int score, coinScore, tickCount, deadTimer;
    private boolean dead;

    //── Míssil ────────────────────────────────────────────────────────
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

        if (flapPressed()) {
            birdVel = FLAP;
            birdAngle = -25;
        }

        prevBirdY = birdY;
        birdVel += GRAVITY;
        birdY += birdVel;
        birdAngle = Math.min(birdAngle + 3, 60);

        if (tickCount % PIPE_FREQ == 0) {
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

        //chama míssil ────────────────────────────────────────────────────────
        if(score != 0 && score%10 == 0 && score != lastMissileScore){
            
            missile = new Missile(birdY, game.width, score);
            lastMissileScore = score;

        }

       //atualiza míssil ────────────────────────────────────────────────────────
        if(missile != null){
            missile.update();
        }

        groundScroll = (groundScroll + PIPE_SPD) % 30;

        // Colisões (use hitbox retângulos — same bounds as the squares below)
        int bx = BIRD_X - BIRD_W / 2, by = (int) birdY - BIRD_H / 2;
        Rectangle birdRect = new Rectangle(bx + 3, by + 3, BIRD_W - 6, BIRD_H - 6); // slight inset

        //colisão do míssil ────────────────────────────────────────────────────────
        if(missile != null){
            Rectangle missileColide = missile.getBounds();

            if(birdRect.intersects(missileColide)){
                die();
                return;
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

        // ── chão ────────────────────────────────────────────────────────
        g.setColor(new Color(210, 170, 80));
        g.fillRect(0, groundTop, game.width, GROUND_H);
        // TODO: sprite

        // ── míssil ───────────────────────────────────────────────────────
        if(missile != null){
            missile.render(g);
        }

        // ── Bird (interpolated) ───────────────────────────────────────────
        float ry = prevBirdY + (birdY - prevBirdY) * alpha;
        int bx = BIRD_X - BIRD_W / 2;
        int by = (int) ry - BIRD_H / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(Math.toRadians(birdAngle), BIRD_X, ry);
        g2.setColor(new Color(255, 200, 0));
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
        g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2 + 2, 62);
        g.setColor(Color.WHITE);
        g.drawString(sc, game.width / 2 - fm.stringWidth(sc) / 2, 60);

        // ── Coin Score ───────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.YELLOW);
        g.drawString(
                "Moedas: " + coinScore,
                10,
                25);

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
