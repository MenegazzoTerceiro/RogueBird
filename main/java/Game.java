import java.awt.*;
import java.awt.image.BufferStrategy;
import javax.swing.*;

public class Game implements Runnable {

    // ── Config ─────────────────────────────────────────────────────────────
    public final int width, height;
    private static final int FPS = 60;
    private static final long NS_PER_TICK = 1_000_000_000L / FPS;

    // ── Window ─────────────────────────────────────────────────────────────
    private final JFrame window;
    private final Canvas canvas;

    // ── Input ──────────────────────────────────────────────────────────────
    public final KeyHandler   keys;
    public final MouseHandler mouse;

    // ── States ─────────────────────────────────────────────────────────────
    private GameState current, pending;
   public int coins     = 0;   // persistent wallet
    public int ownedSkins = 0;  // bitmask of purchased skin IDs
    public int equippedSkin = 0; // 0=none, 1=hat, 2=sunglasses, 3=cape, 4=crown
    // ── Loop ───────────────────────────────────────────────────────────────
    private boolean running;

    public Game(String title, int width, int height) {
        this.width  = width;
        this.height = height;

        keys  = new KeyHandler();
        mouse = new MouseHandler();

        // Canvas (drawing surface)
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);
        canvas.addKeyListener(keys);
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);

        // Window
        window = new JFrame(title);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(canvas);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        canvas.requestFocusInWindow();

        // Primeiro estado
        setState(new MenuState(this));

        running = true;
        new Thread(this, "GameLoop").start();
    }

    // ── State API ──────────────────────────────────────────────────────────
    public void setState(GameState next) { pending = next; }

    // ── Loop ───────────────────────────────────────────────────────────────
    @Override
    public void run() {
        long last = System.nanoTime(), lag = 0;

        while (running) {
            long now = System.nanoTime();
            lag += now - last;
            last = now;

            keys.update();

            // Apply pending state transition
            if (pending != null) {
                if (current != null) current.onExit();
                current = pending;
                pending = null;
                current.onEnter();
            }

            while (lag >= NS_PER_TICK) {
                if (current != null) current.update();
                lag -= NS_PER_TICK;
            }

            render((float) lag / NS_PER_TICK);

            long sleep = (NS_PER_TICK - (System.nanoTime() - now)) / 1_000_000;
            if (sleep > 1) try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────
    private void render(float alpha) {
        if (canvas.getBufferStrategy() == null) {
            canvas.createBufferStrategy(2);
            return;
        }
        BufferStrategy bs = canvas.getBufferStrategy();
        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, width, height);
                    if (current != null) current.render(g, alpha);
                } finally {
                    g.dispose();
                }
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
    }
}
