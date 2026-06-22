package edu.psu.hurtak;

/**
 * Project: Solo Lab 5 Assignment
 * Purpose Details: Creates a beginner-friendly Java Swing space game with stars,
 * player image, score, obstacles, sound, shield, health, power-ups, timer, levels,
 * shield recharge bar, and purple orb extra shots.
 * Course: IST 242
 * Author: Alexander Matthew Hurtak
 * Date Developed: 06/16/2026
 * Last Date Changed: 06/21/2026
 * Rev: 4
 */

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpaceGame extends JFrame implements KeyListener {

    private static final int WIDTH = 700;
    private static final int HEIGHT = 700;

    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 40;

    private static final int OBSTACLE_WIDTH = 32;
    private static final int OBSTACLE_HEIGHT = 32;

    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 15;

    private static final int PLAYER_SPEED = 15;
    private static final int PROJECTILE_SPEED = 20;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private Timer timer;
    private Random random;

    private int score;
    private int health;
    private int level;
    private int timeLeft;
    private int tickCounter;
    private int extraShots;

    private int playerX;
    private int playerY;

    private boolean firing;
    private boolean gameOver;
    private boolean shieldActive;

    private long shieldStartTime;
    private long lastShieldUseTime;

    private final long shieldDuration = 3000;
    private final long shieldRechargeTime = 7000;

    private List<Point> stars;
    private List<Point> obstacles;
    private List<Point> healthPowerUps;
    private List<Point> purpleOrbs;
    private List<Point> projectiles;

    private BufferedImage shipImage;
    private BufferedImage obstacleSheet;
    private BufferedImage healthImage;

    /**
     * Constructor sets up the game window, images, labels, variables, and timer.
     */
    public SpaceGame() {
        setTitle("Lab5Hurtak Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        random = new Random();

        loadImages();

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        gamePanel.setLayout(null);
        gamePanel.setBackground(Color.BLACK);

        scoreLabel = new JLabel();
        scoreLabel.setForeground(Color.BLUE);
        scoreLabel.setBounds(10, 10, 650, 25);
        gamePanel.add(scoreLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        resetGame();

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
                gamePanel.repaint();
            }
        });

        timer.start();
    }

    private void loadImages() {
        try {
            shipImage = ImageIO.read(getClass().getResource("/assets/ship.png"));
            obstacleSheet = ImageIO.read(getClass().getResource("/assets/obstacles.png"));
            healthImage = ImageIO.read(getClass().getResource("/assets/health.png"));
        } catch (Exception e) {
            System.out.println("Image file missing. The game will still run.");
        }
    }

    private void resetGame() {
        score = 0;
        health = 3;
        level = 1;
        timeLeft = 90;
        tickCounter = 0;
        extraShots = 0;

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 70;

        firing = false;
        gameOver = false;
        shieldActive = false;

        shieldStartTime = 0;
        lastShieldUseTime = System.currentTimeMillis() - shieldRechargeTime;

        stars = generateStars(200);
        obstacles = new ArrayList<>();
        healthPowerUps = new ArrayList<>();
        purpleOrbs = new ArrayList<>();
        projectiles = new ArrayList<>();

        updateScoreLabel();
    }

    private List<Point> generateStars(int numberOfStars) {
        List<Point> starList = new ArrayList<>();

        for (int i = 0; i < numberOfStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starList.add(new Point(x, y));
        }

        return starList;
    }

    private Color generateRandomColor() {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        return new Color(red, green, blue);
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (Point star : stars) {
            g.setColor(generateRandomColor());
            g.fillOval(star.x, star.y, 2, 2);
        }

        if (shipImage != null) {
            g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        } else {
            g.setColor(Color.BLUE);
            g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        if (shieldActive) {
            g.setColor(new Color(0, 200, 255, 100));
            g.fillOval(playerX - 10, playerY - 10, PLAYER_WIDTH + 20, PLAYER_HEIGHT + 20);
        }

        for (Point projectile : projectiles) {
            g.setColor(Color.GREEN);
            g.fillRect(projectile.x, projectile.y, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        for (Point obstacle : obstacles) {
            drawObstacle(g, obstacle);
        }

        for (Point powerUp : healthPowerUps) {
            if (healthImage != null) {
                g.drawImage(healthImage, powerUp.x, powerUp.y, 25, 25, null);
            } else {
                g.setColor(Color.PINK);
                g.fillOval(powerUp.x, powerUp.y, 25, 25);
            }
        }

        for (Point orb : purpleOrbs) {
            g.setColor(Color.MAGENTA);
            g.fillOval(orb.x, orb.y, 25, 25);

            g.setColor(Color.WHITE);
            g.drawOval(orb.x, orb.y, 25, 25);
        }

        drawShieldRechargeBar(g);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 28));

            if (health > 0 && timeLeft <= 0) {
                g.setColor(Color.GREEN);
                g.drawString("You Win!", 285, 330);
            } else {
                g.setColor(Color.WHITE);
                g.drawString("Game Over!", 270, 330);
            }

            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press ESC to reset", 285, 360);
        }
    }

    private void drawShieldRechargeBar(Graphics g) {
        int barX = 10;
        int barY = 40;
        int barWidth = 150;
        int barHeight = 15;

        double percentReady = getShieldRechargePercent();

        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);

        g.setColor(Color.CYAN);
        g.fillRect(barX, barY, (int) (barWidth * percentReady), barHeight);

        g.setColor(Color.WHITE);

        if (percentReady >= 1.0) {
            g.drawString("Shield Ready", barX + 160, barY + 13);
        } else {
            g.drawString("Shield Recharging", barX + 160, barY + 13);
        }
    }

    private double getShieldRechargePercent() {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastShieldUseTime;

        double percent = (double) timePassed / shieldRechargeTime;

        if (percent > 1.0) {
            percent = 1.0;
        }

        return percent;
    }

    private void drawObstacle(Graphics g, Point obstacle) {
        if (obstacleSheet != null) {
            int spriteWidth = obstacleSheet.getWidth() / 4;
            int spriteHeight = obstacleSheet.getHeight();

            int spriteIndex = Math.abs(obstacle.x + obstacle.y) % 4;
            int spriteX = spriteIndex * spriteWidth;

            BufferedImage sprite = obstacleSheet.getSubimage(spriteX, 0, spriteWidth, spriteHeight);
            g.drawImage(sprite, obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        }
    }

    private void update() {
        if (gameOver) {
            return;
        }

        tickCounter++;

        if (tickCounter >= 50) {
            tickCounter = 0;
            timeLeft--;

            if (timeLeft <= 60 && level < 2) {
                level = 2;
            }

            if (timeLeft <= 45 && level < 3) {
                level = 3;
            }

            if (timeLeft <= 0) {
                gameOver = true;
            }
        }

        moveStars();
        moveObstacles();
        createObstacles();
        movePowerUps();
        createPowerUps();
        movePurpleOrbs();
        createPurpleOrbs();
        moveProjectiles();
        checkPlayerCollisions();
        checkProjectileCollisions();
        checkPowerUpCollisions();
        checkPurpleOrbCollisions();
        checkShieldTimer();

        updateScoreLabel();
    }

    private void moveStars() {
        for (Point star : stars) {
            star.y += level;

            if (star.y > HEIGHT) {
                star.y = 0;
                star.x = random.nextInt(WIDTH);
            }
        }
    }

    private void moveObstacles() {
        int speed;

        if (level == 1) {
            speed = 4;
        } else if (level == 2) {
            speed = 7;
        } else {
            speed = 12;
        }

        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += speed;

            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                i--;
            }
        }
    }

    private void createObstacles() {
        double chance;

        if (level == 1) {
            chance = 0.025;
        } else if (level == 2) {
            chance = 0.045;
        } else {
            chance = 0.090;
        }

        if (random.nextDouble() < chance) {
            int x = random.nextInt(WIDTH - OBSTACLE_WIDTH);
            obstacles.add(new Point(x, 0));
        }

        if (level == 3 && random.nextDouble() < 0.050) {
            int x = random.nextInt(WIDTH - OBSTACLE_WIDTH);
            obstacles.add(new Point(x, 0));
        }
    }

    private void movePowerUps() {
        for (int i = 0; i < healthPowerUps.size(); i++) {
            healthPowerUps.get(i).y += 2;

            if (healthPowerUps.get(i).y > HEIGHT) {
                healthPowerUps.remove(i);
                i--;
            }
        }
    }

    private void createPowerUps() {
        if (random.nextDouble() < 0.003) {
            int x = random.nextInt(WIDTH - 25);
            healthPowerUps.add(new Point(x, 0));
        }
    }

    private void movePurpleOrbs() {
        for (int i = 0; i < purpleOrbs.size(); i++) {
            purpleOrbs.get(i).y += 3;

            if (purpleOrbs.get(i).y > HEIGHT) {
                purpleOrbs.remove(i);
                i--;
            }
        }
    }

    private void createPurpleOrbs() {
        if (random.nextDouble() < 0.004) {
            int x = random.nextInt(WIDTH - 25);
            purpleOrbs.add(new Point(x, 0));
        }
    }

    private void moveProjectiles() {
        for (int i = 0; i < projectiles.size(); i++) {
            projectiles.get(i).y -= PROJECTILE_SPEED;

            if (projectiles.get(i).y < 0) {
                projectiles.remove(i);
                i--;
            }
        }
    }

    private void checkPlayerCollisions() {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(
                    obstacles.get(i).x,
                    obstacles.get(i).y,
                    OBSTACLE_WIDTH,
                    OBSTACLE_HEIGHT
            );

            if (playerRect.intersects(obstacleRect)) {
                if (!shieldActive) {
                    health--;
                    playSound("/assets/collision.wav");
                }

                obstacles.remove(i);
                i--;

                if (health <= 0) {
                    gameOver = true;
                }
            }
        }
    }

    private void checkProjectileCollisions() {
        for (int p = 0; p < projectiles.size(); p++) {
            Rectangle projectileRect = new Rectangle(
                    projectiles.get(p).x,
                    projectiles.get(p).y,
                    PROJECTILE_WIDTH,
                    PROJECTILE_HEIGHT
            );

            for (int o = 0; o < obstacles.size(); o++) {
                Rectangle obstacleRect = new Rectangle(
                        obstacles.get(o).x,
                        obstacles.get(o).y,
                        OBSTACLE_WIDTH,
                        OBSTACLE_HEIGHT
                );

                if (projectileRect.intersects(obstacleRect)) {
                    obstacles.remove(o);
                    projectiles.remove(p);
                    score += 10;
                    p--;
                    break;
                }
            }
        }
    }

    private void checkPowerUpCollisions() {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int i = 0; i < healthPowerUps.size(); i++) {
            Rectangle powerRect = new Rectangle(
                    healthPowerUps.get(i).x,
                    healthPowerUps.get(i).y,
                    25,
                    25
            );

            if (playerRect.intersects(powerRect)) {
                health++;

                if (health > 5) {
                    health = 5;
                }

                healthPowerUps.remove(i);
                i--;
            }
        }
    }

    private void checkPurpleOrbCollisions() {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int i = 0; i < purpleOrbs.size(); i++) {
            Rectangle orbRect = new Rectangle(
                    purpleOrbs.get(i).x,
                    purpleOrbs.get(i).y,
                    25,
                    25
            );

            if (playerRect.intersects(orbRect)) {
                if (extraShots < 5) {
                    extraShots++;
                }

                purpleOrbs.remove(i);
                i--;
            }
        }
    }

    private void checkShieldTimer() {
        if (shieldActive) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - shieldStartTime > shieldDuration) {
                shieldActive = false;
            }
        }
    }

    private void activateShield() {
        if (getShieldRechargePercent() >= 1.0 && !shieldActive) {
            shieldActive = true;
            shieldStartTime = System.currentTimeMillis();
            lastShieldUseTime = System.currentTimeMillis();
        }
    }

    private void updateScoreLabel() {
        scoreLabel.setText("Score: " + score
                + "   Health: " + health
                + "   Time: " + timeLeft
                + "   Level: " + level
                + "   Extra Shots: " + extraShots);
    }

    private void playSound(String soundPath) {
        try {
            InputStream inputStream = getClass().getResourceAsStream(soundPath);

            if (inputStream == null) {
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
            Clip clip = AudioSystem.getClip();

            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.out.println("Sound could not play.");
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_SPACE && !firing) {
            fireProjectile();
        } else if (keyCode == KeyEvent.VK_CONTROL) {
            activateShield();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            resetGame();
        }
    }

    private void fireProjectile() {
        firing = true;

        int centerShotX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        int shotY = playerY;

        projectiles.add(new Point(centerShotX, shotY));

        for (int i = 0; i < extraShots; i++) {
            int extraX = centerShotX + ((i + 1) * 12);

            if (i % 2 == 1) {
                extraX = centerShotX - ((i + 1) * 12);
            }

            projectiles.add(new Point(extraX, shotY));
        }

        playSound("/assets/fire.wav");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    firing = false;
                } catch (InterruptedException e) {
                    System.out.println("Firing thread interrupted.");
                }
            }
        }).start();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SpaceGame().setVisible(true);
            }
        });
    }
}