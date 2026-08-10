package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.*;

/** v0.6.2: restore v0.6.0 feel; only improve green-enemy readability. */
public class GameViewV10 extends GameViewV8 {
    public GameViewV10(Context c) {
        super(c);
        banner = "READABILITY PASS";
    }

    @Override
    void bg(Canvas c) {
        int biome = ((int)((Math.hypot(worldX, worldY) / 100.0) / 22.0)) % 3;
        if (biome != 0) {
            super.bg(c);
            return;
        }

        Paint q = this.p;
        q.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{Color.rgb(42, 88, 158), Color.rgb(91, 174, 191), Color.rgb(188, 178, 132)},
                new float[]{0f, .56f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), q);
        q.setShader(null);

        // Keep the original Emerald Planet sky and atmosphere.
        q.setColor(Color.argb(170, 244, 255, 210));
        c.drawCircle(getWidth() * .80f, getHeight() * .15f, 43f, q);
        q.setColor(Color.argb(115, 185, 245, 255));
        c.drawCircle(getWidth() * .17f, getHeight() * .12f, 26f, q);

        float horizon = getHeight() * .60f;
        drawMountains(c, horizon, .13f, 240f, Color.rgb(54, 91, 117), 135f);
        drawMountains(c, horizon + 36f, .27f, 190f, Color.rgb(67, 118, 100), 105f);

        // Main combat surface is now warm stone instead of green grass.
        q.setColor(Color.rgb(174, 160, 116));
        c.drawRect(0, horizon, getWidth(), getHeight(), q);

        // Subtle moving stone seams retain the sense of travelling across a surface.
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(2f);
        q.setColor(Color.argb(42, 75, 61, 43));
        float ox = modLocal(worldX * .58f, 105f);
        float oy = modLocal(worldY * .32f, 72f);
        for (float x = -105f - ox; x < getWidth() + 105f; x += 105f)
            c.drawLine(x, horizon, x + 30f, getHeight(), q);
        for (float y = horizon - oy; y < getHeight(); y += 72f)
            c.drawLine(0, y, getWidth(), y, q);
        q.setStyle(Paint.Style.FILL);

        // Green remains in vegetation only, preserving the original planet identity.
        float offset = modLocal(worldX * .48f, 230f);
        for (int i = -1; i < 7; i++) {
            float x = i * 230f - offset + 65f;
            float y = horizon + 58f + ((i * 47 + (int)(worldY * .17f)) % 105 + 105) % 105;
            q.setColor(Color.rgb(72, 91, 121));
            Path trunk = new Path();
            trunk.moveTo(x - 10, y + 42); trunk.lineTo(x - 3, y - 24); trunk.lineTo(x + 10, y + 42); trunk.close();
            c.drawPath(trunk, q);
            q.setColor(Color.rgb(78, 172, 137));
            c.drawOval(new RectF(x - 31, y - 48, x + 31, y - 11), q);
            q.setColor(Color.argb(75, 210, 255, 220));
            c.drawOval(new RectF(x - 18, y - 43, x + 18, y - 29), q);
        }

        t.setTextAlign(Paint.Align.RIGHT);
        t.setTextSize(15f);
        t.setColor(Color.argb(160,255,255,255));
        c.drawText("EMERALD PLANET", getWidth() - 22f, getHeight() - 33f, t);
    }

    @Override
    void drawEnemy(Canvas c, E e) {
        if (e.ty == 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f);
            p.setColor(Color.argb(190, 24, 31, 28));
            c.drawCircle(e.x, e.y, e.r + 3f, p);
            p.setStyle(Paint.Style.FILL);
        }
        super.drawEnemy(c, e);
    }

    private void drawMountains(Canvas c, float base, float parallax, float spacing, int color, float height) {
        float offset = modLocal(worldX * parallax, spacing);
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
            p.setColor(Color.argb(45,255,255,255));
            Path cap = new Path();
            cap.moveTo(x - 13, base - height + 19 - wobble);
            cap.lineTo(x, base - height - wobble);
            cap.lineTo(x + 16, base - height + 22 - wobble);
            cap.close();
            c.drawPath(cap, p);
            p.setColor(color);
        }
    }

    private float modLocal(double a, float b) {
        float x = (float)(a % b);
        return x < 0 ? x + b : x;
    }
}
