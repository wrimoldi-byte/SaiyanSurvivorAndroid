package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.*;

/** v0.5.0: refined Dragon-Ball-inspired worlds, projectile trails and easier early game. */
public class GameViewV6 extends GameViewV5 {
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameViewV6(Context c) {
        super(c);
        dmg = 1.25f;
        banner = "WORLD ADVENTURE";
    }

    @Override
    void spawn(boolean isBoss, long elapsed) {
        float margin = isBoss ? 180f : 105f;
        float x, y;
        int edge = r.nextInt(4);
        if (edge == 0) { x = -margin; y = r.nextFloat() * getHeight(); }
        else if (edge == 1) { x = getWidth() + margin; y = r.nextFloat() * getHeight(); }
        else if (edge == 2) { x = r.nextFloat() * getWidth(); y = -margin; }
        else { x = r.nextFloat() * getWidth(); y = getHeight() + margin; }

        float scaling = 1f + elapsed / 115000f;
        int type = isBoss ? 4 : (elapsed < 40000 ? 0 : elapsed < 85000 ? (r.nextBoolean() ? 0 : 1) : elapsed < 130000 ? r.nextInt(3) : r.nextInt(4));

        // First 90 seconds are intentionally faster and more satisfying.
        float early = elapsed < 30000 ? 0.58f : elapsed < 60000 ? 0.68f : elapsed < 90000 ? 0.82f : 1f;
        float baseHp = isBoss ? 245f * scaling : (31f + type * 16f) * scaling * early;
        float radius = isBoss ? 42f : 18f + type * 2f;
        float speed = isBoss ? 74f : 105f - type * 9f;
        float touch = isBoss ? 17f : 6.5f + type * 1.8f;
        es.add(new E(x, y, type, baseHp, radius, speed, touch, isBoss));
    }

    @Override
    void bg(Canvas c) {
        int biome = biomeIndex();
        if (biome == 0) drawEmeraldWorld(c);
        else if (biome == 1) drawRockyWorld(c);
        else drawRuinedWorld(c);
    }

    private int biomeIndex() {
        double meters = Math.hypot(worldX, worldY) / 100.0;
        return ((int)(meters / 22.0)) % 3;
    }

    private void drawEmeraldWorld(Canvas c) {
        Paint p = this.p;
        p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{Color.rgb(42, 88, 158), Color.rgb(91, 174, 191), Color.rgb(132, 208, 133)},
                new float[]{0f, .56f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null);

        // twin celestial bodies
        p.setColor(Color.argb(170, 244, 255, 210));
        c.drawCircle(getWidth() * .80f, getHeight() * .15f, 43f, p);
        p.setColor(Color.argb(115, 185, 245, 255));
        c.drawCircle(getWidth() * .17f, getHeight() * .12f, 26f, p);

        float horizon = getHeight() * .60f;
        drawMountainLayer(c, horizon, .13f, 240f, Color.rgb(54, 91, 117), 135f);
        drawMountainLayer(c, horizon + 36f, .27f, 190f, Color.rgb(67, 118, 100), 105f);

        p.setColor(Color.rgb(112, 185, 91));
        c.drawRect(0, horizon, getWidth(), getHeight(), p);
        drawGroundGrid(c, horizon, Color.argb(45, 44, 104, 54));

        // bulb trees / alien vegetation
        float offset = mod(worldX * .48f, 230f);
        for (int i = -1; i < 7; i++) {
            float x = i * 230f - offset + 65f;
            float y = horizon + 58f + ((i * 47 + (int)(worldY * .17f)) % 105 + 105) % 105;
            p.setColor(Color.rgb(72, 91, 121));
            Path trunk = new Path();
            trunk.moveTo(x - 10, y + 42); trunk.lineTo(x - 3, y - 24); trunk.lineTo(x + 10, y + 42); trunk.close();
            c.drawPath(trunk, p);
            p.setColor(Color.rgb(78, 172, 137));
            c.drawOval(new RectF(x - 31, y - 48, x + 31, y - 11), p);
            p.setColor(Color.argb(75, 210, 255, 220));
            c.drawOval(new RectF(x - 18, y - 43, x + 18, y - 29), p);
        }
        drawWorldLabel(c, "EMERALD PLANET");
    }

    private void drawRockyWorld(Canvas c) {
        Paint p = this.p;
        p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{Color.rgb(37, 62, 115), Color.rgb(220, 139, 83), Color.rgb(218, 171, 94)},
                new float[]{0f, .54f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null);
        p.setColor(Color.argb(150, 255, 224, 140));
        c.drawCircle(getWidth() * .78f, getHeight() * .17f, 47f, p);

        float horizon = getHeight() * .57f;
        drawMountainLayer(c, horizon + 8, .12f, 220f, Color.rgb(99, 70, 78), 150f);
        drawMountainLayer(c, horizon + 50, .31f, 170f, Color.rgb(137, 86, 70), 118f);
        p.setColor(Color.rgb(188, 128, 71));
        c.drawRect(0, horizon, getWidth(), getHeight(), p);

        float off = mod(worldX * .62f, 190f);
        for (int i = -1; i < 9; i++) {
            float x = i * 190f - off;
            float y = horizon + 35f + ((i * 53 + (int)(worldY * .21f)) % 120 + 120) % 120;
            p.setColor(Color.rgb(111, 73, 65));
            c.drawOval(new RectF(x, y, x + 50, y + 24), p);
            p.setColor(Color.rgb(155, 103, 72));
            c.drawOval(new RectF(x + 8, y + 4, x + 35, y + 16), p);
        }
        drawCraterPattern(c, horizon, Color.argb(65, 73, 45, 42));
        drawWorldLabel(c, "ROCKY WASTELAND");
    }

    private void drawRuinedWorld(Canvas c) {
        Paint p = this.p;
        p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{Color.rgb(24, 25, 50), Color.rgb(96, 57, 72), Color.rgb(115, 93, 74)},
                new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null);
        p.setColor(Color.argb(145, 209, 111, 91));
        c.drawCircle(getWidth() * .82f, getHeight() * .15f, 39f, p);

        float horizon = getHeight() * .61f;
        drawMountainLayer(c, horizon + 15f, .18f, 215f, Color.rgb(55, 49, 66), 120f);
        p.setColor(Color.rgb(91, 82, 72));
        c.drawRect(0, horizon, getWidth(), getHeight(), p);
        drawCraterPattern(c, horizon, Color.argb(95, 45, 39, 37));

        // ruined towers
        float off = mod(worldX * .42f, 265f);
        for (int i = -1; i < 7; i++) {
            float x = i * 265f - off + 50f;
            float base = horizon + 48f + ((i * 43 + (int)(worldY * .18f)) % 90 + 90) % 90;
            p.setColor(Color.rgb(63, 68, 71));
            float h = 45f + (i & 1) * 28f;
            Path ruin = new Path();
            ruin.moveTo(x - 24, base); ruin.lineTo(x - 22, base - h); ruin.lineTo(x - 8, base - h + 12);
            ruin.lineTo(x + 2, base - h - 8); ruin.lineTo(x + 20, base - h + 9); ruin.lineTo(x + 24, base); ruin.close();
            c.drawPath(ruin, p);
            p.setColor(Color.argb(110, 224, 113, 76));
            c.drawRect(x - 10, base - h + 15, x - 3, base - h + 23, p);
            c.drawRect(x + 5, base - h + 28, x + 12, base - h + 36, p);
        }
        drawWorldLabel(c, "RUINED BATTLE ZONE");
    }

    private void drawMountainLayer(Canvas c, float base, float parallax, float spacing, int color, float height) {
        float offset = mod(worldX * parallax, spacing);
        p.setColor(color);
        for (int i = -2; i < 9; i++) {
            float x = i * spacing - offset;
            float wobble = ((i * 37) & 3) * 13f;
            Path m = new Path();
            m.moveTo(x - spacing * .62f, base);
            m.lineTo(x, base - height - wobble);
            m.lineTo(x + spacing * .62f, base);
            m.close();
            c.drawPath(m, p);
            p.setColor(Color.argb(45, 255, 255, 255));
            Path cap = new Path();
            cap.moveTo(x - 13, base - height + 19 - wobble);
            cap.lineTo(x, base - height - wobble);
            cap.lineTo(x + 16, base - height + 22 - wobble);
            cap.close(); c.drawPath(cap, p);
            p.setColor(color);
        }
    }

    private void drawGroundGrid(Canvas c, float horizon, int color) {
        p.setColor(color); p.setStrokeWidth(2f);
        float ox = mod(worldX * .58f, 95f), oy = mod(worldY * .32f, 70f);
        for (float x = -95 - ox; x < getWidth() + 95; x += 95) c.drawLine(x, horizon, x + 30, getHeight(), p);
        for (float y = horizon - oy; y < getHeight(); y += 70) c.drawLine(0, y, getWidth(), y, p);
    }

    private void drawCraterPattern(Canvas c, float horizon, int color) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f); p.setColor(color);
        float off = mod(worldX * .52f, 210f);
        for (int i = -1; i < 8; i++) {
            float x = i * 210f - off + 60f;
            float y = horizon + 70f + ((i * 61 + (int)(worldY * .22f)) % 110 + 110) % 110;
            c.drawOval(new RectF(x - 34, y - 10, x + 34, y + 10), p);
            c.drawOval(new RectF(x - 21, y - 6, x + 21, y + 6), p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawWorldLabel(Canvas c, String label) {
        t.setTextAlign(Paint.Align.RIGHT); t.setTextSize(15f); t.setColor(Color.argb(160,255,255,255));
        c.drawText(label, getWidth() - 22f, getHeight() - 33f, t);
    }

    @Override
    void drawBullet(Canvas c, B b) {
        float speed = (float)Math.max(1, Math.hypot(b.vx, b.vy));
        float ux = b.vx / speed, uy = b.vy / speed;
        float trail = b.ty == 1 ? 150f : b.ty == 3 ? 80f : b.ty == 2 ? 72f : 48f;

        // outer glow / trail
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeWidth(b.ty == 1 ? b.r * 1.55f : Math.max(5f, b.r * .85f));
        if (b.ty == 0) stroke.setColor(Color.argb(75, 80, 225, 255));
        else if (b.ty == 1) stroke.setColor(Color.argb(100, 92, 200, 255));
        else if (b.ty == 2) stroke.setColor(Color.argb(90, 255, 224, 80));
        else stroke.setColor(Color.argb(100, 145, 205, 255));
        c.drawLine(b.x - ux * trail, b.y - uy * trail, b.x, b.y, stroke);

        // bright inner trail
        stroke.setStrokeWidth(b.ty == 1 ? Math.max(4f, b.r * .52f) : 3f);
        stroke.setColor(Color.argb(190, 245, 255, 255));
        c.drawLine(b.x - ux * trail * .72f, b.y - uy * trail * .72f, b.x, b.y, stroke);

        if (b.ty == 0) {
            RadialGradient g = new RadialGradient(b.x,b.y,b.r*1.8f,
                    new int[]{Color.WHITE,Color.rgb(85,225,255),Color.argb(0,50,160,255)},null,Shader.TileMode.CLAMP);
            fx.setShader(g); c.drawCircle(b.x,b.y,b.r*1.8f,fx); fx.setShader(null);
        } else if (b.ty == 1) {
            fx.setColor(Color.WHITE); c.drawCircle(b.x,b.y,b.r*.72f,fx);
            fx.setStyle(Paint.Style.STROKE); fx.setStrokeWidth(3f); fx.setColor(Color.rgb(120,220,255)); c.drawCircle(b.x,b.y,b.r+5f,fx); fx.setStyle(Paint.Style.FILL);
        } else if (b.ty == 2) {
            fx.setColor(Color.rgb(255,239,90)); c.drawCircle(b.x,b.y,b.r,fx);
            fx.setStyle(Paint.Style.STROKE); fx.setStrokeWidth(3f); fx.setColor(Color.WHITE); c.drawCircle(b.x,b.y,b.r*.62f,fx); fx.setStyle(Paint.Style.FILL);
        } else {
            RadialGradient g = new RadialGradient(b.x,b.y,b.r*2.1f,
                    new int[]{Color.WHITE,Color.rgb(120,205,255),Color.argb(0,80,140,255)},null,Shader.TileMode.CLAMP);
            fx.setShader(g); c.drawCircle(b.x,b.y,b.r*2.05f,fx); fx.setShader(null);
        }
    }

    @Override
    void drawEnemy(Canvas c, E e) {
        // shadow
        p.setColor(Color.argb(60,0,0,0));
        c.drawOval(new RectF(e.x-e.r*.9f,e.y+e.r*.58f,e.x+e.r*.9f,e.y+e.r*.95f),p);

        if (e.ty == 0) drawSeedEnemy(c,e);
        else if (e.ty == 1) drawSpaceSoldier(c,e);
        else if (e.ty == 2) drawAndroidEnemy(c,e);
        else if (e.ty == 3) drawMajinEnemy(c,e);
        else drawBossEnemy(c,e);
    }

    private void drawSeedEnemy(Canvas c,E e){
        p.setColor(Color.rgb(81,176,73)); c.drawCircle(e.x,e.y,e.r,p);
        p.setColor(Color.rgb(58,126,55)); c.drawOval(new RectF(e.x-e.r*.65f,e.y,e.x+e.r*.65f,e.y+e.r*.8f),p);
        p.setStrokeWidth(3f); p.setColor(Color.rgb(48,83,42)); c.drawLine(e.x-5,e.y-e.r+2,e.x-12,e.y-e.r-13,p); c.drawLine(e.x+5,e.y-e.r+2,e.x+12,e.y-e.r-13,p);
        eyePair(c,e,Color.rgb(255,78,74));
    }

    private void drawSpaceSoldier(Canvas c,E e){
        p.setColor(Color.rgb(120,73,177)); c.drawCircle(e.x,e.y,e.r,p);
        p.setColor(Color.rgb(224,225,232)); c.drawOval(new RectF(e.x-e.r*.72f,e.y-e.r*.36f,e.x+e.r*.72f,e.y+e.r*.5f),p);
        p.setColor(Color.rgb(63,52,83)); c.drawRect(e.x-e.r*.72f,e.y+e.r*.2f,e.x+e.r*.72f,e.y+e.r*.52f,p);
        eyePair(c,e,Color.rgb(255,233,109));
    }

    private void drawAndroidEnemy(Canvas c,E e){
        p.setColor(Color.rgb(160,169,177)); c.drawRoundRect(new RectF(e.x-e.r,e.y-e.r,e.x+e.r,e.y+e.r),7,7,p);
        p.setColor(Color.rgb(62,72,80)); c.drawRoundRect(new RectF(e.x-e.r*.72f,e.y-e.r*.62f,e.x+e.r*.72f,e.y+e.r*.08f),5,5,p);
        p.setColor(Color.rgb(255,68,68)); c.drawCircle(e.x,e.y-e.r*.28f,4f,p);
        p.setColor(Color.rgb(78,89,98)); c.drawRect(e.x-e.r*.55f,e.y+e.r*.32f,e.x+e.r*.55f,e.y+e.r*.48f,p);
    }

    private void drawMajinEnemy(Canvas c,E e){
        p.setColor(Color.rgb(218,112,180)); c.drawCircle(e.x,e.y,e.r,p);
        p.setColor(Color.rgb(179,75,143)); c.drawOval(new RectF(e.x-e.r*.45f,e.y+2,e.x+e.r*.45f,e.y+e.r*.68f),p);
        eyePair(c,e,Color.rgb(72,31,65));
        p.setStrokeWidth(4f); p.setColor(Color.rgb(150,68,119)); c.drawLine(e.x,e.y-e.r+2,e.x+10,e.y-e.r-15,p);
    }

    private void drawBossEnemy(Canvas c,E e){
        fx.setColor(Color.argb(75,255,65,65)); c.drawCircle(e.x,e.y,e.r+15,fx);
        p.setColor(Color.rgb(102,48,158)); c.drawCircle(e.x,e.y,e.r,p);
        p.setColor(Color.rgb(58,34,98)); c.drawOval(new RectF(e.x-e.r*.78f,e.y,e.x+e.r*.78f,e.y+e.r*.75f),p);
        eyePair(c,e,Color.rgb(255,232,112));
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5f); p.setColor(Color.rgb(255,70,70)); c.drawCircle(e.x,e.y,e.r+10,p); p.setStyle(Paint.Style.FILL);
    }

    private void eyePair(Canvas c,E e,int color){p.setColor(color);c.drawCircle(e.x-6,e.y-5,3,p);c.drawCircle(e.x+6,e.y-5,3,p);}

    private static float mod(float a,float b){float m=a%b;return m<0?m+b:m;}

    @Override
    void restart() {
        super.restart();
        dmg = 1.25f;
        show("WORLD ADVENTURE");
    }
}
