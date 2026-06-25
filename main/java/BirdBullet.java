import java.awt.*;

public class BirdBullet {

    private float x;
    private float y;

    private float speed;

    BirdBullet(float x, float y) {

        this.x = x;
        this.y = y;

        speed = 12;
    }

    void update() {
        x += speed;
    }

    void render(Graphics2D g) {

        g.setColor(Color.YELLOW);

        g.fillRect(
                (int) x,
                (int) y,
                12,
                3);
    }

    Rectangle getBounds() {

        return new Rectangle(
                (int) x,
                (int) y,
                12,
                3);
    }

    boolean isOffScreen(int screenWidth) {
        return x > screenWidth;
    }
}
