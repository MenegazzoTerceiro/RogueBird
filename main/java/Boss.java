import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class Boss {

    public enum AttackState {
        IDLE,
        FIREBALL_ATTACK,
        LASER_AIMING,
        LASER_LOCKED,
        LASER_FIRING,
        DYING
    }

    private int x;
    private int y;

    private int targetX;
    private boolean positioned;

    private int width;
    private int height;
    private int pipeHeight;
    private int pipeWidth;

    private int maxHealth;
    private int currentHealth;
    private int level;

    private AttackState attackState;
    private int stateTimer;
    private int attackCooldown;
    private boolean laserHitApplied;
    private int laserDamage;
    private int fireballDamage;
    private List<Fireball> fireballs;
    private int fireballBurst;
    private int deathSpeed;

    private int targetBirdX;
    private int targetBirdY;
    private double laserAngle;

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getCurrentHealth() {
        return this.currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    private int calculateHealth(int level) {
        return (int) (10 * Math.pow(1.5, level - 1));
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }

    public int getLaserDamage() {
        return laserDamage;
    }

    public int getFireballDamage() {
        return fireballDamage;
    }

    public List<Fireball> getFireball() {
        return fireballs;
    }

    public Boss(int x, int y, int width, int height, int targetX, int pipeHeight, int level) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.maxHealth = calculateHealth(level);
        this.currentHealth = maxHealth;

        this.targetX = targetX;
        this.pipeHeight = pipeHeight;
        this.pipeWidth = width + 20;
        this.level = level;
        this.positioned = false;

        attackState = AttackState.IDLE;
        stateTimer = 0;
        attackCooldown = 180;
        laserHitApplied = false;
        this.laserDamage = calculateLaserDamage(level);
        this.fireballDamage = calculateFireballDamage(level);
        fireballs = new ArrayList<>();
        this.deathSpeed = 0;

        System.out.println("BOSS COMPILADO NOVAMENTE");
    }

    private void updateAttackState(int birdX, int birdY) {

        stateTimer++;

        switch (attackState) {

            case IDLE:

                if (attackCooldown > 0) {
                    attackCooldown--;
                    break;
                }

                if (Math.random() < 0.5) {
                    attackState = AttackState.LASER_AIMING;
                } else {
                    attackState = AttackState.FIREBALL_ATTACK;
                }

                fireballBurst = 0;
                stateTimer = 0;

                System.out.println("Novo ataque: " + attackState);

                break;

            case FIREBALL_ATTACK:

                if (stateTimer == 1
                        || stateTimer == 10
                        || stateTimer == 20
                        || stateTimer == 50
                        || stateTimer == 60
                        || stateTimer == 70
                        || stateTimer == 100
                        || stateTimer == 110
                        || stateTimer == 120) {

                    shootFireball(
                            birdX,
                            birdY);
                }

                if (stateTimer >= 120) {

                    attackState = AttackState.IDLE;

                    attackCooldown = 100;
                    stateTimer = 0;
                }

                break;

            case LASER_AIMING:

                targetBirdX = birdX;
                targetBirdY = birdY;

                int bossCenterX = x + width / 2;
                int bossCenterY = y + height / 2;

                laserAngle = Math.atan2(
                        targetBirdY - bossCenterY,
                        targetBirdX - bossCenterX);

                if (stateTimer >= 120) {

                    attackState = AttackState.LASER_LOCKED;

                    stateTimer = 0;

                    System.out.println("Laser travado");
                }

                break;

            case LASER_LOCKED:

                if (stateTimer >= 15) { // 0,5 segundo
                    attackState = AttackState.LASER_FIRING;
                    laserHitApplied = false;
                    stateTimer = 0;
                    System.out.println("Laser disparado");
                }

                break;

            case LASER_FIRING:

                if (stateTimer >= 10) { // ~0,16 segundo

                    attackState = AttackState.IDLE;

                    attackCooldown = 180;
                    stateTimer = 0;

                    System.out.println("Fim do laser");
                }

                break;

            case DYING:
                deathSpeed += 1;
                y += deathSpeed;
                break;

            default:
                break;
        }
    }

    private int calculateLaserDamage(int level) {
        return (int) Math.ceil(3 * Math.pow(1.5, level - 1));
    }

    private int calculateFireballDamage(int level) {
        return (int) Math.ceil(1 * Math.pow(1.5, level - 1));
    }

    private void updateFireballs() {
        for (Fireball fireball : fireballs) {
            fireball.update();
        }
    }

    private void shootFireball(int birdX, int birdY) {
        int startX = x + width / 2;
        int startY = y + height / 2;

        double angle = Math.atan2(birdY - startY, birdX - startX);
        double speed = 10;

        double vx = Math.cos(angle) * speed;
        double vy = Math.sin(angle) * speed;

        fireballs.add(
                new Fireball(
                        startX,
                        startY,
                        vx,
                        vy,
                        20));
    }

    public void updat(int birdX, int birdY) {
        if (!positioned) {
            x -= 2; // Move o boss para a esquerda até atingir a posição alvo
            if (x <= targetX) {
                x = targetX;
                positioned = true;
            }
            return; // Não atualiza o estado de ataque até que o boss esteja posicionado
        }

        updateAttackState(birdX, birdY);
        updateFireballs();
    }

    public boolean hasLaserHit() {
        return laserHitApplied;
    }

    public void markLaserHit() {
        laserHitApplied = true;
    }

    public boolean isLaserFiring() {
        return attackState == AttackState.LASER_FIRING;
    }

    public Shape getLaserHitbox() {

        if (attackState != AttackState.LASER_FIRING) {
            return null;
        }

        int startX = x + width / 2;
        int startY = y + height / 2;

        int endX = (int) (startX + Math.cos(laserAngle) * 1000);
        int endY = (int) (startY + Math.sin(laserAngle) * 1000);

        Line2D laserLine = new Line2D.Double(
                startX,
                startY,
                endX,
                endY);

        return new BasicStroke(50).createStrokedShape(laserLine);
    }


    public void render(Graphics2D g) {

        // Cano
        int pipeX = x - (pipeWidth - width) / 2;
        g.setColor(new Color(80, 180, 60));

        g.fillRect(
                pipeX,
                y + height,
                pipeWidth,
                pipeHeight);

        // mira do laser
        if (attackState == AttackState.LASER_AIMING || attackState == AttackState.LASER_LOCKED) {

            g.setColor(Color.RED);

            int startX = x + width / 2;
            int startY = y + height / 2;

            int endX = (int) (startX + Math.cos(laserAngle) * 1000);
            int endY = (int) (startY + Math.sin(laserAngle) * 1000);

            g.drawLine(
                    startX,
                    startY,
                    endX,
                    endY);
        }

        // laser disparado
        if (attackState == AttackState.LASER_FIRING) {

            int startX = x + width / 2;
            int startY = y + height / 2;

            int endX = (int) (startX + Math.cos(laserAngle) * 1000);
            int endY = (int) (startY + Math.sin(laserAngle) * 1000);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setStroke(new BasicStroke(50));
            g2.setColor(new Color(255, 120, 120));

            g2.drawLine(
                    startX,
                    startY,
                    endX,
                    endY);

            g2.setStroke(new BasicStroke(30));
            g2.setColor(Color.WHITE);

            g2.drawLine(
                    startX,
                    startY,
                    endX,
                    endY);

            g2.dispose();
        }

        // bolas de fogo
        for (Fireball fireball : fireballs) {
            fireball.render(g);
        }

        // Corpo do boss
        g.setColor(Color.RED);

        g.fillRect(
                x,
                y,
                width,
                height);

    }

    public void takeDamage(int damage) {
        if (damage <= 0) {
            return;
        }

        currentHealth -= damage;

        if (currentHealth < 0) {
            currentHealth = 0;
        }

        if (currentHealth <= 0 && attackState != AttackState.DYING) {

            attackState = AttackState.DYING;
            stateTimer = 0;
            System.out.println("Boss morreu");
        }
    }

    public boolean hasFinishedDeathAnimation() {
        return attackState == AttackState.DYING && y > 800;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

}