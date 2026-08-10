package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

/** v0.5.1: left stick movement + right stick manual aiming, with auto-aim fallback. */
public class GameViewV7 extends GameViewV6 {
    private int movePointerId = -1;
    private int aimPointerId = -1;
    private boolean aiming = false;
    private float aimBaseX, aimBaseY;
    private float aimX, aimY;
    private float lastAimX = 1f, lastAimY = 0f;
    private final Paint aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameViewV7(Context c) {
        super(c);
        banner = "DUAL STICK AIM";
    }

    @Override
    void shot(int ty, int amount) {
        float angle;
        if (aiming && Math.hypot(aimX, aimY) > 6f) {
            angle = (float)Math.atan2(aimY, aimX);
            float len = (float)Math.max(1, Math.hypot(aimX, aimY));
            lastAimX = aimX / len;
            lastAimY = aimY / len;
        } else {
            E q = near();
            if (q == null) return;
            angle = (float)Math.atan2(q.y - py, q.x - px);
        }

        for (int i = 0; i < amount; i++) {
            float aa = angle + (i - (amount - 1) / 2f) * .15f;
            float sp = ty == 3 ? 260 : ty == 1 ? 620 : 700;
            float rr = ty == 3 ? 20 + bomb * 3 : ty == 1 ? 16 + kame * 3 : ty == 2 ? 15 + disc * 2 : 9 + ki;
            float dd = (ty == 3 ? 45 + bomb * 16 : ty == 1 ? 30 + kame * 13 : ty == 2 ? 18 + disc * 8 : 10 + ki * 4) * dmg;
            bs.add(new B(ty, px, py, (float)Math.cos(aa) * sp, (float)Math.sin(aa) * sp, rr, dd,
                    ty == 2 ? 3 + disc : ty == 1 ? 2 + kame / 2 : ty == 3 ? 99 : Math.max(0, ki - 4)));
        }
    }

    @Override
    void hud(Canvas c) {
        super.hud(c);

        float bx = aiming ? aimBaseX : getWidth() - 112f;
        float by = aiming ? aimBaseY : getHeight() - 112f;
        float len = (float)Math.hypot(aimX, aimY);
        float knobX = bx;
        float knobY = by;
        if (aiming && len > 0) {
            float m = Math.min(70f, len);
            knobX = bx + aimX / len * m;
            knobY = by + aimY / len * m;
        }

        aimPaint.setStyle(Paint.Style.FILL);
        aimPaint.setColor(Color.argb(72, 120, 220, 255));
        c.drawCircle(bx, by, 72f, aimPaint);
        aimPaint.setStyle(Paint.Style.STROKE);
        aimPaint.setStrokeWidth(3f);
        aimPaint.setColor(Color.argb(180, 160, 235, 255));
        c.drawCircle(bx, by, 72f, aimPaint);
        aimPaint.setStyle(Paint.Style.FILL);
        aimPaint.setColor(Color.argb(190, 220, 250, 255));
        c.drawCircle(knobX, knobY, 31f, aimPaint);

        float dx = aiming && len > 6 ? aimX / len : lastAimX;
        float dy = aiming && len > 6 ? aimY / len : lastAimY;
        aimPaint.setStrokeWidth(5f);
        aimPaint.setColor(Color.argb(185, 120, 235, 255));
        c.drawLine(bx + dx * 34f, by + dy * 34f, bx + dx * 62f, by + dy * 62f, aimPaint);

        t.setTextAlign(Paint.Align.CENTER);
        t.setTextSize(14f);
        t.setColor(Color.argb(190,255,255,255));
        c.drawText("AIM", bx, by - 82f, t);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int actionIndex = e.getActionIndex();

        if (dead && action == MotionEvent.ACTION_DOWN) {
            restart();
            resetPointers();
            return true;
        }

        if (choose) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                float tx = e.getX(actionIndex), ty = e.getY(actionIndex);
                for (U u : us) {
                    if (u.rc != null && u.rc.contains(tx, ty)) {
                        apply(u.id, false);
                        resetPointers();
                        return true;
                    }
                }
            }
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int id = e.getPointerId(actionIndex);
            float x = e.getX(actionIndex), y = e.getY(actionIndex);
            if (x < getWidth() * .5f && movePointerId == -1) {
                movePointerId = id;
                joy = true;
                jbx = x; jby = y; jx = jy = 0;
            } else if (x >= getWidth() * .5f && aimPointerId == -1) {
                aimPointerId = id;
                aiming = true;
                aimBaseX = x; aimBaseY = y; aimX = aimY = 0;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (movePointerId != -1) {
                int idx = e.findPointerIndex(movePointerId);
                if (idx >= 0) {
                    float dx = e.getX(idx) - jbx, dy = e.getY(idx) - jby;
                    float d = (float)Math.max(1, Math.hypot(dx, dy));
                    float m = Math.min(70f, d);
                    jx = dx / d * m; jy = dy / d * m;
                }
            }
            if (aimPointerId != -1) {
                int idx = e.findPointerIndex(aimPointerId);
                if (idx >= 0) {
                    aimX = e.getX(idx) - aimBaseX;
                    aimY = e.getY(idx) - aimBaseY;
                    float d = (float)Math.hypot(aimX, aimY);
                    if (d > 6f) { lastAimX = aimX / d; lastAimY = aimY / d; }
                }
            }
        } else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            int id = action == MotionEvent.ACTION_CANCEL ? -99 : e.getPointerId(actionIndex);
            if (action == MotionEvent.ACTION_CANCEL || id == movePointerId) {
                movePointerId = -1; joy = false; jx = jy = 0;
            }
            if (action == MotionEvent.ACTION_CANCEL || id == aimPointerId) {
                if (Math.hypot(aimX, aimY) > 6f) {
                    float d = (float)Math.hypot(aimX, aimY);
                    lastAimX = aimX / d; lastAimY = aimY / d;
                }
                aimPointerId = -1; aiming = false; aimX = aimY = 0;
            }
        }
        return true;
    }

    @Override
    void restart() {
        super.restart();
        resetPointers();
        lastAimX = 1f; lastAimY = 0f;
        banner = "DUAL STICK AIM";
    }

    private void resetPointers() {
        movePointerId = aimPointerId = -1;
        joy = false; aiming = false;
        jx = jy = aimX = aimY = 0;
    }
}
