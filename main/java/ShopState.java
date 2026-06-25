import java.awt.*;
import java.awt.event.KeyEvent;

public class ShopState implements GameState {

    private final Game game;
    private boolean blink = true;
    private int tick;

    // ── Shop items: { skinId, price } ────────────────────────────────────
    private static final int[][] ITEMS = {
        { 1, 10  },  // Hat
        { 2, 15  },  // Sunglasses
        { 3, 20  },  // Cape
        { 4, 30  },  // Crown
    };
    private static final String[] ITEM_NAMES = { "Chapéu", "Óculos", "Capa", "Coroa" };

    private int selected = 0;
    private String feedback = "";
    private int feedbackTimer = 0;

    public ShopState(Game game) { this.game = game; }

    @Override
    public void onEnter() { tick = 0; feedback = ""; }

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

        // Also allow mouse click on item rectangles (handled in render via stored rects)
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
            feedback = "Já equipado!";
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

    // ── Simple owned-set using a bitmask stored in game ──────────────────
    // You'll need: public int ownedSkins = 0; in Game.java
    private boolean isOwned(int skinId) {
        return (game.ownedSkins & (1 << skinId)) != 0;
    }
    private void setOwned(int skinId) {
        game.ownedSkins |= (1 << skinId);
    }

    // Stored each frame so mouse clicks can reference them
    private final Rectangle[] itemRects = new Rectangle[ITEMS.length];

    @Override
    public void render(Graphics2D g, float alpha) {
        int cx = game.width / 2;
        int groundTop = game.height - PlayState.GROUND_H;

        // ── Background ────────────────────────────────────────────────────
        g.setColor(new Color(80, 180, 240));
        g.fillRect(0, 0, game.width, game.height);

        g.setColor(new Color(210, 170, 80));
        g.fillRect(0, groundTop, game.width, PlayState.GROUND_H);

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

        // ── Items ─────────────────────────────────────────────────────────
        int itemW = 100, itemH = 130, gap = 20;
        int totalW = ITEMS.length * itemW + (ITEMS.length - 1) * gap;
        int startX = cx - totalW / 2;
        int itemY = 130;

        for (int i = 0; i < ITEMS.length; i++) {
            int skinId = ITEMS[i][0];
            int price  = ITEMS[i][1];
            int x = startX + i * (itemW + gap);

            itemRects[i] = new Rectangle(x, itemY, itemW, itemH);

            boolean isSel   = (i == selected);
            boolean owned   = isOwned(skinId);
            boolean equipped = game.equippedSkin == skinId;

            // Card background
            g.setColor(isSel ? new Color(255, 240, 180) : new Color(255, 255, 255, 60));
            g.fillRoundRect(x, itemY, itemW, itemH, 12, 12);
            g.setColor(isSel ? new Color(200, 140, 0) : new Color(255, 255, 255, 120));
            g.drawRoundRect(x, itemY, itemW, itemH, 12, 12);

            // Bird preview
            int bpx = x + itemW / 2 - PlayState.BIRD_W / 2;
            int bpy = itemY + 20;
            g.setColor(new Color(255, 200, 0));
            g.fillRect(bpx, bpy, PlayState.BIRD_W, PlayState.BIRD_H);
            drawSkinPreview(g, skinId, bpx, bpy);

            // Item name
            g.setFont(new Font("Arial", Font.BOLD, 14));
            fm = g.getFontMetrics();
            g.setColor(isSel ? new Color(80, 40, 0) : Color.WHITE);
            String name = ITEM_NAMES[i];
            g.drawString(name, x + itemW / 2 - fm.stringWidth(name) / 2, itemY + 75);

            // Price or status
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            fm = g.getFontMetrics();
            String status = equipped ? "EQUIPADO" : owned ? "Equipar" : "$ " + price;
            g.setColor(equipped ? new Color(100, 220, 80)
                     : owned    ? new Color(150, 220, 255)
                     : (game.coins >= price ? new Color(255, 220, 0) : new Color(200, 80, 80)));
            g.drawString(status, x + itemW / 2 - fm.stringWidth(status) / 2, itemY + 95);
        }

        // ── Selection arrow ───────────────────────────────────────────────
        int selX = startX + selected * (itemW + gap) + itemW / 2;
        g.setColor(new Color(255, 220, 0));
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("▼", selX - 8, itemY - 6);

        // ── Controls hint ─────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        fm = g.getFontMetrics();
        g.setColor(new Color(220, 220, 220));
        String hint = "◀ ▶ selecionar   ENTER comprar/equipar   ESC voltar";
        g.drawString(hint, cx - fm.stringWidth(hint) / 2, groundTop - 20);

        // ── Feedback ──────────────────────────────────────────────────────
        if (feedbackTimer > 0) {
            float alpha2 = Math.min(1f, feedbackTimer / 30f);
            g.setColor(new Color(255, 255, 100, (int)(alpha2 * 220)));
            g.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g.getFontMetrics();
            g.drawString(feedback, cx - fm.stringWidth(feedback) / 2, itemY + itemH + 40);
        }

        // ── Back hint ─────────────────────────────────────────────────────
        if (blink) {
            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.setColor(Color.WHITE);
            g.drawString("ESC to back", 20, game.height - 10);
        }
    }

    /** Draws just the accessory for the shop preview (no rotation context needed). */
    private void drawSkinPreview(Graphics2D g, int skinId, int bx, int by) {
        switch (skinId) {
            case 1 -> { // Hat
                int cx = bx + PlayState.BIRD_W / 2;
                g.setColor(new Color(30, 20, 10));
                g.fillRect(cx - 10, by - 4, 20, 4);
                g.fillRect(cx - 6,  by - 14, 12, 11);
                g.setColor(new Color(180, 30, 30));
                g.fillRect(cx - 6,  by - 6,  12, 3);
            }
            case 2 -> { // Sunglasses
                int faceX = bx + PlayState.BIRD_W - 10;
                int midY  = by + PlayState.BIRD_H / 2 - 2;
                g.setColor(new Color(20, 20, 20, 200));
                g.fillOval(faceX - 10, midY - 4, 9, 7);
                g.fillOval(faceX,      midY - 4, 9, 7);
                g.setColor(new Color(60, 60, 60));
                g.drawLine(faceX - 1, midY, faceX, midY);
            }
            case 3 -> { // Cape
                int tipX = bx - 6;
                int topY = by + 4, botY = by + PlayState.BIRD_H - 4;
                int[] xs = { bx, bx, tipX };
                int[] ys = { topY, botY, (topY + botY) / 2 };
                g.setColor(new Color(180, 30, 200));
                g.fillPolygon(xs, ys, 3);
                g.setColor(new Color(220, 100, 255));
                g.drawPolygon(xs, ys, 3);
            }
            case 4 -> { // Crown
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