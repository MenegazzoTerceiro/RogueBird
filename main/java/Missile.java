import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Missile {

    private float x, y;
    private float mSpeed;
    private boolean activeAlert;
    private int alertTimer;
    private BufferedImage sprite;


    Missile(float birdY, int width, int score){
        x = width+20;
        y = birdY;
        mSpeed = 10 + (score/10);
        activeAlert = true;
        alertTimer = 20;
        Math.min(mSpeed, 23);
        try {
        sprite = ImageIO.read(getClass().getResource("/recursos/sprites/Missile.png"));
        } catch (IOException e) {
        e.printStackTrace();
        }
    }
    

    void update(){
        if(activeAlert){
            alertTimer--;

            if(alertTimer <= 0){
                activeAlert = false;
                Sound.play("missile-launch");
            }
        }

        if(!activeAlert){
            x -= mSpeed;
        }
        
    }

    void render(Graphics2D g){

        if(activeAlert){
            g.setColor(Color.RED);
            g.fillRect((int)x-50, (int)y, 30, 30);
        }

        if(!activeAlert){
            g.drawImage(sprite, (int)x, (int)y, 40, 20, null);
        }

    }

    Rectangle getBounds(){
        return new Rectangle((int)x, (int)y, 40, 20);
    }
}
