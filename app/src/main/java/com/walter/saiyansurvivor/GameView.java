package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    private static final int TYPE_KI = 0;
    private static final int TYPE_KAME = 1;
    private static final int TYPE_DISC = 2;
    private static final int TYPE_BOMB = 3;

    private static final int ENEMY_SAIBAMAN = 0;
    private static final int ENEMY_SOLDIER = 1;
    private static final int ENEMY_ANDROID = 2;
    private static final int ENEMY_MAJIN = 3;
    private static final int ENEMY_BOSS = 4;

    private final Random rng = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ui = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Orb> orbs = new ArrayList<>();
    private final ArrayList<Chest> chests = new ArrayList<>();
    private final ArrayList<Floater> floaters = new ArrayList<>();
    private final ArrayList<Explosion> explosions = new ArrayList<>();
    private final ArrayList<UpgradeOption> choices = new ArrayList<>();

    private LinearGradient skyGradient;
    private float px, py;
    private float joyX, joyY, joyBaseX, joyBaseY;
    private boolean joyActive = false;
    private boolean initialized = false;
    private boolean gameOver = false;
    private boolean choosingUpgrade = false;

    private long lastFrameNanos;
    private long runStart;
    private long lastSpawn;
    private long lastWaveBanner;
    private long lastBoss;
    private long lastChest;
    private long lastKiShot;
    private long lastKame;
    private long lastDisc;
    private long lastBomb;
    private long bannerUntil;

    private float worldShiftX = 0f;
    private float worldShiftY = 0f;

    private float hp = 120f;
    private float maxHp = 120f;
    private float xp = 0f;
    private float xpNext = 12f;
    private int level = 1;
    private int kills = 0;

    private float moveSpeed = 330f;
    private float damageMultiplier = 1f;
    private float cooldownMultiplier = 1f;
    private float pickupRange = 90f;
    private float armor = 0f;
    private float regenPerSec = 0f;

    private int kiLevel = 1;
    private int kameLevel = 0;
    private int discLevel = 0;
    private int bombLevel = 0;
    private int kaiokenLevel = 0;
    private int senzuLevel = 0;
    private int radarLevel = 0;
    private int armorLevel = 0;
    private int superSaiyanLevel = 0;

    private String banner = "SURVIVE THE WAVE!";
    private int pendingUpgrades = 0;

    public GameView(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        text.setColor(Color.WHITE);
        text.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!initialized) {
            px = w * 0.5f;
            py = h * 0.58f;
            lastFrameNanos = System.nanoTime();
            runStart = System.currentTimeMillis();
            initialized = true;
        }
        skyGradient = new LinearGradient(
                0, 0, 0, h,
                new int[]{Color.rgb(16, 26, 62), Color.rgb(34, 66, 118), Color.rgb(102, 164, 112)},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!initialized) return;

        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        if (!gameOver && !choosingUpgrade) {
            update(dt);
        }
        drawWorld(c);
        invalidate();
    }

    private void update(float dt) {
        long ms = System.currentTimeMillis();

        float len = (float) Math.hypot(joyX, joyY);
        if (len > 0.05f) {
            float normX = joyX / Math.max(1f, len);
            float normY = joyY / Math.max(1f, len);
            float stepX = normX * moveSpeed * dt;
            float stepY = normY * moveSpeed * dt;
            px += stepX;
            py += stepY;
            worldShiftX += stepX * 0.35f;
            worldShiftY += stepY * 0.22f;
        }

        px = clamp(px, 44, getWidth() - 44);
        py = clamp(py, 78, getHeight() - 56);

        if (regenPerSec > 0f) {
            hp = Math.min(maxHp, hp + regenPerSec * dt);
        }

        long elapsed = ms - runStart;
        spawnLogic(ms, elapsed);
        weaponLogic(ms);

        updateProjectiles(dt);
        updateEnemies(dt);
        updateOrbs(dt);
        updateChests();
        updateExplosions(dt);
        updateFloaters(dt);
    }

    private void spawnLogic(long ms, long elapsed) {
        long spawnCd = Math.max(120, 620 - elapsed / 210);
        if (ms - lastSpawn >= spawnCd) {
            spawnEnemy(false, elapsed);
            if (elapsed > 60000 && rng.nextFloat() < 0.22f) spawnEnemy(false, elapsed);
            if (elapsed > 120000 && rng.nextFloat() < 0.18f) spawnEnemy(false, elapsed);
            lastSpawn = ms;
        }

        if (ms - lastWaveBanner > 25000) {
            banner = waveName(elapsed);
            bannerUntil = ms + 1400;
            lastWaveBanner = ms;
        }

        if (elapsed > 35000 && ms - lastBoss > 50000) {
            spawnEnemy(true, elapsed);
            banner = "BOSS APPROACHING!";
            bannerUntil = ms + 1800;
            lastBoss = ms;
        }

        if (ms - lastChest > 24000) {
            chests.add(new Chest(
                    80 + rng.nextFloat() * (getWidth() - 160),
                    110 + rng.nextFloat() * (getHeight() - 220)
            ));
            lastChest = ms;
        }
    }

    private String waveName(long elapsed) {
        if (elapsed < 40000) return "SAIBAMAN SWARM";
        if (elapsed < 85000) return "FRIEZA FORCE RAID";
        if (elapsed < 130000) return "ANDROID ONSLAUGHT";
        return "MAJIN CHAOS";
    }

    private void weaponLogic(long ms) {
        if (ms - lastKiShot >= adjustedCd(Math.max(210, 620 - (kiLevel - 1) * 65))) {
            fireKiBurst();
            lastKiShot = ms;
        }
        if (kameLevel > 0 && ms - lastKame >= adjustedCd(Math.max(1800, 4300 - kameLevel * 420))) {
            fireKamehameha();
            lastKame = ms;
        }
        if (discLevel > 0 && ms - lastDisc >= adjustedCd(Math.max(1200, 3200 - discLevel * 280))) {
            fireDiscVolley();
            lastDisc = ms;
        }
        if (bombLevel > 0 && ms - lastBomb >= adjustedCd(Math.max(2400, 6200 - bombLevel * 420))) {
            fireSpiritBomb();
            lastBomb = ms;
        }
    }

    private long adjustedCd(float base) {
        return (long) Math.max(120f, base * cooldownMultiplier);
    }

    private Enemy nearestEnemy() {
        Enemy bestEnemy = null;
        float best = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            float d = dist(px, py, e.x, e.y);
            if (d < best) {
                best = d;
                bestEnemy = e;
            }
        }
        return bestEnemy;
    }

    private void fireKiBurst() {
        Enemy target = nearestEnemy();
        if (target == null) return;
        float angle = (float) Math.atan2(target.y - py, target.x - px);
        int amount = 1 + (kiLevel >= 3 ? 1 : 0) + (kiLevel >= 5 ? 1 : 0);
        float spreadBase = amount <= 1 ? 0f : 0.14f;
        for (int i = 0; i < amount; i++) {
            float spread = (i - (amount - 1) / 2f) * spreadBase;
            float a = angle + spread;
            float dmg = (10f + kiLevel * 4f) * damageMultiplier;
            float speed = 720f + kiLevel * 40f;
            float radius = 9f + kiLevel * 1.5f;
            projectiles.add(Projectile.make(TYPE_KI, px, py - 8f,
                    (float) Math.cos(a) * speed,
                    (float) Math.sin(a) * speed,
                    radius, dmg, 1.6f, Math.max(0, kiLevel - 4)));
        }
    }

    private void fireKamehameha() {
        Enemy target = nearestEnemy();
        if (target == null) return;
        float angle = (float) Math.atan2(target.y - py, target.x - px);
        float speed = 620f + kameLevel * 25f;
        float dmg = (22f + 12f * kameLevel) * damageMultiplier;
        float radius = 15f + kameLevel * 3f;
        Projectile beam = Projectile.make(TYPE_KAME, px, py - 6f,
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed,
                radius, dmg, 1.4f, 2 + kameLevel / 2);
        beam.trail = 90f + kameLevel * 24f;
        projectiles.add(beam);
        banner = "KAMEHAMEHA!";
        bannerUntil = System.currentTimeMillis() + 650;
    }

    private void fireDiscVolley() {
        Enemy target = nearestEnemy();
        if (target == null) return;
        float angle = (float) Math.atan2(target.y - py, target.x - px);
        int amount = discLevel >= 4 ? 2 : 1;
        for (int i = 0; i < amount; i++) {
            float spread = amount == 1 ? 0f : (i == 0 ? -0.18f : 0.18f);
            Projectile disc = Projectile.make(TYPE_DISC, px, py,
                    (float) Math.cos(angle + spread) * (600f + discLevel * 35f),
                    (float) Math.sin(angle + spread) * (600f + discLevel * 35f),
                    14f + discLevel * 1.7f,
                    (18f + discLevel * 7f) * damageMultiplier,
                    2.3f,
                    3 + discLevel);
            disc.spin = rng.nextBoolean() ? 1f : -1f;
            projectiles.add(disc);
        }
    }

    private void fireSpiritBomb() {
        Enemy target = nearestEnemy();
        if (target == null) return;
        float angle = (float) Math.atan2(target.y - py, target.x - px);
        Projectile bomb = Projectile.make(TYPE_BOMB, px, py - 14,
                (float) Math.cos(angle) * 260f,
                (float) Math.sin(angle) * 260f,
                18f + bombLevel * 3f,
                (40f + bombLevel * 16f) * damageMultiplier,
                3.2f,
                99);
        bomb.trail = 22f;
        projectiles.add(bomb);
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile pr = projectiles.get(i);
            pr.life -= dt;
            pr.rotation += dt * 9f * pr.spin;
            pr.prevX = pr.x;
            pr.prevY = pr.y;
            pr.x += pr.vx * dt;
            pr.y += pr.vy * dt;

            boolean remove = false;
            if (pr.life <= 0 || pr.x < -120 || pr.x > getWidth() + 120 || pr.y < -120 || pr.y > getHeight() + 120) {
                if (pr.type == TYPE_BOMB) {
                    explode(pr.x, pr.y, 66f + bombLevel * 18f, pr.damage * 1.2f);
                }
                projectiles.remove(i);
                continue;
            }

            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (dist(pr.x, pr.y, e.x, e.y) < e.r + pr.r) {
                    float dealt = pr.damage;
                    if (pr.type == TYPE_KAME) dealt *= 1.15f;
                    e.hp -= dealt;
                    pr.hits++;
                    floaters.add(new Floater(e.x, e.y, "-" + (int) dealt, colorForProjectile(pr.type)));
                    if (pr.type == TYPE_BOMB) {
                        explode(pr.x, pr.y, 66f + bombLevel * 18f, pr.damage * 1.2f);
                        remove = true;
                    }
                    if (e.hp <= 0) {
                        killEnemy(j, e);
                    }
                    if (!remove && pr.hits > pr.pierce) {
                        remove = true;
                    }
                    if (remove) break;
                }
            }

            if (remove) {
                projectiles.remove(i);
            }
        }
    }

    private void explode(float x, float y, float radius, float damage) {
        explosions.add(new Explosion(x, y, radius));
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy e = enemies.get(j);
            if (dist(x, y, e.x, e.y) <= radius + e.r) {
                e.hp -= damage;
                floaters.add(new Floater(e.x, e.y, "BOOM", Color.rgb(255, 232, 150)));
                if (e.hp <= 0) killEnemy(j, e);
            }
        }
    }

    private void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            float dx = px - e.x;
            float dy = py - e.y;
            float d = (float) Math.max(1f, Math.hypot(dx, dy));
            e.x += dx / d * e.speed * dt;
            e.y += dy / d * e.speed * dt;
            if (e.hitCooldown > 0f) e.hitCooldown -= dt;

            if (d < e.r + 22f && e.hitCooldown <= 0f) {
                float damage = e.touchDamage - armor;
                if (damage < 1f) damage = 1f;
                hp -= damage;
                e.hitCooldown = 0.7f;
                floaters.add(new Floater(px, py - 38, "HIT!", Color.WHITE));
                if (hp <= 0f) {
                    hp = 0f;
                    gameOver = true;
                    banner = "YOU WERE DEFEATED";
                    bannerUntil = System.currentTimeMillis() + 2000;
                }
            }
        }
    }

    private void updateOrbs(float dt) {
        for (int i = orbs.size() - 1; i >= 0; i--) {
            Orb o = orbs.get(i);
            float d = dist(px, py, o.x, o.y);
            if (d < pickupRange) {
                float dx = px - o.x;
                float dy = py - o.y;
                float pull = Math.min(1f, dt * (7.5f + radarLevel));
                o.x += dx * pull;
                o.y += dy * pull;
            }
            if (d < 30f) {
                xp += o.value;
                orbs.remove(i);
                checkLevelUp();
            }
        }
    }

    private void updateChests() {
        for (int i = chests.size() - 1; i >= 0; i--) {
            Chest ch = chests.get(i);
            if (dist(px, py, ch.x, ch.y) < 36f) {
                openChest();
                chests.remove(i);
                break;
            }
        }
    }

    private void updateExplosions(float dt) {
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion e = explosions.get(i);
            e.life -= dt;
            if (e.life <= 0) explosions.remove(i);
        }
    }

    private void updateFloaters(float dt) {
        for (int i = floaters.size() - 1; i >= 0; i--) {
            Floater f = floaters.get(i);
            f.life -= dt;
            f.y -= 38f * dt;
            if (f.life <= 0f) floaters.remove(i);
        }
    }

    private void spawnEnemy(boolean boss, long elapsed) {
        float x;
        float y;
        int edge = rng.nextInt(4);
        if (edge == 0) {
            x = -50;
            y = rng.nextFloat() * getHeight();
        } else if (edge == 1) {
            x = getWidth() + 50;
            y = rng.nextFloat() * getHeight();
        } else if (edge == 2) {
            x = rng.nextFloat() * getWidth();
            y = -50;
        } else {
            x = rng.nextFloat() * getWidth();
            y = getHeight() + 50;
        }

        float scale = 1f + elapsed / 100000f;
        Enemy e = new Enemy();
        e.x = x;
        e.y = y;

        if (boss) {
            e.type = ENEMY_BOSS;
            e.r = 36f + elapsed / 25000f;
            e.hp = 220f * scale;
            e.speed = 78f + elapsed / 7000f;
            e.touchDamage = 18f;
            e.boss = true;
        } else {
            int type;
            float roll = rng.nextFloat();
            if (elapsed < 40000) {
                type = ENEMY_SAIBAMAN;
            } else if (elapsed < 85000) {
                type = roll < 0.55f ? ENEMY_SOLDIER : ENEMY_SAIBAMAN;
            } else if (elapsed < 130000) {
                if (roll < 0.4f) type = ENEMY_SOLDIER;
                else if (roll < 0.78f) type = ENEMY_ANDROID;
                else type = ENEMY_SAIBAMAN;
            } else {
                if (roll < 0.25f) type = ENEMY_SOLDIER;
                else if (roll < 0.6f) type = ENEMY_ANDROID;
                else type = ENEMY_MAJIN;
            }
            e.type = type;
            if (type == ENEMY_SAIBAMAN) {
                e.r = 18f;
                e.hp = 34f * scale;
                e.speed = 106f + rng.nextFloat() * 18f;
                e.touchDamage = 7f;
            } else if (type == ENEMY_SOLDIER) {
                e.r = 20f;
                e.hp = 48f * scale;
                e.speed = 96f + rng.nextFloat() * 15f;
                e.touchDamage = 8f;
            } else if (type == ENEMY_ANDROID) {
                e.r = 22f;
                e.hp = 70f * scale;
                e.speed = 86f + rng.nextFloat() * 10f;
                e.touchDamage = 10f;
            } else {
                e.r = 24f;
                e.hp = 88f * scale;
                e.speed = 76f + rng.nextFloat() * 12f;
                e.touchDamage = 12f;
            }
        }
        enemies.add(e);
    }

    private void killEnemy(int index, Enemy e) {
        enemies.remove(index);
        kills++;
        int count = e.boss ? 16 : (e.type == ENEMY_MAJIN ? 3 : 2);
        float orbValue = e.boss ? 4f : (e.type == ENEMY_ANDROID ? 2f : 1f);
        for (int i = 0; i < count; i++) {
            orbs.add(new Orb(e.x + rng.nextFloat() * 18f - 9f, e.y + rng.nextFloat() * 18f - 9f, orbValue));
        }
        if (e.boss) {
            chests.add(new Chest(e.x, e.y));
            banner = "BOSS DEFEATED";
            bannerUntil = System.currentTimeMillis() + 1900;
        }
    }

    private void checkLevelUp() {
        while (xp >= xpNext) {
            xp -= xpNext;
            level++;
            xpNext = (float) (xpNext * 1.24 + 5);
            pendingUpgrades++;
        }
        if (!choosingUpgrade && pendingUpgrades > 0) {
            presentUpgradeChoices();
        }
    }

    private void presentUpgradeChoices() {
        if (pendingUpgrades > 0) pendingUpgrades--;
        choosingUpgrade = true;
        choices.clear();
        ArrayList<UpgradeOption> pool = buildUpgradePool();
        Collections.shuffle(pool, rng);
        HashSet<String> used = new HashSet<>();
        for (UpgradeOption option : pool) {
            if (used.add(option.id)) {
                choices.add(option);
            }
            if (choices.size() >= 3) break;
        }
        if (choices.isEmpty()) {
            choices.add(new UpgradeOption("power", "Power Up", "Small boost to damage and speed"));
        }
        banner = "LEVEL " + level + " - CHOOSE AN UPGRADE";
        bannerUntil = System.currentTimeMillis() + 3000;
    }

    private ArrayList<UpgradeOption> buildUpgradePool() {
        ArrayList<UpgradeOption> pool = new ArrayList<>();
        if (kiLevel < 6) pool.add(new UpgradeOption("ki", "Ki Blast", "More shots, damage and speed"));
        if (kameLevel < 5) pool.add(new UpgradeOption("kame", "Kamehameha", kameLevel == 0 ? "Unlock a powerful beam" : "Bigger beam with more damage"));
        if (discLevel < 5) pool.add(new UpgradeOption("disc", "Kienzan", discLevel == 0 ? "Unlock a piercing spinning disc" : "More pierce and more discs"));
        if (bombLevel < 4) pool.add(new UpgradeOption("bomb", "Genkidama", bombLevel == 0 ? "Unlock a huge explosive spirit bomb" : "Larger explosions and damage"));
        if (kaiokenLevel < 4) pool.add(new UpgradeOption("kaioken", "Kaioken", "Move faster and attack faster"));
        if (senzuLevel < 3) pool.add(new UpgradeOption("senzu", "Senzu Bean", "Heal and increase regeneration"));
        if (radarLevel < 4) pool.add(new UpgradeOption("radar", "Dragon Radar", "Greater pickup range and attraction"));
        if (armorLevel < 4) pool.add(new UpgradeOption("armor", "Saiyan Armor", "Reduce incoming damage"));
        if (level >= 7 && superSaiyanLevel < 3) pool.add(new UpgradeOption("ssj", "Super Saiyan", superSaiyanLevel == 0 ? "Transform and unleash a golden aura" : "More aura, damage and speed"));
        return pool;
    }

    private void openChest() {
        ArrayList<UpgradeOption> pool = buildUpgradePool();
        if (!pool.isEmpty()) {
            UpgradeOption op = pool.get(rng.nextInt(pool.size()));
            applyUpgrade(op, true);
        } else {
            damageMultiplier += 0.08f;
            moveSpeed += 12f;
            banner = "CHEST: POWER SURGE";
            bannerUntil = System.currentTimeMillis() + 1800;
        }
        hp = Math.min(maxHp, hp + 18f);
    }

    private void applyUpgrade(UpgradeOption op, boolean fromChest) {
        String msg = op.title;
        if ("ki".equals(op.id)) {
            kiLevel++;
            msg = "Ki Blast Lv " + kiLevel;
        } else if ("kame".equals(op.id)) {
            kameLevel++;
            msg = "Kamehameha Lv " + kameLevel;
        } else if ("disc".equals(op.id)) {
            discLevel++;
            msg = "Kienzan Lv " + discLevel;
        } else if ("bomb".equals(op.id)) {
            bombLevel++;
            msg = "Genkidama Lv " + bombLevel;
        } else if ("kaioken".equals(op.id)) {
            kaiokenLevel++;
            moveSpeed += 24f;
            cooldownMultiplier *= 0.92f;
            msg = "Kaioken Lv " + kaiokenLevel;
        } else if ("senzu".equals(op.id)) {
            senzuLevel++;
            maxHp += 18f;
            hp = Math.min(maxHp, hp + 34f);
            regenPerSec += 0.35f;
            msg = "Senzu Bean Lv " + senzuLevel;
        } else if ("radar".equals(op.id)) {
            radarLevel++;
            pickupRange += 32f;
            msg = "Dragon Radar Lv " + radarLevel;
        } else if ("armor".equals(op.id)) {
            armorLevel++;
            armor += 1.3f;
            msg = "Saiyan Armor Lv " + armorLevel;
        } else if ("ssj".equals(op.id)) {
            superSaiyanLevel++;
            damageMultiplier += 0.2f;
            moveSpeed += 18f;
            cooldownMultiplier *= 0.95f;
            msg = superSaiyanLevel == 1 ? "SUPER SAIYAN!" : "Super Saiyan Lv " + superSaiyanLevel;
        } else {
            damageMultiplier += 0.06f;
            moveSpeed += 8f;
            msg = "Power Up";
        }
        choosingUpgrade = false;
        choices.clear();
        banner = (fromChest ? "CHEST: " : "LEVEL UP: ") + msg;
        bannerUntil = System.currentTimeMillis() + 2000;
        if (!fromChest && pendingUpgrades > 0) {
            presentUpgradeChoices();
        }
    }

    private int colorForProjectile(int type) {
        if (type == TYPE_KI) return Color.rgb(120, 235, 255);
        if (type == TYPE_KAME) return Color.rgb(120, 210, 255);
        if (type == TYPE_DISC) return Color.rgb(255, 240, 120);
        return Color.rgb(255, 222, 120);
    }

    private void drawWorld(Canvas c) {
        drawBackground(c);
        drawGroundDeco(c);

        for (Chest ch : chests) drawChest(c, ch);
        for (Orb orb : orbs) drawOrb(c, orb);
        for (Explosion ex : explosions) drawExplosion(c, ex);
        for (Projectile pr : projectiles) drawProjectile(c, pr);
        for (Enemy e : enemies) drawEnemy(c, e);
        drawPlayer(c);
        for (Floater f : floaters) drawFloater(c, f);

        drawHud(c);
        if (System.currentTimeMillis() < bannerUntil) drawBanner(c);
        if (choosingUpgrade) drawUpgradeOverlay(c);
        if (gameOver) drawGameOver(c);
    }

    private void drawBackground(Canvas c) {
        p.setShader(skyGradient);
        c.drawRect(0, 0, getWidth(), getHeight(), p);
        p.setShader(null);

        p.setColor(Color.argb(150, 255, 255, 220));
        c.drawCircle(getWidth() * 0.82f, getHeight() * 0.17f, 42f, p);
        p.setColor(Color.argb(95, 162, 250, 255));
        c.drawCircle(getWidth() * 0.17f, getHeight() * 0.12f, 28f, p);

        float farX = (worldShiftX * 0.15f) % getWidth();
        drawMountainBand(c, farX, getHeight() * 0.48f, Color.rgb(60, 88, 104), 120f, 200f, 0.55f);
        drawMountainBand(c, farX * 1.3f, getHeight() * 0.58f, Color.rgb(78, 110, 84), 150f, 250f, 0.72f);

        for (int i = 0; i < 4; i++) {
            float cx = ((i * 260f) - (worldShiftX * 0.22f)) % (getWidth() + 260f);
            if (cx < -140) cx += getWidth() + 260f;
            float cy = 70 + i * 52f + (i % 2 == 0 ? 0 : 18);
            p.setColor(Color.argb(30, 255, 255, 255));
            c.drawOval(new RectF(cx, cy, cx + 150, cy + 34), p);
        }
    }

    private void drawMountainBand(Canvas c, float shift, float baseY, int color, float peakMin, float peakMax, float alphaMul) {
        Path path = new Path();
        path.moveTo(-200, getHeight());
        float x = -200 - shift;
        while (x < getWidth() + 240) {
            float peak = peakMin + ((int) (Math.abs(x) / 200f) % 3) * 18f + rngBasedNoise((int) x) * (peakMax - peakMin);
            path.lineTo(x, baseY);
            path.lineTo(x + 90, baseY - peak * alphaMul);
            path.lineTo(x + 180, baseY);
            x += 180;
        }
        path.lineTo(getWidth() + 200, getHeight());
        path.close();
        p.setColor(color);
        c.drawPath(path, p);
    }

    private float rngBasedNoise(int seed) {
        int n = seed * 73428767;
        n = (n << 13) ^ n;
        return 0.5f + 0.5f * (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824f);
    }

    private void drawGroundDeco(Canvas c) {
        float groundTop = getHeight() * 0.66f;
        p.setColor(Color.rgb(114, 170, 95));
        c.drawRect(0, groundTop, getWidth(), getHeight(), p);

        p.setColor(Color.argb(45, 20, 70, 26));
        for (int i = -1; i < 8; i++) {
            float x = ((i * 170f) - (worldShiftX * 0.4f)) % (getWidth() + 190f);
            if (x < -100) x += getWidth() + 190f;
            c.drawOval(new RectF(x, groundTop + 40, x + 120, groundTop + 85), p);
        }

        p.setColor(Color.rgb(93, 104, 116));
        for (int i = -1; i < 10; i++) {
            float x = ((i * 120f) - (worldShiftX * 0.65f)) % (getWidth() + 120f);
            if (x < -60) x += getWidth() + 120f;
            float y = groundTop + 56 + (i % 3) * 14;
            c.drawRoundRect(new RectF(x, y, x + 32, y + 18), 8, 8, p);
        }
    }

    private void drawChest(Canvas c, Chest ch) {
        p.setColor(Color.rgb(112, 56, 28));
        c.drawRoundRect(new RectF(ch.x - 18, ch.y - 13, ch.x + 18, ch.y + 13), 6, 6, p);
        p.setColor(Color.rgb(242, 182, 52));
        c.drawRoundRect(new RectF(ch.x - 18, ch.y - 13, ch.x + 18, ch.y - 2), 6, 6, p);
        p.setColor(Color.YELLOW);
        c.drawRect(ch.x - 3, ch.y - 13, ch.x + 3, ch.y + 13, p);
        p.setColor(Color.argb(90, 255, 220, 110));
        c.drawCircle(ch.x, ch.y - 15, 13, p);
    }

    private void drawOrb(Canvas c, Orb o) {
        p.setColor(Color.rgb(90, 228, 255));
        c.drawCircle(o.x, o.y, 7f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(Color.WHITE);
        c.drawCircle(o.x, o.y, 10f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawExplosion(Canvas c, Explosion ex) {
        float t = ex.life / 0.35f;
        p.setColor(Color.argb((int) (110 * t), 255, 230, 110));
        c.drawCircle(ex.x, ex.y, ex.radius * (1.12f - 0.12f * t), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f);
        p.setColor(Color.argb((int) (180 * t), 255, 255, 255));
        c.drawCircle(ex.x, ex.y, ex.radius * (1.34f - 0.2f * t), p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawProjectile(Canvas c, Projectile pr) {
        if (pr.type == TYPE_KI) {
            p.setColor(Color.rgb(95, 230, 255));
            c.drawCircle(pr.x, pr.y, pr.r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            p.setColor(Color.WHITE);
            c.drawCircle(pr.x, pr.y, pr.r + 3f, p);
            p.setStyle(Paint.Style.FILL);
        } else if (pr.type == TYPE_KAME) {
            float len = pr.trail;
            float angle = (float) Math.atan2(pr.vy, pr.vx);
            float tx = pr.x - (float) Math.cos(angle) * len;
            float ty = pr.y - (float) Math.sin(angle) * len;
            p.setStrokeWidth(pr.r * 1.4f);
            p.setColor(Color.argb(130, 120, 220, 255));
            c.drawLine(tx, ty, pr.x, pr.y, p);
            p.setStrokeWidth(pr.r * 0.8f);
            p.setColor(Color.WHITE);
            c.drawLine(tx, ty, pr.x, pr.y, p);
            p.setColor(Color.rgb(115, 205, 255));
            c.drawCircle(pr.x, pr.y, pr.r, p);
        } else if (pr.type == TYPE_DISC) {
            c.save();
            c.translate(pr.x, pr.y);
            c.rotate((float) Math.toDegrees(pr.rotation));
            p.setColor(Color.rgb(255, 240, 110));
            c.drawCircle(0, 0, pr.r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(Color.WHITE);
            c.drawCircle(0, 0, pr.r - 4f, p);
            c.drawLine(-pr.r + 2, 0, pr.r - 2, 0, p);
            p.setStyle(Paint.Style.FILL);
            c.restore();
        } else {
            RadialGradient grad = new RadialGradient(pr.x, pr.y, pr.r * 1.8f,
                    new int[]{Color.WHITE, Color.rgb(145, 215, 255), Color.argb(40, 90, 160, 255)},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
            p.setShader(grad);
            c.drawCircle(pr.x, pr.y, pr.r * 1.7f, p);
            p.setShader(null);
            p.setColor(Color.rgb(120, 205, 255));
            c.drawCircle(pr.x, pr.y, pr.r, p);
        }
    }

    private void drawEnemy(Canvas c, Enemy e) {
        if (e.type == ENEMY_SAIBAMAN) {
            p.setColor(Color.rgb(90, 180, 78));
            c.drawCircle(e.x, e.y, e.r, p);
            p.setColor(Color.BLACK);
            c.drawCircle(e.x - 5, e.y - 4, 2.2f, p);
            c.drawCircle(e.x + 5, e.y - 4, 2.2f, p);
            p.setStrokeWidth(2f);
            c.drawLine(e.x, e.y - e.r, e.x - 8, e.y - e.r - 12, p);
            c.drawLine(e.x, e.y - e.r, e.x + 8, e.y - e.r - 12, p);
        } else if (e.type == ENEMY_SOLDIER) {
            p.setColor(Color.rgb(134, 90, 190));
            c.drawCircle(e.x, e.y, e.r, p);
            p.setColor(Color.rgb(228, 228, 240));
            c.drawCircle(e.x, e.y - 2, e.r * 0.58f, p);
            p.setColor(Color.BLACK);
            c.drawRect(e.x - 6, e.y + 7, e.x + 6, e.y + 10, p);
        } else if (e.type == ENEMY_ANDROID) {
            p.setColor(Color.rgb(165, 175, 183));
            c.drawRoundRect(new RectF(e.x - e.r, e.y - e.r, e.x + e.r, e.y + e.r), 8, 8, p);
            p.setColor(Color.rgb(255, 70, 70));
            c.drawCircle(e.x, e.y - 2, 4, p);
            p.setColor(Color.rgb(70, 80, 90));
            c.drawRect(e.x - 8, e.y + 8, e.x + 8, e.y + 11, p);
        } else if (e.type == ENEMY_MAJIN) {
            p.setColor(Color.rgb(220, 118, 182));
            c.drawCircle(e.x, e.y, e.r, p);
            p.setColor(Color.rgb(128, 32, 92));
            c.drawCircle(e.x - 5, e.y - 5, 2.4f, p);
            c.drawCircle(e.x + 5, e.y - 5, 2.4f, p);
            c.drawArc(new RectF(e.x - 8, e.y - 2, e.x + 8, e.y + 10), 20, 140, false, p);
        } else {
            p.setColor(Color.rgb(116, 56, 168));
            c.drawCircle(e.x, e.y, e.r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5f);
            p.setColor(Color.rgb(255, 72, 72));
            c.drawCircle(e.x, e.y, e.r + 10, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(255, 230, 130));
            c.drawCircle(e.x - 10, e.y - 6, 4, p);
            c.drawCircle(e.x + 10, e.y - 6, 4, p);
        }
    }

    private void drawPlayer(Canvas c) {
        int auraColor = superSaiyanLevel > 0 ? Color.argb(100, 255, 220, 70)
                : (kaiokenLevel > 0 ? Color.argb(100, 255, 90, 90) : Color.argb(80, 110, 210, 255));
        p.setColor(auraColor);
        c.drawCircle(px, py - 5f, 34f + superSaiyanLevel * 3f + kaiokenLevel * 2f, p);

        p.setColor(Color.rgb(242, 196, 142));
        c.drawCircle(px, py - 16f, 11f, p);
        p.setColor(Color.rgb(40, 84, 222));
        c.drawRect(px - 12f, py - 6f, px + 12f, py + 18f, p);
        p.setColor(Color.rgb(242, 122, 38));
        c.drawRect(px - 15f, py - 2f, px + 15f, py + 18f, p);
        p.setColor(Color.rgb(35, 54, 116));
        c.drawRect(px - 12f, py + 18f, px - 3f, py + 34f, p);
        c.drawRect(px + 3f, py + 18f, px + 12f, py + 34f, p);

        Path hair = new Path();
        hair.moveTo(px - 11f, py - 18f);
        hair.lineTo(px - 18f, py - 32f);
        hair.lineTo(px - 6f, py - 28f);
        hair.lineTo(px - 2f, py - 40f);
        hair.lineTo(px + 5f, py - 29f);
        hair.lineTo(px + 16f, py - 36f);
        hair.lineTo(px + 10f, py - 20f);
        hair.close();
        p.setColor(superSaiyanLevel > 0 ? Color.rgb(255, 228, 84) : Color.rgb(24, 24, 24));
        p.setPathEffect(new CornerPathEffect(4f));
        c.drawPath(hair, p);
        p.setPathEffect(null);
    }

    private void drawFloater(Canvas c, Floater f) {
        text.setColor(f.color);
        text.setTextSize(20f);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(f.text, f.x, f.y, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHud(Canvas c) {
        ui.setColor(Color.argb(165, 0, 0, 0));
        c.drawRoundRect(new RectF(14, 12, 368, 108), 16, 16, ui);

        text.setColor(Color.WHITE);
        text.setTextSize(25f);
        c.drawText("LV " + level + "   Kills " + kills + "   " + timeText(), 28, 40, text);
        text.setTextSize(18f);
        c.drawText(activeWeaponsText(), 28, 64, text);

        ui.setColor(Color.rgb(58, 58, 58));
        c.drawRoundRect(new RectF(28, 76, 340, 88), 8, 8, ui);
        ui.setColor(Color.rgb(224, 80, 80));
        c.drawRoundRect(new RectF(28, 76, 28 + 312f * (hp / maxHp), 88), 8, 8, ui);

        ui.setColor(Color.argb(160, 25, 25, 25));
        c.drawRoundRect(new RectF(16, getHeight() - 22, getWidth() - 16, getHeight() - 10), 8, 8, ui);
        ui.setColor(Color.rgb(86, 220, 255));
        c.drawRoundRect(new RectF(16, getHeight() - 22, 16 + (getWidth() - 32) * (xp / xpNext), getHeight() - 10), 8, 8, ui);

        text.setTextSize(16f);
        c.drawText("DMG x" + format1(damageMultiplier) + "   SPD " + (int) moveSpeed + "   PICKUP " + (int) pickupRange, 382, 34, text);
        c.drawText("Kaioken " + kaiokenLevel + "   SSJ " + superSaiyanLevel + "   Armor " + armorLevel, 382, 58, text);

        float bx = joyActive ? joyBaseX : 110f;
        float by = joyActive ? joyBaseY : getHeight() - 110f;
        ui.setColor(Color.argb(65, 255, 255, 255));
        c.drawCircle(bx, by, 72f, ui);
        ui.setColor(Color.argb(150, 255, 255, 255));
        c.drawCircle(bx + joyX, by + joyY, 30f, ui);
    }

    private String activeWeaponsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ki ").append(kiLevel);
        if (kameLevel > 0) sb.append(" • Kame ").append(kameLevel);
        if (discLevel > 0) sb.append(" • Kienzan ").append(discLevel);
        if (bombLevel > 0) sb.append(" • Genkidama ").append(bombLevel);
        return sb.toString();
    }

    private String format1(float v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private void drawBanner(Canvas c) {
        ui.setColor(Color.argb(210, 0, 0, 0));
        RectF r = new RectF(getWidth() / 2f - 250f, 24, getWidth() / 2f + 250f, 88);
        c.drawRoundRect(r, 18, 18, ui);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(28f);
        text.setColor(Color.rgb(255, 236, 120));
        c.drawText(banner, getWidth() / 2f, 65, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void drawUpgradeOverlay(Canvas c) {
        ui.setColor(Color.argb(205, 5, 8, 16));
        c.drawRect(0, 0, getWidth(), getHeight(), ui);

        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(Color.WHITE);
        text.setTextSize(34f);
        c.drawText("Choose your upgrade", getWidth() / 2f, 88f, text);
        text.setTextSize(18f);
        c.drawText("Tap one card to continue", getWidth() / 2f, 114f, text);

        float cardW = 240f;
        float cardH = 200f;
        float gap = 26f;
        float total = cardW * choices.size() + gap * (choices.size() - 1);
        float startX = (getWidth() - total) / 2f;
        float y = getHeight() * 0.32f;

        for (int i = 0; i < choices.size(); i++) {
            UpgradeOption op = choices.get(i);
            float left = startX + i * (cardW + gap);
            RectF card = new RectF(left, y, left + cardW, y + cardH);
            op.rect = card;

            ui.setColor(Color.argb(235, 24, 34, 54));
            c.drawRoundRect(card, 20, 20, ui);
            ui.setColor(Color.argb(255, 255, 212, 84));
            ui.setStyle(Paint.Style.STROKE);
            ui.setStrokeWidth(3f);
            c.drawRoundRect(card, 20, 20, ui);
            ui.setStyle(Paint.Style.FILL);

            text.setColor(Color.rgb(255, 235, 140));
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(25f);
            c.drawText(op.title, left + 18f, y + 38f, text);
            text.setColor(Color.WHITE);
            text.setTextSize(17f);
            drawMultilineText(c, op.desc, left + 18f, y + 72f, cardW - 36f, 22f);
        }
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void drawMultilineText(Canvas c, String str, float x, float y, float maxWidth, float lineHeight) {
        String[] words = str.split(" ");
        StringBuilder line = new StringBuilder();
        float dy = 0f;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (text.measureText(test) > maxWidth && line.length() > 0) {
                c.drawText(line.toString(), x, y + dy, text);
                line = new StringBuilder(word);
                dy += lineHeight;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) c.drawText(line.toString(), x, y + dy, text);
    }

    private void drawGameOver(Canvas c) {
        ui.setColor(Color.argb(225, 0, 0, 0));
        c.drawRect(0, 0, getWidth(), getHeight(), ui);
        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(Color.WHITE);
        text.setTextSize(54f);
        c.drawText("GAME OVER", getWidth() / 2f, getHeight() / 2f - 30f, text);
        text.setTextSize(26f);
        c.drawText("Kills: " + kills + "   •   Level: " + level, getWidth() / 2f, getHeight() / 2f + 12f, text);
        c.drawText("Touch the screen to restart", getWidth() / 2f, getHeight() / 2f + 58f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private String timeText() {
        long s = (System.currentTimeMillis() - runStart) / 1000L;
        return String.format(Locale.US, "%02d:%02d", s / 60L, s % 60L);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (gameOver && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            restart();
            return true;
        }

        if (choosingUpgrade && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float tx = e.getX();
            float ty = e.getY();
            for (UpgradeOption op : choices) {
                if (op.rect != null && op.rect.contains(tx, ty)) {
                    applyUpgrade(op, false);
                    return true;
                }
            }
            return true;
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                joyActive = true;
                joyBaseX = e.getX();
                joyBaseY = e.getY();
                joyX = 0f;
                joyY = 0f;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - joyBaseX;
                float dy = e.getY() - joyBaseY;
                float d = (float) Math.max(1f, Math.hypot(dx, dy));
                float m = Math.min(72f, d);
                joyX = dx / d * m;
                joyY = dy / d * m;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                joyActive = false;
                joyX = 0f;
                joyY = 0f;
                break;
        }
        return true;
    }

    private void restart() {
        enemies.clear();
        projectiles.clear();
        orbs.clear();
        chests.clear();
        floaters.clear();
        explosions.clear();
        choices.clear();

        hp = 120f;
        maxHp = 120f;
        xp = 0f;
        xpNext = 12f;
        level = 1;
        kills = 0;
        moveSpeed = 330f;
        damageMultiplier = 1f;
        cooldownMultiplier = 1f;
        pickupRange = 90f;
        armor = 0f;
        regenPerSec = 0f;

        kiLevel = 1;
        kameLevel = 0;
        discLevel = 0;
        bombLevel = 0;
        kaiokenLevel = 0;
        senzuLevel = 0;
        radarLevel = 0;
        armorLevel = 0;
        superSaiyanLevel = 0;

        px = getWidth() * 0.5f;
        py = getHeight() * 0.58f;
        worldShiftX = 0f;
        worldShiftY = 0f;

        runStart = System.currentTimeMillis();
        lastSpawn = lastWaveBanner = lastBoss = lastChest = 0L;
        lastKiShot = lastKame = lastDisc = lastBomb = 0L;
        banner = "SURVIVE THE WAVE!";
        bannerUntil = System.currentTimeMillis() + 1200;
        pendingUpgrades = 0;
        joyX = joyY = 0f;
        joyActive = false;
        choosingUpgrade = false;
        gameOver = false;
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    private static float dist(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(ax - bx, ay - by);
    }

    static class Enemy {
        float x, y;
        float hp;
        float r;
        float speed;
        float touchDamage;
        float hitCooldown;
        int type;
        boolean boss;
    }

    static class Projectile {
        int type;
        float x, y, prevX, prevY;
        float vx, vy;
        float r;
        float damage;
        float life;
        int pierce;
        int hits;
        float trail;
        float rotation;
        float spin = 1f;

        static Projectile make(int type, float x, float y, float vx, float vy, float r, float damage, float life, int pierce) {
            Projectile p = new Projectile();
            p.type = type;
            p.x = x;
            p.y = y;
            p.prevX = x;
            p.prevY = y;
            p.vx = vx;
            p.vy = vy;
            p.r = r;
            p.damage = damage;
            p.life = life;
            p.pierce = pierce;
            return p;
        }
    }

    static class Orb {
        float x, y, value;
        Orb(float x, float y, float value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }

    static class Chest {
        float x, y;
        Chest(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Floater {
        float x, y, life = 0.7f;
        String text;
        int color;
        Floater(float x, float y, String text, int color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }
    }

    static class Explosion {
        float x, y, radius, life = 0.35f;
        Explosion(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    static class UpgradeOption {
        String id;
        String title;
        String desc;
        RectF rect;
        UpgradeOption(String id, String title, String desc) {
            this.id = id;
            this.title = title;
            this.desc = desc;
        }
    }
}
