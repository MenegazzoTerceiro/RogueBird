public class Bird {

    private int maxHealth;
    private int currentHealth;

    private int maxShieldUses;
    private int shieldUses;

    private int shieldHealth;
    private int maxShieldHealth;

    private boolean shieldActive;

    // atributos futuros da loja
    private int bulletDamage;
    private float bulletSpeed;

    public Bird() {

        maxHealth = 8;
        currentHealth = maxHealth;

        maxShieldUses = 3;
        shieldUses = maxShieldUses;

        maxShieldHealth = 3;
        shieldHealth = maxShieldHealth;

        shieldActive = false;

        bulletDamage = 1;
        bulletSpeed = 12f;
    }

    public void takeDamage(int damage) {

        if (shieldActive) {

            if (damage >= shieldHealth) {

                int remainingDamage = damage - shieldHealth;

                shieldHealth = 0;
                shieldActive = false;

                currentHealth -= remainingDamage;

            } else {

                shieldHealth -= damage;
            }

        } else {

            currentHealth -= damage;
        }

        if (currentHealth < 0) {
            currentHealth = 0;
        }
    }

    public void activateShield() {

        if (shieldUses <= 0) {
            return;
        }

        if (shieldActive) {
            return;
        }

        shieldUses--;

        shieldHealth = maxShieldHealth;

        shieldActive = true;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getShieldUses() {
        return shieldUses;
    }

    public boolean isShieldActive() {
        return shieldActive;
    }

    public int getShieldHealth() {
        return shieldHealth;
    }

    public int getBulletDamage() {
        return bulletDamage;
    }

    public float getBulletSpeed() {
        return bulletSpeed;
    }

    public int getMaxShieldhealth() {
        return maxShieldHealth;
    }
}