package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Shot> shots = new ArrayList<>();
    private final ArrayList<Orb> orbs = new ArrayList<>();
    private final ArrayList<Chest> chests = new ArrayList<>();

    private float px, py, jx, jy, baseX, baseY;
    private float hp = 100, maxHp = 100, xp = 0, xpNext = 10;
    private float speed = 320, damage = 12, fireDelay = 600, projectileSpeed = 760;
    private int level = 1, kills = 0, multi = 1, pierce = 0;
    private long started, lastFrame, lastSpawn, lastShot, lastChest, lastBoss;
    private boolean initialized, joystick, gameOver;
    private String power = "Energy Blast", banner = "SURVIVE!";
    private long bannerUntil;

    public GameView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(8, 10, 24));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!initialized) {
            px = w / 2f; py = h / 2f;
            started = System.currentTimeMillis();
            lastFrame = System.nanoTime();
            initialized = true;
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!initialized) return;
        long now = System.nanoTime();
        float dt = Math.min(.033f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        if (!gameOver) update(dt);
        drawGame(c);
        invalidate();
    }

    private void update(float dt) {
        long now = System.currentTimeMillis();
        float len = (float)Math.hypot(jx, jy);
        if (len > 1) {
            px += jx / len * speed * dt;
            py += jy / len * speed * dt;
        }
        px = clamp(px, 25, getWidth() - 25);
        py = clamp(py, 25, getHeight() - 25);

        long elapsed = now - started;
        long spawnDelay = Math.max(150, 700 - elapsed / 220);
        if (now - lastSpawn > spawnDelay) {
            spawn(false); lastSpawn = now;
        }
        if (elapsed > 30000 && now - lastBoss > 45000) {
            spawn(true); lastBoss = now; showBanner("BOSS!");
        }
        if (now - lastChest > 22000) {
            chests.add(new Chest(80 + rnd.nextFloat() * (getWidth() - 160), 80 + rnd.nextFloat() * (getHeight() - 160)));
            lastChest = now;
        }
        if (now - lastShot > fireDelay) {
            fire(); lastShot = now;
        }

        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            s.x += s.vx * dt; s.y += s.vy * dt; s.life -= dt;
            if (s.life <= 0) { shots.remove(i); continue; }
            boolean removed = false;
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (dist(s.x,s.y,e.x,e.y) < s.r + e.r) {
                    e.hp -= s.damage; s.hits++;
                    if (e.hp <= 0) kill(j, e);
                    if (s.hits > pierce) { shots.remove(i); removed = true; break; }
                }
            }
            if (removed) continue;
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            float dx = px - e.x, dy = py - e.y;
            float d = Math.max(1, (float)Math.hypot(dx,dy));
            e.x += dx / d * e.speed * dt;
            e.y += dy / d * e.speed * dt;
            e.cooldown -= dt;
            if (d < e.r + 22 && e.cooldown <= 0) {
                hp -= e.boss ? 18 : 7;
                e.cooldown = .7f;
                if (hp <= 0) { hp = 0; gameOver = true; }
            }
        }

        for (int i = orbs.size() - 1; i >= 0; i--) {
            Orb o = orbs.get(i);
            float d = dist(px,py,o.x,o.y);
            if (d < 130) {
                o.x += (px - o.x) * Math.min(1, dt * 9);
                o.y += (py - o.y) * Math.min(1, dt * 9);
            }
            if (d < 28) {
                xp += o.value; orbs.remove(i); levelCheck();
            }
        }

        for (int i = chests.size() - 1; i >= 0; i--) {
            Chest chest = chests.get(i);
            if (dist(px,py,chest.x,chest.y) < 42) {
                upgrade(true); chests.remove(i);
            }
        }
    }

    private void spawn(boolean boss) {
        float x, y;
        int edge = rnd.nextInt(4);
        if (edge == 0) { x = -40; y = rnd.nextFloat() * getHeight(); }
        else if (edge == 1) { x = getWidth() + 40; y = rnd.nextFloat() * getHeight(); }
        else if (edge == 2) { x = rnd.nextFloat() * getWidth(); y = -40; }
        else { x = rnd.nextFloat() * getWidth(); y = getHeight() + 40; }
        float scale = 1 + (System.currentTimeMillis() - started) / 70000f;
        enemies.add(new Enemy(x,y,boss ? 220 * scale : 28 * scale,boss ? 55 : 18,boss ? 62 : 100,boss));
    }

    private void fire() {
        if (enemies.isEmpty()) return;
        Enemy target = enemies.get(0);
        float best = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            float d = dist(px,py,e.x,e.y);
            if (d < best) { best = d; target = e; }
        }
        float angle = (float)Math.atan2(target.y - py, target.x - px);
        for (int i = 0; i < multi; i++) {
            float a = angle + (i - (multi - 1) / 2f) * .13f;
            shots.add(new Shot(px,py,(float)Math.cos(a)*projectileSpeed,(float)Math.sin(a)*projectileSpeed,damage));
        }
    }

    private void kill(int index, Enemy e) {
        enemies.remove(index); kills++;
        int amount = e.boss ? 18 : 1;
        for (int i = 0; i < amount; i++) orbs.add(new Orb(e.x + rnd.nextInt(20)-10,e.y+rnd.nextInt(20)-10,e.boss ? 2 : 1));
        if (e.boss) { chests.add(new Chest(e.x,e.y)); showBanner("BOSS DEFEATED!"); }
    }

    private void levelCheck() {
        while (xp >= xpNext) {
            xp -= xpNext; level++; xpNext = xpNext * 1.22f + 4; upgrade(false);
        }
    }

    private void upgrade(boolean chest) {
        String msg;
        switch (rnd.nextInt(7)) {
            case 0: damage += 6; power = "Kamehameha"; msg = "KAMEHAMEHA +DMG"; break;
            case 1: multi = Math.min(5, multi + 1); power = "Multi Ki Blast"; msg = "MULTI KI BLAST"; break;
            case 2: fireDelay = Math.max(180, fireDelay - 70); power = "Rapid Ki"; msg = "RAPID KI"; break;
            case 3: speed += 35; msg = "KAIOKEN: SPEED"; break;
            case 4: pierce = Math.min(5, pierce + 1); power = "Kienzan"; msg = "KIENZAN: PIERCE"; break;
            case 5: maxHp += 15; hp = Math.min(maxHp, hp + 30); msg = "SENZU BEAN"; break;
            default: projectileSpeed += 70; damage += 2; msg = "DRAGON RADAR"; break;
        }
        if (level >= 8 && rnd.nextFloat() < .2f) {
            damage += 10; speed += 25; power = "Super Saiyan Aura"; msg = "SUPER SAIYAN!";
        }
        showBanner((chest ? "CHEST: " : "LEVEL UP: ") + msg);
    }

    private void showBanner(String text) { banner = text; bannerUntil = System.currentTimeMillis() + 1800; }

    private void drawGame(Canvas c) {
        p.setStrokeWidth(1); p.setColor(Color.rgb(20,25,55));
        for (int x=0;x<getWidth();x+=80) c.drawLine(x,0,x,getHeight(),p);
        for (int y=0;y<getHeight();y+=80) c.drawLine(0,y,getWidth(),y,p);

        for (Orb o: orbs) { p.setColor(Color.CYAN); c.drawCircle(o.x,o.y,6,p); }
        for (Chest ch: chests) { p.setColor(Color.rgb(235,170,40)); c.drawRect(ch.x-17,ch.y-13,ch.x+17,ch.y+13,p); p.setColor(Color.YELLOW); c.drawRect(ch.x-3,ch.y-13,ch.x+3,ch.y+13,p); }
        for (Shot s: shots) { p.setColor(Color.rgb(80,205,255)); c.drawCircle(s.x,s.y,s.r,p); }
        for (Enemy e: enemies) { p.setColor(e.boss ? Color.MAGENTA : Color.rgb(80,190,90)); c.drawCircle(e.x,e.y,e.r,p); }

        p.setColor(Color.argb(90,255,220,40)); c.drawCircle(px,py,34,p);
        p.setColor(Color.rgb(255,190,50)); c.drawCircle(px,py,22,p);
        p.setColor(Color.rgb(45,75,210)); c.drawRect(px-12,py+2,px+12,py+20,p);

        p.setColor(Color.argb(190,0,0,0)); c.drawRect(14,12,390,96,p);
        p.setColor(Color.WHITE); p.setTextSize(24); c.drawText("LV " + level + "  Kills " + kills + "  " + timeText(),28,40,p);
        p.setTextSize(19); c.drawText(power,28,67,p);
        p.setColor(Color.DKGRAY); c.drawRect(28,76,350,88,p);
        p.setColor(Color.RED); c.drawRect(28,76,28+322*(hp/maxHp),88,p);

        p.setColor(Color.DKGRAY); c.drawRect(14,getHeight()-18,getWidth()-14,getHeight()-8,p);
        p.setColor(Color.CYAN); c.drawRect(14,getHeight()-18,14+(getWidth()-28)*(xp/xpNext),getHeight()-8,p);

        float bx = joystick ? baseX : 110, by = joystick ? baseY : getHeight()-110;
        p.setColor(Color.argb(70,255,255,255)); c.drawCircle(bx,by,70,p);
        p.setColor(Color.argb(150,255,255,255)); c.drawCircle(bx+jx,by+jy,30,p);

        if (System.currentTimeMillis() < bannerUntil) {
            p.setColor(Color.argb(210,0,0,0)); c.drawRoundRect(getWidth()/2f-250,25,getWidth()/2f+250,88,18,18,p);
            p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.YELLOW); p.setTextSize(29); c.drawText(banner,getWidth()/2f,66,p); p.setTextAlign(Paint.Align.LEFT);
        }
        if (gameOver) {
            p.setColor(Color.argb(225,0,0,0)); c.drawRect(0,0,getWidth(),getHeight(),p);
            p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(54); c.drawText("GAME OVER",getWidth()/2f,getHeight()/2f-30,p);
            p.setTextSize(26); c.drawText("Kills: "+kills+"  •  Level: "+level,getWidth()/2f,getHeight()/2f+20,p);
            c.drawText("Tocá para reiniciar",getWidth()/2f,getHeight()/2f+65,p); p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private String timeText() {
        long s = (System.currentTimeMillis() - started) / 1000;
        return String.format(Locale.US, "%02d:%02d", s/60, s%60);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (gameOver && e.getActionMasked() == MotionEvent.ACTION_DOWN) { restart(); return true; }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                joystick = true; baseX = e.getX(); baseY = e.getY(); jx = jy = 0; break;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - baseX, dy = e.getY() - baseY;
                float d = Math.max(1,(float)Math.hypot(dx,dy));
                float m = Math.min(70,d); jx = dx/d*m; jy = dy/d*m; break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                joystick = false; jx = jy = 0; break;
        }
        return true;
    }

    private void restart() {
        enemies.clear(); shots.clear(); orbs.clear(); chests.clear();
        hp=maxHp=100; xp=0; xpNext=10; speed=320; damage=12; fireDelay=600; projectileSpeed=760;
        level=1; kills=0; multi=1; pierce=0; power="Energy Blast"; px=getWidth()/2f; py=getHeight()/2f;
        started=System.currentTimeMillis(); lastSpawn=lastShot=lastChest=lastBoss=0; gameOver=false;
    }

    private static float clamp(float v,float min,float max){ return Math.max(min,Math.min(max,v)); }
    private static float dist(float ax,float ay,float bx,float by){ return (float)Math.hypot(ax-bx,ay-by); }

    static class Enemy { float x,y,hp,r,speed,cooldown; boolean boss; Enemy(float x,float y,float hp,float r,float speed,boolean boss){this.x=x;this.y=y;this.hp=hp;this.r=r;this.speed=speed;this.boss=boss;} }
    static class Shot { float x,y,vx,vy,damage,r=9,life=2.2f; int hits; Shot(float x,float y,float vx,float vy,float damage){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.damage=damage;} }
    static class Orb { float x,y,value; Orb(float x,float y,float value){this.x=x;this.y=y;this.value=value;} }
    static class Chest { float x,y; Chest(float x,float y){this.x=x;this.y=y;} }
}
