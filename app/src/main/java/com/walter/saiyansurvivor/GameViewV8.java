package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.*;
import java.util.*;

/** v0.6.0: premium visual pass, richer HUD, particles, hits, chests and character/enemy rendering. */
public class GameViewV8 extends GameViewV7 {
    private final Paint premium = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Pfx> particles = new ArrayList<>();
    private float screenFlash = 0f;
    private float shake = 0f;
    private long lastParticleTick = 0;

    public GameViewV8(Context c) {
        super(c);
        banner = "PREMIUM VISUAL PASS";
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!init) return;
        long n = System.nanoTime();
        float dt = Math.min(.033f, (n - frame) / 1e9f);
        frame = n;
        if (!dead && !choose) update(dt);
        updateParticles(dt);
        float sx = 0, sy = 0;
        if (shake > 0f) {
            shake = Math.max(0f, shake - dt * 7f);
            sx = (r.nextFloat() - .5f) * 12f * shake;
            sy = (r.nextFloat() - .5f) * 12f * shake;
        }
        c.save(); c.translate(sx, sy); render(c); c.restore();
        if (screenFlash > 0f) {
            screenFlash = Math.max(0f, screenFlash - dt * 3.5f);
            premium.setColor(Color.argb((int)(screenFlash * 110),255,255,255));
            c.drawRect(0,0,getWidth(),getHeight(),premium);
        }
        invalidate();
    }

    private void updateParticles(float dt) {
        for (int i = particles.size()-1; i >= 0; i--) {
            Pfx q = particles.get(i);
            q.x += q.vx * dt; q.y += q.vy * dt; q.life -= dt;
            q.vx *= .985f; q.vy *= .985f;
            if (q.life <= 0) particles.remove(i);
        }
        long now = System.currentTimeMillis();
        if (ssj > 0 && now - lastParticleTick > 65) {
            lastParticleTick = now;
            particles.add(new Pfx(px + r.nextFloat()*42-21, py + r.nextFloat()*48-26,
                    r.nextFloat()*30-15, -35-r.nextFloat()*45, .45f, Color.rgb(255,225,70), 4+r.nextFloat()*4));
        }
    }

    @Override
    void bullets(float dt) {
        for (int i = bs.size()-1; i >= 0; i--) {
            B b = bs.get(i);
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
            if (b.life < 0 || dist(px,py,b.x,b.y) > 1600) { bs.remove(i); continue; }
            boolean rm = false;
            for (int j = es.size()-1; j >= 0; j--) {
                E e = es.get(j);
                if (dist(b.x,b.y,e.x,e.y) < b.r + e.r) {
                    e.hp -= b.d; b.h++;
                    burst(e.x,e.y,b.ty);
                    screenFlash = Math.max(screenFlash, b.ty==1 || b.ty==3 ? .65f : .22f);
                    shake = Math.max(shake, b.ty==1 ? .65f : b.ty==3 ? .8f : .18f);
                    if (e.hp <= 0) kill(j,e);
                    if (b.h > b.p) rm = true;
                    if (rm) break;
                }
            }
            if (rm) bs.remove(i);
        }
    }

    private void burst(float x,float y,int ty) {
        int n = ty==3 ? 18 : ty==1 ? 12 : 7;
        int col = ty==2 ? Color.rgb(255,230,80) : Color.rgb(110,220,255);
        for (int i=0;i<n;i++) {
            double a=r.nextDouble()*Math.PI*2; float sp=45+r.nextFloat()*120;
            particles.add(new Pfx(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,.30f+r.nextFloat()*.35f,col,3+r.nextFloat()*5));
        }
    }

    @Override
    void kill(int i, E e) {
        burst(e.x,e.y,e.boss?3:0);
        if (e.boss) { shake = 1f; screenFlash = .9f; }
        super.kill(i,e);
    }

    @Override
    void render(Canvas c) {
        bg(c);
        for (Pfx q:particles) drawParticle(c,q);
        for (C q:cs) drawChest(c,q);
        for (O o:os) drawOrb(c,o);
        for (B b:bs) drawBullet(c,b);
        for (E e:es) drawEnemy(c,e);
        drawPlayer(c);
        hud(c);
        if (System.currentTimeMillis()<bannerUntil) drawBanner(c);
        if (choose) choices(c);
        if (dead) over(c);
    }

    private void drawParticle(Canvas c,Pfx q){
        float a=Math.max(0,Math.min(1,q.life/.7f));
        premium.setColor((q.color&0x00ffffff)|((int)(a*210)<<24));
        c.drawCircle(q.x,q.y,q.size,premium);
    }

    private void drawOrb(Canvas c,O o){
        RadialGradient g=new RadialGradient(o.x,o.y,13f,new int[]{Color.WHITE,Color.rgb(80,235,255),Color.argb(0,0,180,255)},null,Shader.TileMode.CLAMP);
        premium.setShader(g); c.drawCircle(o.x,o.y,13f,premium); premium.setShader(null);
        premium.setColor(Color.argb(90,255,255,255)); c.drawCircle(o.x-3,o.y-3,3,premium);
    }

    @Override
    void drawPlayer(Canvas c) {
        float pulse=(float)(Math.sin(System.currentTimeMillis()/110.0)*2.5);
        int aura=ssj>0?Color.argb(95,255,223,70):kai>0?Color.argb(90,255,70,75):Color.argb(60,80,190,255);
        premium.setColor(aura); c.drawOval(new RectF(px-35-pulse,py-51-pulse,px+35+pulse,py+38+pulse),premium);
        if (ssj>0) { premium.setColor(Color.argb(65,255,245,150)); c.drawOval(new RectF(px-46,py-62,px+46,py+45),premium); }
        premium.setColor(Color.argb(75,0,0,0)); c.drawOval(new RectF(px-24,py+28,px+24,py+39),premium);

        // legs/boots
        premium.setColor(Color.rgb(28,55,145)); c.drawRoundRect(new RectF(px-13,py+15,px-3,py+35),4,4,premium); c.drawRoundRect(new RectF(px+3,py+15,px+13,py+35),4,4,premium);
        premium.setColor(Color.rgb(30,30,45)); c.drawOval(new RectF(px-15,py+31,px-1,py+39),premium); c.drawOval(new RectF(px+1,py+31,px+15,py+39),premium);

        // torso
        premium.setColor(Color.rgb(240,116,32)); c.drawRoundRect(new RectF(px-17,py-8,px+17,py+20),8,8,premium);
        premium.setColor(Color.rgb(32,64,185)); c.drawRect(px-17,py+7,px+17,py+13,premium);
        premium.setColor(Color.rgb(245,194,145)); c.drawCircle(px,py-20,12,premium);

        // arms
        premium.setStrokeWidth(7f); premium.setStrokeCap(Paint.Cap.ROUND); premium.setColor(Color.rgb(245,194,145));
        c.drawLine(px-15,py-2,px-24,py+11,premium); c.drawLine(px+15,py-2,px+24,py+11,premium);

        // hair
        premium.setColor(ssj>0?Color.rgb(255,220,58):Color.rgb(28,30,38));
        Path hair=new Path(); hair.moveTo(px-13,py-29); hair.lineTo(px-9,py-46); hair.lineTo(px-1,py-34); hair.lineTo(px+4,py-50); hair.lineTo(px+10,py-34); hair.lineTo(px+16,py-44); hair.lineTo(px+13,py-27); hair.close(); c.drawPath(hair,premium);
        // face details
        premium.setColor(Color.rgb(34,34,44)); c.drawCircle(px-4,py-21,1.8f,premium); c.drawCircle(px+4,py-21,1.8f,premium);
        premium.setStrokeWidth(2f); c.drawLine(px-4,py-14,px+4,py-14,premium);

        outline.setStrokeWidth(2.2f); outline.setColor(Color.argb(150,20,20,30));
        c.drawRoundRect(new RectF(px-17,py-8,px+17,py+20),8,8,outline); c.drawCircle(px,py-20,12,outline);
    }

    @Override
    void drawEnemy(Canvas c,E e) {
        premium.setColor(Color.argb(65,0,0,0)); c.drawOval(new RectF(e.x-e.r*.95f,e.y+e.r*.62f,e.x+e.r*.95f,e.y+e.r*.98f),premium);
        if(e.ty==0) drawGreenRaider(c,e); else if(e.ty==1) drawArmorSoldier(c,e); else if(e.ty==2) drawAndroid(c,e); else if(e.ty==3) drawMajin(c,e); else drawBoss(c,e);
    }

    private void drawGreenRaider(Canvas c,E e){
        premium.setColor(Color.rgb(88,181,74)); c.drawOval(new RectF(e.x-e.r,e.y-e.r,e.x+e.r,e.y+e.r),premium);
        premium.setColor(Color.rgb(61,133,52)); Path spikes=new Path(); spikes.moveTo(e.x-e.r*.8f,e.y-e.r*.35f); spikes.lineTo(e.x-e.r*1.35f,e.y-e.r*.75f); spikes.lineTo(e.x-e.r*.45f,e.y-e.r*.72f); spikes.close(); c.drawPath(spikes,premium);
        premium.setColor(Color.rgb(235,238,180)); c.drawOval(new RectF(e.x-9,e.y-8,e.x-2,e.y-2),premium); c.drawOval(new RectF(e.x+2,e.y-8,e.x+9,e.y-2),premium);
        premium.setColor(Color.rgb(40,35,30)); c.drawCircle(e.x-5,e.y-5,2,premium); c.drawCircle(e.x+5,e.y-5,2,premium);
    }
    private void drawArmorSoldier(Canvas c,E e){
        premium.setColor(Color.rgb(83,63,153)); c.drawRoundRect(new RectF(e.x-e.r*.78f,e.y-e.r*.65f,e.x+e.r*.78f,e.y+e.r*.85f),8,8,premium);
        premium.setColor(Color.rgb(230,220,190)); c.drawRoundRect(new RectF(e.x-e.r*.63f,e.y-e.r*.28f,e.x+e.r*.63f,e.y+e.r*.28f),5,5,premium);
        premium.setColor(Color.rgb(64,35,102)); c.drawCircle(e.x,e.y-e.r*.55f,e.r*.42f,premium);
        premium.setColor(Color.rgb(252,93,104)); c.drawRect(e.x+2,e.y-e.r*.62f,e.x+10,e.y-e.r*.53f,premium);
    }
    private void drawAndroid(Canvas c,E e){
        premium.setColor(Color.rgb(166,174,183)); c.drawRoundRect(new RectF(e.x-e.r*.75f,e.y-e.r*.8f,e.x+e.r*.75f,e.y+e.r*.85f),5,5,premium);
        premium.setColor(Color.rgb(64,72,81)); c.drawRect(e.x-e.r*.75f,e.y-e.r*.15f,e.x+e.r*.75f,e.y+e.r*.15f,premium);
        premium.setColor(Color.rgb(228,65,65)); c.drawCircle(e.x-6,e.y-e.r*.43f,2.7f,premium); c.drawCircle(e.x+6,e.y-e.r*.43f,2.7f,premium);
    }
    private void drawMajin(Canvas c,E e){
        premium.setColor(Color.rgb(215,112,177)); c.drawOval(new RectF(e.x-e.r,e.y-e.r*.9f,e.x+e.r,e.y+e.r),premium);
        premium.setColor(Color.rgb(171,79,141)); c.drawOval(new RectF(e.x-e.r*.5f,e.y-e.r*.2f,e.x+e.r*.5f,e.y+e.r*.72f),premium);
        premium.setColor(Color.WHITE); c.drawOval(new RectF(e.x-8,e.y-10,e.x-2,e.y-4),premium); c.drawOval(new RectF(e.x+2,e.y-10,e.x+8,e.y-4),premium);
        premium.setColor(Color.rgb(35,25,45)); c.drawCircle(e.x-5,e.y-7,2,premium); c.drawCircle(e.x+5,e.y-7,2,premium);
    }
    private void drawBoss(Canvas c,E e){
        premium.setColor(Color.argb(70,190,70,255)); c.drawCircle(e.x,e.y,e.r+15,premium);
        premium.setColor(Color.rgb(93,45,155)); c.drawRoundRect(new RectF(e.x-e.r*.8f,e.y-e.r,e.x+e.r*.8f,e.y+e.r),14,14,premium);
        premium.setColor(Color.rgb(46,27,86)); c.drawOval(new RectF(e.x-e.r*.55f,e.y-e.r*.72f,e.x+e.r*.55f,e.y-e.r*.1f),premium);
        premium.setColor(Color.rgb(255,205,70)); c.drawCircle(e.x-9,e.y-e.r*.42f,3.5f,premium); c.drawCircle(e.x+9,e.y-e.r*.42f,3.5f,premium);
        outline.setStrokeWidth(5f); outline.setColor(Color.argb(220,255,80,100)); c.drawCircle(e.x,e.y,e.r+9,outline);
    }

    @Override
    void drawChest(Canvas c,C q){
        premium.setColor(Color.argb(65,255,210,70)); c.drawCircle(q.x,q.y,29,premium);
        premium.setColor(Color.rgb(88,44,24)); c.drawRoundRect(new RectF(q.x-21,q.y-14,q.x+21,q.y+15),7,7,premium);
        premium.setColor(Color.rgb(154,82,38)); c.drawRoundRect(new RectF(q.x-19,q.y-13,q.x+19,q.y-2),6,6,premium);
        premium.setColor(Color.rgb(255,210,62)); c.drawRect(q.x-4,q.y-14,q.x+4,q.y+15,premium); c.drawCircle(q.x,q.y+1,4,premium);
        outline.setStrokeWidth(2.5f); outline.setColor(Color.rgb(45,25,18)); c.drawRoundRect(new RectF(q.x-21,q.y-14,q.x+21,q.y+15),7,7,outline);
    }

    @Override
    void hud(Canvas c){
        // premium top card
        premium.setColor(Color.argb(190,13,18,31)); c.drawRoundRect(new RectF(14,12,548,116),18,18,premium);
        premium.setShader(new LinearGradient(14,12,548,12,new int[]{Color.rgb(47,92,183),Color.rgb(112,69,176)},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(14,12,548,17),18,18,premium); premium.setShader(null);
        t.setTextAlign(Paint.Align.LEFT); t.setColor(Color.WHITE); t.setTextSize(23); t.setFakeBoldText(true); c.drawText("LV "+lv+"   KILLS "+kills+"   "+time(),28,42,t); t.setFakeBoldText(false);
        t.setTextSize(15); t.setColor(Color.rgb(180,210,255)); c.drawText("PREMIUM WORLD  •  "+(int)(Math.hypot(worldX,worldY)/100)+" m",28,65,t);
        // hp
        premium.setColor(Color.rgb(47,50,62)); c.drawRoundRect(new RectF(28,78,480,91),7,7,premium);
        premium.setColor(Color.rgb(232,67,77)); c.drawRoundRect(new RectF(28,78,28+452*(hp/mhp),91),7,7,premium);
        // xp
        premium.setColor(Color.rgb(27,30,39)); c.drawRoundRect(new RectF(14,getHeight()-20,getWidth()-14,getHeight()-8),7,7,premium);
        premium.setColor(Color.rgb(62,220,244)); c.drawRoundRect(new RectF(14,getHeight()-20,14+(getWidth()-28)*(xp/nxp),getHeight()-8),7,7,premium);
        // ability chips
        drawChip(c,28,101,"KI "+ki,Color.rgb(65,196,235));
        if(kame>0)drawChip(c,96,101,"K "+kame,Color.rgb(82,142,255));
        if(disc>0)drawChip(c,154,101,"D "+disc,Color.rgb(235,204,63));
        if(bomb>0)drawChip(c,212,101,"G "+bomb,Color.rgb(127,190,255));
        // draw sticks after panel
        float x=joy?jbx:110,y=joy?jby:getHeight()-110; premium.setColor(Color.argb(65,255,255,255)); c.drawCircle(x,y,70,premium); outline.setStrokeWidth(2.5f); outline.setColor(Color.argb(120,255,255,255)); c.drawCircle(x,y,70,outline); premium.setColor(Color.argb(180,255,255,255)); c.drawCircle(x+jx,y+jy,30,premium);
        // right stick from v0.5.1
        super.hud(c);
    }

    private void drawChip(Canvas c,float x,float y,String s,int col){
        premium.setColor(Color.argb(210,28,33,49)); c.drawRoundRect(new RectF(x,y-14,x+52,y+5),9,9,premium);
        premium.setColor(col); c.drawCircle(x+10,y-4,5,premium); t.setTextSize(12); t.setColor(Color.WHITE); t.setTextAlign(Paint.Align.LEFT); c.drawText(s,x+19,y,t);
    }

    @Override
    void choices(Canvas c){
        premium.setColor(Color.argb(230,8,12,22)); c.drawRect(0,0,getWidth(),getHeight(),premium);
        t.setTextAlign(Paint.Align.CENTER); t.setColor(Color.WHITE); t.setFakeBoldText(true); t.setTextSize(34); c.drawText("LEVEL UP",getWidth()/2f,76,t); t.setFakeBoldText(false);
        t.setTextSize(16); t.setColor(Color.rgb(170,195,230)); c.drawText("Choose your next power",getWidth()/2f,102,t);
        float w=230,h=184,g=24,s=(getWidth()-(w*3+g*2))/2,y=getHeight()*.32f;
        for(int i=0;i<us.size();i++){
            U u=us.get(i);float l=s+i*(w+g);u.rc=new RectF(l,y,l+w,y+h);
            premium.setShader(new LinearGradient(l,y,l,y+h,new int[]{Color.rgb(37,47,76),Color.rgb(18,24,40)},null,Shader.TileMode.CLAMP)); c.drawRoundRect(u.rc,20,20,premium); premium.setShader(null);
            outline.setStrokeWidth(2.5f); outline.setColor(Color.rgb(92,140,230)); c.drawRoundRect(u.rc,20,20,outline);
            int col=powerColor(u.id); premium.setColor(col); c.drawCircle(l+w/2,y+47,24,premium);
            t.setTextSize(20);t.setColor(Color.WHITE);c.drawText(name(u.id),l+w/2,y+93,t);
            t.setTextSize(13);t.setColor(Color.rgb(176,197,226));c.drawText(powerDesc(u.id),l+w/2,y+122,t);
            t.setTextSize(12);t.setColor(col);c.drawText("POWER UP",l+w/2,y+153,t);
        }
    }

    private int powerColor(String id){if(id.equals("ki"))return Color.rgb(72,210,255);if(id.equals("kame"))return Color.rgb(75,130,255);if(id.equals("disc"))return Color.rgb(255,215,65);if(id.equals("bomb"))return Color.rgb(125,195,255);if(id.equals("kai"))return Color.rgb(255,80,80);if(id.equals("senzu"))return Color.rgb(80,220,105);if(id.equals("radar"))return Color.rgb(230,170,65);return Color.rgb(255,220,70);}
    private String powerDesc(String id){if(id.equals("ki"))return "Faster energy barrage";if(id.equals("kame"))return "Heavy piercing beam";if(id.equals("disc"))return "Spinning piercing disc";if(id.equals("bomb"))return "Large energy sphere";if(id.equals("kai"))return "Movement speed boost";if(id.equals("senzu"))return "Heal and raise max HP";if(id.equals("radar"))return "Collect XP from farther";return "Stronger transformed state";}

    @Override
    void restart(){ super.restart(); particles.clear(); screenFlash=shake=0f; banner="PREMIUM VISUAL PASS"; }

    static class Pfx{float x,y,vx,vy,life,size;int color;Pfx(float x,float y,float vx,float vy,float life,int color,float size){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.color=color;this.size=size;}}
}
