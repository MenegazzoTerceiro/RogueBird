import java.awt.*;

public class Fireball {

    private double x;
    private double y;

    private double vx;
    private double vy;

    private int size;

    public Fireball(
            double x,
            double y,
            double vx,
            double vy,
            int size) {

        this.x = x;
        this.y = y;

        this.vx = vx;
        this.vy = vy;

        this.size = size;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public void render(Graphics2D g) {

        g.setColor(Color.ORANGE);

        g.fillOval(
                (int)x - size / 2,
                (int)y - size / 2,
                size,
                size);
    }

    public Rectangle getBounds() {

        return new Rectangle(
                (int)x - size / 2,
                (int)y - size / 2,
                size,
                size);
    }
}