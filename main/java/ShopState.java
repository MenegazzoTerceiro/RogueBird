import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class ShopState implements GameState {

    private final Game game;
    private boolean blink = true;
    private int tick;

    // ── Sprites ───────────────────────────────────────────────────────────
    private BufferedImage imgBackground;
    private BufferedImage imgFloor;

    // ── Shop items: { skinId, price } ────────────────────────────────────
    private static final int[][] ITEMS = {
        { 1, 10  },  // Hat
        { 2, 15  },  // Sunglasses
        { 3, 20  },  // Cape
        { 4, 30  },  // Crown
    };
    private static final String[] ITEM_NAMES = { "Chapeu", "Oculos", "Capa", "Coroa" };

    private static final int COLS = 2;

    private int selected = 0;
    private String feedback = "";
    private int feedbackTimer = 0;

    public ShopState(Game game) { this.game = game; }

    // ── Sprite loader ─────────────────────────────────────────────────────
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
        tick = 0;
        feedback = "";
        imgBackground = load("background.png");
        imgFloor      = load("chao.png");
    }

    @Override public void onExit() {}

    @Override
    public void update() {
        if (++tick % 35 == 0) blink = !blink;

        if (game.keys.isJustPressed(KeyEvent.VK_ESCAPE))
            game.setState(new MenuState(game));

        if (game.keys.isJustPressed(KeyEvent.VK_LEFT))
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
        if (game.keys.isJustPressed(KeyEvent.VK_RIGHT))
            selected = (selected + 1) % ITEMS.length;

        if (game.keys.isJustPressed(KeyEvent.VK_UP))
            selected = (selected - COLS + ITEMS.length) % ITEMS.length;
        if (game.keys.isJustPressed(KeyEvent.VK_DOWN))
            selected = (selected + COLS) % ITEMS.length;

        for (int i = 0; i < itemRects.length; i++) {
            if (game.mouse.isJustPressed(MouseHandler.LEFT)
                    && itemRects[i] != null
                    && itemRects[i].contains(game.mouse.getPosition())) {
                selected = i;
            }
        }

        if (game.keys.isJustPressed(KeyEvent.VK_ENTER)
                || game.keys.isJustPressed(KeyEvent.VK_SPACE)) {
            buyOrEquip(selected);
        }

        if (feedbackTimer > 0) feedbackTimer--;
    }

    private void buyOrEquip(int idx) {
        int skinId = ITEMS[idx][0];
        int price  = ITEMS[idx][1];

        if (game.equippedSkin == skinId) {
            feedback = "Ja equipado!";
        } else if (isOwned(skinId)) {
            game.equippedSkin = skinId;
            feedback = "Equipado: " + ITEM_NAMES[idx];
        } else if (game.coins >= price) {
            game.coins -= price;
            setOwned(skinId);
            game.equippedSkin = skinId;
            feedback = "Comprado e equipado!";
        } else {
            feedback = "Moedas insuficientes!";
        }
        feedbackTimer = 120;
    }

    private boolean isOwned(int skinId) { return (game.ownedSkins & (1 << skinId)) != 0; }
    private void setOwned(int skinId)   { game.ownedSkins |= (1 << skinId); }

    private final Rectangle[] itemRects = new Rectangle[ITEMS.length];

    @Override
    public void render(Graphics2D g, float alpha) {
        int cx        = game.width / 2;
        int groundTop = game.height - PlayState.GROUND_H;

        // ── Background sprite ─────────────────────────────────────────────
        g.drawImage(imgBackground, 0, 0, game.width, groundTop, null);

        // ── Floor sprite ──────────────────────────────────────────────────
        g.drawImage(imgFloor, 0, groundTop, game.width, game.height, null);

        // ── Title ─────────────────────────────────────────────────────────
        String title = "LOJA";
        g.setFont(new Font("Arial", Font.BOLD, 44));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(80, 40, 0));
        g.drawString(title, cx - fm.stringWidth(title) / 2 + 3, 63);
        g.setColor(new Color(255, 220, 0));
        g.drawString(title, cx - fm.stringWidth(title) / 2, 60);

        // ── Wallet ────────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(new Color(255, 220, 0));
        String wallet = "$ " + game.coins;
        g.drawString(wallet, cx - g.getFontMetrics().stringWidth(wallet) / 2, 100);

        // ── 2×2 Grid Items ────────────────────────────────────────────────
        int itemW  = 110, itemH  = 130;
        int colGap = 24,  rowGap = 24;
        int rows   = (int) Math.ceil((double) ITEMS.length / COLS);

        int totalW = COLS * itemW + (COLS - 1) * colGap;
        int totalH = rows  * itemH + (rows  - 1) * rowGap;

        int startX = cx - totalW / 2;
        int startY = 120;

        for (int i = 0; i < ITEMS.length; i++) {
            int col = i % COLS;
            int row = i / COLS;

            int skinId = ITEMS[i][0];
            int price  = ITEMS[i][1];
            int x = startX + col * (itemW + colGap);
            int y = startY + row * (itemH + rowGap);

            itemRects[i] = new Rectangle(x, y, itemW, itemH);

            boolean isSel    = (i == selected);
            boolean owned    = isOwned(skinId);
            boolean equipped = game.equippedSkin == skinId;

            g.setColor(isSel ? new Color(255, 240, 180) : new Color(255, 255, 255, 60));
            g.fillRoundRect(x, y, itemW, itemH, 12, 12);
            g.setColor(isSel ? new Color(200, 140, 0) : new Color(255, 255, 255, 120));
            g.drawRoundRect(x, y, itemW, itemH, 12, 12);

            int bpx = x + itemW / 2 - PlayState.BIRD_W / 2;
            int bpy = y + 20;
            g.setColor(new Color(255, 200, 0));
            g.fillRect(bpx, bpy, PlayState.BIRD_W, PlayState.BIRD_H);
            drawSkinPreview(g, skinId, bpx, bpy);

            g.setFont(new Font("Arial", Font.BOLD, 14));
            fm = g.getFontMetrics();
            g.setColor(isSel ? new Color(80, 40, 0) : Color.WHITE);
            String name = ITEM_NAMES[i];
            g.drawString(name, x + itemW / 2 - fm.stringWidth(name) / 2, y + 75);

            g.setFont(new Font("Arial", Font.PLAIN, 13));
            fm = g.getFontMetrics();
            String status = equipped ? "EQUIPADO" : owned ? "Equipar" : "$ " + price;
            g.setColor(equipped ? new Color(100, 220, 80)
                     : owned    ? new Color(150, 220, 255)
                     : (game.coins >= price ? new Color(255, 220, 0) : new Color(200, 80, 80)));
            g.drawString(status, x + itemW / 2 - fm.stringWidth(status) / 2, y + 95);

            if (isSel) {
                g.setColor(new Color(255, 220, 0));
                g.setFont(new Font("Arial", Font.PLAIN, 20));
                g.drawString("V", x + itemW / 2 - 8, y - 6);
            }
        }

        // ── Controls hint ─────────────────────────────────────────────────
        int gridBottom = startY + totalH;
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        fm = g.getFontMetrics();
        g.setColor(new Color(220, 220, 220));
        String hint = "SETAS selecionar   ENTER comprar/equipar   ESC voltar";
        g.drawString(hint, cx - fm.stringWidth(hint) / 2, gridBottom + 26);

        // ── Feedback ──────────────────────────────────────────────────────
        if (feedbackTimer > 0) {
            float alpha2 = Math.min(1f, feedbackTimer / 30f);
            g.setColor(new Color(255, 255, 100, (int)(alpha2 * 220)));
            g.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g.getFontMetrics();
            g.drawString(feedback, cx - fm.stringWidth(feedback) / 2, gridBottom + 54);
        }

        // ── Back hint ─────────────────────────────────────────────────────
        if (blink) {
            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.setColor(Color.WHITE);
            g.drawString("ESC to back", 20, game.height - 10);
        }
    }

    private void drawSkinPreview(Graphics2D g, int skinId, int bx, int by) {
        switch (skinId) {
            case 1 -> {
                int cx = bx + PlayState.BIRD_W / 2;
                g.setColor(new Color(30, 20, 10));
                g.fillRect(cx - 10, by - 4,  20, 4);
                g.fillRect(cx - 6,  by - 14, 12, 11);
                g.setColor(new Color(180, 30, 30));
                g.fillRect(cx - 6,  by - 6,  12, 3);
            }
            case 2 -> {
                int faceX = bx + PlayState.BIRD_W - 10;
                int midY  = by + PlayState.BIRD_H / 2 - 2;
                g.setColor(new Color(20, 20, 20, 200));
                g.fillOval(faceX - 10, midY - 4, 9, 7);
                g.fillOval(faceX,      midY - 4, 9, 7);
                g.setColor(new Color(60, 60, 60));
                g.drawLine(faceX - 1, midY, faceX, midY);
            }
            case 3 -> {
                int tipX = bx - 6;
                int topY = by + 4, botY = by + PlayState.BIRD_H - 4;
                int[] xs = { bx, bx, tipX };
                int[] ys = { topY, botY, (topY + botY) / 2 };
                g.setColor(new Color(180, 30, 200));
                g.fillPolygon(xs, ys, 3);
                g.setColor(new Color(220, 100, 255));
                g.drawPolygon(xs, ys, 3);
            }
            case 4 -> {
                int cx = bx + PlayState.BIRD_W / 2;
                int[] xs = { cx-9, cx-9, cx-3, cx, cx+3, cx+9, cx+9 };
                int[] ys = { by-2, by-10, by-6, by-12, by-6, by-10, by-2 };
                g.setColor(new Color(255, 200, 0));
                g.fillPolygon(xs, ys, 7);
                g.setColor(new Color(200, 140, 0));
                g.drawPolygon(xs, ys, 7);
                g.setColor(new Color(255, 60, 60));
                g.fillOval(cx - 2, by - 12, 4, 4);
            }
        }
    }
}